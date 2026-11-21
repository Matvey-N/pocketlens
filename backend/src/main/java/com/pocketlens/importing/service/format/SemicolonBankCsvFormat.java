package com.pocketlens.importing.service.format;

import org.springframework.stereotype.Component;

/**
 * Common European bank export: semicolon-separated, {@code dd.MM.yyyy} dates, comma decimals and separate
 * debit/credit columns ({@code Date;Details;Debit;Credit;Currency}).
 */
@Component
public class SemicolonBankCsvFormat extends MappedStatementFormat {

    public SemicolonBankCsvFormat() {
        super("eu-debit-credit", "European bank (Date;Details;Debit;Credit;Currency)",
                new ColumnMapping(';', "Date", "dd.MM.yyyy", "Details", null, "Debit", "Credit", "Currency", null,
                        ','));
    }
}
