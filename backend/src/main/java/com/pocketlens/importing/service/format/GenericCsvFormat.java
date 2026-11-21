package com.pocketlens.importing.service.format;

import org.springframework.stereotype.Component;

/**
 * PocketLens template: {@code Date,Description,Amount,Currency} with ISO dates. Also used for demo data.
 */
@Component
public class GenericCsvFormat extends MappedStatementFormat {

    public GenericCsvFormat() {
        super("generic", "PocketLens template (Date, Description, Amount, Currency)",
                new ColumnMapping(',', "Date", "yyyy-MM-dd", "Description", "Amount", null, null, "Currency", null,
                        '.'));
    }
}
