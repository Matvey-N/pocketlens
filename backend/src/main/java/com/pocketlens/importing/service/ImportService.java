package com.pocketlens.importing.service;

import com.pocketlens.categorization.service.CategorizationOutcome;
import com.pocketlens.categorization.service.CategorizationService;
import com.pocketlens.categorization.service.MerchantNormalizer;
import com.pocketlens.common.config.AppProperties;
import com.pocketlens.common.config.CacheConfig;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.importing.domain.ImportBatch;
import com.pocketlens.importing.persistence.ImportBatchRepository;
import com.pocketlens.importing.persistence.ImportProfileRepository;
import com.pocketlens.importing.service.format.MappedStatementFormat;
import com.pocketlens.importing.service.format.ParsedTransaction;
import com.pocketlens.importing.service.format.RowParseException;
import com.pocketlens.importing.service.format.StatementFormat;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports a CSV statement (UC-1): validate → parse and normalise → skip duplicates → store → categorise.
 * A file with missing columns or invalid rows is rejected as a whole, so nothing is imported half-way.
 */
@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);
    private static final int MAX_REPORTED_ERRORS = 20;
    private static final int PREVIEW_ROWS = 5;
    private static final Set<String> CSV_CONTENT_TYPES = Set.of("text/csv", "application/csv", "text/plain",
            "application/vnd.ms-excel", "application/octet-stream");

    private final StatementFormatRegistry registry;
    private final ImportProfileRepository profiles;
    private final ImportBatchRepository batches;
    private final TransactionRepository transactions;
    private final CsvReader csvReader;
    private final FingerprintCalculator fingerprintCalculator;
    private final MerchantNormalizer merchantNormalizer;
    private final CategorizationService categorizationService;
    private final AppProperties.Imports limits;
    private final Clock clock;

    public ImportService(StatementFormatRegistry registry, ImportProfileRepository profiles,
                         ImportBatchRepository batches, TransactionRepository transactions, CsvReader csvReader,
                         FingerprintCalculator fingerprintCalculator, MerchantNormalizer merchantNormalizer,
                         CategorizationService categorizationService, AppProperties properties, Clock clock) {
        this.registry = registry;
        this.profiles = profiles;
        this.batches = batches;
        this.transactions = transactions;
        this.csvReader = csvReader;
        this.fingerprintCalculator = fingerprintCalculator;
        this.merchantNormalizer = merchantNormalizer;
        this.categorizationService = categorizationService;
        this.limits = properties.imports();
        this.clock = clock;
    }

    /**
     * Imports a file using a built-in format ({@code formatId}) or a saved mapping ({@code profileId}).
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public ImportSummary importStatement(UUID userId, ImportFile file, String formatId, UUID profileId) {
        validateFile(file);
        StatementFormat format = resolveFormat(userId, formatId, profileId);
        ParseResult parsed = parse(csvReader.decode(file.content()), format);

        List<String> fingerprints = fingerprintCalculator.fingerprints(userId, parsed.rows());
        Set<String> existing = fingerprints.isEmpty()
                ? Set.of()
                : transactions.findExistingFingerprints(userId, fingerprints);

        Instant now = clock.instant();
        ImportBatch batch = batches.save(new ImportBatch(userId, safeFileName(file.fileName()), format.displayName(),
                now));
        List<Transaction> fresh = new ArrayList<>();
        for (int i = 0; i < parsed.rows().size(); i++) {
            if (existing.contains(fingerprints.get(i))) {
                continue;
            }
            ParsedTransaction row = parsed.rows().get(i);
            fresh.add(new Transaction(userId, batch.getId(), row.date(), row.amount(), row.currency(),
                    row.description(), merchantNormalizer.normalize(row.description()), fingerprints.get(i), now));
        }
        List<Transaction> saved = transactions.saveAll(fresh);
        CategorizationOutcome outcome = categorizationService.categorize(userId, saved);

        int duplicates = parsed.rows().size() - fresh.size();
        batch.complete(parsed.rowsRead(), saved.size(), duplicates, parsed.skipped(), outcome.needsReview());
        log.info("statement_imported userId={} format={} read={} imported={} duplicates={} skipped={} "
                        + "byRules={} byAi={} needsReview={}", userId, format.id(), parsed.rowsRead(), saved.size(),
                duplicates, parsed.skipped(), outcome.byRules(), outcome.byAi(), outcome.needsReview());
        return new ImportSummary(batch.getId(), batch.getFileName(), format.displayName(), parsed.rowsRead(),
                saved.size(), duplicates, parsed.skipped(), outcome.byRules(), outcome.byAi(), outcome.needsReview(),
                outcome.uncategorized(), outcome.aiAvailable());
    }

    /** Headers and first rows of a file, plus the built-in formats it matches (US-1.4). */
    public CsvPreview preview(ImportFile file) {
        validateFile(file);
        String text = csvReader.decode(file.content());
        char delimiter = csvReader.detectDelimiter(text);
        try (CSVParser parser = csvReader.open(text, delimiter)) {
            List<String> headers = csvReader.headers(parser);
            List<List<String>> rows = parser.stream().limit(PREVIEW_ROWS).map(CSVRecord::toList).toList();
            Set<String> lowerHeaders = headers.stream().map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            List<String> matching = registry.all().stream()
                    .filter(f -> f.delimiter() == delimiter)
                    .filter(f -> f.requiredColumns().stream()
                            .allMatch(c -> lowerHeaders.contains(c.toLowerCase(Locale.ROOT))))
                    .map(StatementFormat::id)
                    .toList();
            return new CsvPreview(String.valueOf(delimiter), headers, rows, matching);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Transactional(readOnly = true)
    public List<ImportBatch> history(UUID userId) {
        return batches.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    private StatementFormat resolveFormat(UUID userId, String formatId, UUID profileId) {
        if ((formatId == null || formatId.isBlank()) == (profileId == null)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Specify exactly one of 'format' or 'profileId'");
        }
        if (profileId != null) {
            return profiles.findByIdAndUserId(profileId, userId)
                    .map(p -> new MappedStatementFormat("profile:" + p.getId(), p.getName(), p.toMapping()))
                    .orElseThrow(() -> new NotFoundException("Import profile", profileId));
        }
        return registry.get(formatId);
    }

    private void validateFile(ImportFile file) {
        if (file.content() == null || file.content().length == 0) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, "The file is empty");
        }
        if (file.content().length > limits.maxFileSizeBytes()) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE,
                    "The file exceeds the maximum size of " + limits.maxFileSizeBytes() / (1024 * 1024) + " MB");
        }
        String name = file.fileName() == null ? "" : file.fileName().toLowerCase(Locale.ROOT);
        String type = file.contentType() == null ? "" : file.contentType().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv") && !CSV_CONTENT_TYPES.contains(type)) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, "Only CSV files can be imported");
        }
    }

    private ParseResult parse(String text, StatementFormat format) {
        try (CSVParser parser = csvReader.open(text, format.delimiter())) {
            List<String> headers = csvReader.headers(parser);
            Set<String> lowerHeaders = headers.stream().map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            List<String> missing = format.requiredColumns().stream()
                    .filter(c -> !lowerHeaders.contains(c.toLowerCase(Locale.ROOT)))
                    .toList();
            if (!missing.isEmpty()) {
                throw new ApiException(ErrorCode.IMPORT_COLUMNS_MISSING,
                        "Missing columns for format '" + format.displayName() + "': " + String.join(", ", missing),
                        Map.of("missingColumns", missing, "foundColumns", headers));
            }
            return readRows(parser, format);
        } catch (IOException | UncheckedIOException e) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, "The file could not be read as CSV");
        }
    }

    private ParseResult readRows(CSVParser parser, StatementFormat format) {
        List<ParsedTransaction> rows = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int read = 0;
        int skipped = 0;
        for (CSVRecord record : parser) {
            read++;
            if (read > limits.maxRows()) {
                throw new ApiException(ErrorCode.IMPORT_FILE_INVALID,
                        "The file has more than " + limits.maxRows() + " rows. Split it into smaller files.");
            }
            try {
                var row = format.parse(record);
                if (row.isPresent()) {
                    rows.add(row.get());
                } else {
                    skipped++;
                }
            } catch (RowParseException e) {
                if (errors.size() < MAX_REPORTED_ERRORS) {
                    errors.add(Map.of("row", record.getRecordNumber() + 1, "message", e.getMessage()));
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.IMPORT_ROWS_INVALID,
                    "Nothing was imported because some rows are invalid", Map.of("rowErrors", errors));
        }
        if (read == 0) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, "The file contains no data rows");
        }
        return new ParseResult(rows, read, skipped);
    }

    private static String safeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "statement.csv";
        }
        String cleaned = name.replaceAll("[\\\\/]", "_").strip();
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private record ParseResult(List<ParsedTransaction> rows, int rowsRead, int skipped) {
    }
}
