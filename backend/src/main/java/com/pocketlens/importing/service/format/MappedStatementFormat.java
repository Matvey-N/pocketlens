package com.pocketlens.importing.service.format;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.csv.CSVRecord;

/**
 * Format driven by a {@link ColumnMapping}. Used for user-defined mappings (US-1.4) and as the base of the
 * built-in presets.
 */
public class MappedStatementFormat implements StatementFormat {

    private static final int MAX_DESCRIPTION = 255;

    private final String id;
    private final String displayName;
    private final ColumnMapping mapping;

    public MappedStatementFormat(String id, String displayName, ColumnMapping mapping) {
        this.id = id;
        this.displayName = displayName;
        this.mapping = mapping;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public char delimiter() {
        return mapping.delimiter();
    }

    protected ColumnMapping mapping() {
        return mapping;
    }

    @Override
    public List<String> requiredColumns() {
        List<String> columns = new ArrayList<>(List.of(mapping.date(), mapping.description()));
        if (mapping.usesSplitAmount()) {
            columns.add(mapping.debit());
            columns.add(mapping.credit());
        } else {
            columns.add(mapping.amount());
        }
        if (mapping.currency() != null && !mapping.currency().isBlank()) {
            columns.add(mapping.currency());
        }
        return columns;
    }

    @Override
    public Optional<ParsedTransaction> parse(CSVRecord row) throws RowParseException {
        BigDecimal amount = readAmount(row);
        if (amount.signum() == 0) {
            return Optional.empty();
        }
        String currency = mapping.currency() == null || mapping.currency().isBlank()
                ? ValueParsers.parseCurrency(mapping.defaultCurrency())
                : ValueParsers.parseCurrency(value(row, mapping.currency()));
        String description = value(row, mapping.description()).strip();
        if (description.isEmpty()) {
            throw new RowParseException("description is empty");
        }
        if (description.length() > MAX_DESCRIPTION) {
            description = description.substring(0, MAX_DESCRIPTION);
        }
        return Optional.of(new ParsedTransaction(
                ValueParsers.parseDate(value(row, mapping.date()), mapping.dateFormat()), amount, currency,
                description));
    }

    /** Signed amount; with separate debit/credit columns debit becomes negative. */
    protected BigDecimal readAmount(CSVRecord row) throws RowParseException {
        char separator = mapping.decimalSeparator();
        if (!mapping.usesSplitAmount()) {
            return ValueParsers.parseAmount(value(row, mapping.amount()), separator);
        }
        BigDecimal debit = ValueParsers.parseOptionalAmount(value(row, mapping.debit()), separator).abs();
        BigDecimal credit = ValueParsers.parseOptionalAmount(value(row, mapping.credit()), separator).abs();
        return credit.subtract(debit);
    }

    protected static String value(CSVRecord row, String column) {
        return row.isMapped(column) && row.isSet(column) ? row.get(column) : "";
    }
}
