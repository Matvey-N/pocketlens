package com.pocketlens.importing.service.format;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/**
 * Revolut account statement export ({@code Type,Product,Started Date,Completed Date,Description,Amount,Fee,
 * Currency,State,Balance}). Only completed transactions are imported; the fee is included in the amount.
 */
@Component
public class RevolutCsvFormat extends MappedStatementFormat {

    private static final String STATE = "State";
    private static final String FEE = "Fee";
    private static final String COMPLETED = "COMPLETED";

    public RevolutCsvFormat() {
        super("revolut", "Revolut (account statement CSV)",
                new ColumnMapping(',', "Completed Date", "yyyy-MM-dd HH:mm:ss", "Description", "Amount", null, null,
                        "Currency", null, '.'));
    }

    @Override
    public List<String> requiredColumns() {
        return List.of("Completed Date", "Description", "Amount", FEE, "Currency", STATE);
    }

    @Override
    public Optional<ParsedTransaction> parse(CSVRecord row) throws RowParseException {
        if (!COMPLETED.equals(value(row, STATE).strip().toUpperCase(Locale.ROOT))) {
            return Optional.empty();
        }
        return super.parse(row);
    }

    @Override
    protected BigDecimal readAmount(CSVRecord row) throws RowParseException {
        BigDecimal amount = super.readAmount(row);
        BigDecimal fee = ValueParsers.parseOptionalAmount(value(row, FEE), mapping().decimalSeparator()).abs();
        return amount.subtract(fee);
    }
}
