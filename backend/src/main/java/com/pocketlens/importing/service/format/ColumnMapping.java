package com.pocketlens.importing.service.format;

/**
 * Which CSV columns contain which value. Either {@code amount} or both {@code debit} and {@code credit} are set.
 *
 * @param dateFormat       java.time pattern, e.g. {@code yyyy-MM-dd} or {@code dd.MM.yyyy}
 * @param currency         column with the currency code, or {@code null} to use {@code defaultCurrency}
 * @param decimalSeparator {@code '.'} or {@code ','}
 */
public record ColumnMapping(char delimiter, String date, String dateFormat, String description, String amount,
                            String debit, String credit, String currency, String defaultCurrency,
                            char decimalSeparator) {

    public boolean usesSplitAmount() {
        return amount == null || amount.isBlank();
    }
}
