package com.pocketlens.importing.service.format;

import org.springframework.stereotype.Component;

/**
 * Wise statement export ({@code TransferWise ID,Date,Amount,Currency,Description,...}, dates as dd-MM-yyyy).
 */
@Component
public class WiseCsvFormat extends MappedStatementFormat {

    public WiseCsvFormat() {
        super("wise", "Wise (statement CSV)",
                new ColumnMapping(',', "Date", "dd-MM-yyyy", "Description", "Amount", null, null, "Currency", null,
                        '.'));
    }
}
