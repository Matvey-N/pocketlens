package com.pocketlens.importing.service.format;

import java.util.List;
import java.util.Optional;
import org.apache.commons.csv.CSVRecord;

/**
 * A bank statement format. Supporting a new bank means implementing this interface (NFR-M1, Open/Closed
 * principle); built-in formats are discovered as Spring beans.
 */
public interface StatementFormat {

    /** Stable identifier used by the API, e.g. {@code revolut}. */
    String id();

    String displayName();

    char delimiter();

    /** Column headers that must be present in the file. */
    List<String> requiredColumns();

    /**
     * Parses one row. Returns empty for rows that are not booked transactions (e.g. declined payments).
     *
     * @throws RowParseException if a value cannot be parsed
     */
    Optional<ParsedTransaction> parse(CSVRecord row) throws RowParseException;
}
