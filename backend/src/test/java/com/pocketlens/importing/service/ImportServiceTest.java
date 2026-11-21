package com.pocketlens.importing.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.categorization.service.CategorizationOutcome;
import com.pocketlens.categorization.service.CategorizationService;
import com.pocketlens.categorization.service.MerchantNormalizer;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.importing.domain.ImportBatch;
import com.pocketlens.importing.domain.ImportProfile;
import com.pocketlens.importing.persistence.ImportBatchRepository;
import com.pocketlens.importing.persistence.ImportProfileRepository;
import com.pocketlens.importing.service.format.ColumnMapping;
import com.pocketlens.importing.service.format.GenericCsvFormat;
import com.pocketlens.importing.service.format.RevolutCsvFormat;
import com.pocketlens.importing.service.format.SemicolonBankCsvFormat;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ImportServiceTest {

    private static final String GENERIC_CSV = """
            Date,Description,Amount,Currency
            2026-11-01,LIDL 1234 VILNIUS,-12.30,EUR
            2026-11-02,CAFE BLOOM 0425,-4.50,EUR
            2026-11-02,CAFE BLOOM 0425,-4.50,EUR
            2026-11-03,Salary,2000.00,EUR
            """;

    private final ImportProfileRepository profiles = mock(ImportProfileRepository.class);
    private final ImportBatchRepository batches = mock(ImportBatchRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final CategorizationService categorization = mock(CategorizationService.class);
    private final FingerprintCalculator fingerprints = new FingerprintCalculator();
    private ImportService service;

    @BeforeEach
    void setUp() {
        StatementFormatRegistry registry = new StatementFormatRegistry(List.of(new GenericCsvFormat(),
                new RevolutCsvFormat(), new SemicolonBankCsvFormat()));
        service = new ImportService(registry, profiles, batches, transactions, new CsvReader(), fingerprints,
                new MerchantNormalizer(), categorization, TestData.properties(), TestData.clock());
        when(batches.save(any())).thenAnswer(i -> TestData.withId(i.getArgument(0), UUID.randomUUID()));
        when(transactions.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(categorization.categorize(eq(USER_ID), anyList()))
                .thenReturn(new CategorizationOutcome(1, 1, 1, 0, true));
    }

    @Test
    void importsNewRowsAndSkipsDuplicates() {
        String alreadyImported = fingerprints.fingerprints(USER_ID, List.of(
                new com.pocketlens.importing.service.format.ParsedTransaction(java.time.LocalDate.of(2026, 11, 1),
                        new java.math.BigDecimal("-12.30"), "EUR", "LIDL 1234 VILNIUS"))).get(0);
        when(transactions.findExistingFingerprints(eq(USER_ID), anyCollection())).thenReturn(Set.of(alreadyImported));

        ImportSummary summary = service.importStatement(USER_ID, file(GENERIC_CSV), "generic", null);

        assertThat(summary.rowsRead()).isEqualTo(4);
        assertThat(summary.imported()).isEqualTo(3);
        assertThat(summary.duplicates()).isEqualTo(1);
        assertThat(summary.needsReview()).isEqualTo(1);
        assertThat(summary.aiAvailable()).isTrue();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Transaction>> saved = ArgumentCaptor.forClass(List.class);
        verify(transactions).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Transaction::getMerchant)
                .containsExactly("CAFE BLOOM", "CAFE BLOOM", "SALARY");
    }

    @Test
    void importsWithSavedProfile() {
        UUID profileId = UUID.randomUUID();
        ImportProfile profile = TestData.withId(new ImportProfile(USER_ID, "My bank",
                new ColumnMapping(';', "Datum", "dd.MM.yyyy", "Text", "Betrag", null, null, null, "EUR", ','),
                TestData.NOW), profileId);
        when(profiles.findByIdAndUserId(profileId, USER_ID)).thenReturn(Optional.of(profile));
        when(transactions.findExistingFingerprints(eq(USER_ID), anyCollection())).thenReturn(Set.of());

        ImportSummary summary = service.importStatement(USER_ID,
                file("Datum;Text;Betrag\n01.11.2026;Bäckerei;-3,50\n"), null, profileId);

        assertThat(summary.imported()).isEqualTo(1);
        assertThat(summary.format()).isEqualTo("My bank");
    }

    @Test
    void rejectsFileWithMissingColumnsAndImportsNothing() {
        assertThatThrownBy(() -> service.importStatement(USER_ID, file("Date,Text\n2026-11-01,x\n"), "generic", null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.code()).isEqualTo(ErrorCode.IMPORT_COLUMNS_MISSING);
                    assertThat(api.details()).containsKey("missingColumns");
                });
        verify(batches, never()).save(any());
    }

    @Test
    void rejectsFileWithInvalidRows() {
        String csv = "Date,Description,Amount,Currency\n2026-13-01,X,-1,EUR\n2026-11-01,Y,abc,EUR\n";
        assertThatThrownBy(() -> service.importStatement(USER_ID, file(csv), "generic", null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.code()).isEqualTo(ErrorCode.IMPORT_ROWS_INVALID);
                    assertThat((List<?>) api.details().get("rowErrors")).hasSize(2);
                });
    }

    @Test
    void validatesFileAndFormatSelection() {
        assertCode(() -> service.importStatement(USER_ID, new ImportFile("a.csv", "text/csv", new byte[0]),
                "generic", null), ErrorCode.IMPORT_FILE_INVALID);
        assertCode(() -> service.importStatement(USER_ID, new ImportFile("a.pdf", "application/pdf",
                "x".getBytes(StandardCharsets.UTF_8)), "generic", null), ErrorCode.IMPORT_FILE_INVALID);
        assertCode(() -> service.importStatement(USER_ID, new ImportFile("a.csv", "text/csv",
                new byte[6 * 1024 * 1024]), "generic", null), ErrorCode.FILE_TOO_LARGE);
        assertCode(() -> service.importStatement(USER_ID, file(GENERIC_CSV), null, null), ErrorCode.VALIDATION_FAILED);
        assertCode(() -> service.importStatement(USER_ID, file(GENERIC_CSV), "unknown", null),
                ErrorCode.VALIDATION_FAILED);
        assertCode(() -> service.importStatement(USER_ID, file("Date,Description,Amount,Currency\n"), "generic", null),
                ErrorCode.IMPORT_FILE_INVALID);
        UUID missingProfile = UUID.randomUUID();
        when(profiles.findByIdAndUserId(missingProfile, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.importStatement(USER_ID, file(GENERIC_CSV), null, missingProfile))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void readsWindows1252AndBom() {
        when(transactions.findExistingFingerprints(eq(USER_ID), anyCollection())).thenReturn(Set.of());
        byte[] latin = "Date,Description,Amount,Currency\n2026-11-01,Café,-1.00,EUR\n"
                .getBytes(Charset.forName("windows-1252"));
        assertThat(service.importStatement(USER_ID, new ImportFile("s.csv", null, latin), "generic", null).imported())
                .isEqualTo(1);

        String withBom = "﻿Date,Description,Amount,Currency\n2026-11-01,Tea,-1.00,EUR\n";
        assertThat(service.importStatement(USER_ID, file(withBom), "generic", null).imported()).isEqualTo(1);
    }

    @Test
    void previewDetectsDelimiterAndMatchingFormats() {
        CsvPreview preview = service.preview(file("Date;Details;Debit;Credit;Currency\n03.11.2026;Maxima;5,00;;EUR\n"));

        assertThat(preview.delimiter()).isEqualTo(";");
        assertThat(preview.headers()).containsExactly("Date", "Details", "Debit", "Credit", "Currency");
        assertThat(preview.rows()).hasSize(1);
        assertThat(preview.matchingFormats()).containsExactly("eu-debit-credit");
    }

    @Test
    void returnsHistory() {
        ImportBatch batch = new ImportBatch(USER_ID, "a.csv", "generic", TestData.NOW);
        when(batches.findTop20ByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(batch));
        assertThat(service.history(USER_ID)).containsExactly(batch);
    }

    private static ImportFile file(String content) {
        return new ImportFile("statement.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(code);
    }
}
