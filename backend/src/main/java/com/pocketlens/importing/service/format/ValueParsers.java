package com.pocketlens.importing.service.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parsing of amounts and dates in the formats banks use ({@code 1 234,56}, {@code -12.50}, {@code (8.00)},
 * {@code €12.50}; {@code 2026-11-03}, {@code 03.11.2026 10:15}).
 */
public final class ValueParsers {

    private static final Pattern NOT_NUMBER = Pattern.compile("[^0-9,.\\-+]");
    private static final Pattern CURRENCY_CODE = Pattern.compile("^[A-Z]{3}$");

    private ValueParsers() {
    }

    public static BigDecimal parseAmount(String raw, char decimalSeparator) throws RowParseException {
        if (raw == null || raw.isBlank()) {
            throw new RowParseException("amount is empty");
        }
        String value = raw.strip();
        boolean negative = value.startsWith("(") && value.endsWith(")");
        value = NOT_NUMBER.matcher(value).replaceAll("");
        if (decimalSeparator == ',') {
            value = value.replace(".", "").replace(',', '.');
        } else {
            value = value.replace(",", "");
        }
        if (value.isEmpty() || "-".equals(value) || "+".equals(value)) {
            throw new RowParseException("amount '" + raw + "' is not a number");
        }
        try {
            BigDecimal amount = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
            return negative ? amount.abs().negate() : amount;
        } catch (NumberFormatException e) {
            throw new RowParseException("amount '" + raw + "' is not a number");
        }
    }

    /** Parses an optional amount column: empty values count as zero. */
    public static BigDecimal parseOptionalAmount(String raw, char decimalSeparator) throws RowParseException {
        return raw == null || raw.isBlank() ? BigDecimal.ZERO : parseAmount(raw, decimalSeparator);
    }

    public static LocalDate parseDate(String raw, String pattern) throws RowParseException {
        if (raw == null || raw.isBlank()) {
            throw new RowParseException("date is empty");
        }
        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(pattern, Locale.ROOT);
        } catch (IllegalArgumentException e) {
            throw new RowParseException("date format '" + pattern + "' is invalid");
        }
        String value = raw.strip();
        try {
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException dateOnly) {
            try {
                return LocalDateTime.parse(value, formatter).toLocalDate();
            } catch (DateTimeParseException withTime) {
                throw new RowParseException("date '" + raw + "' does not match " + pattern);
            }
        }
    }

    public static String parseCurrency(String raw) throws RowParseException {
        String value = raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT);
        if (!CURRENCY_CODE.matcher(value).matches()) {
            throw new RowParseException("currency '" + raw + "' is not a 3-letter code");
        }
        return value;
    }
}
