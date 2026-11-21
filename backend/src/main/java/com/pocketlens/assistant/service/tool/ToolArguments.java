package com.pocketlens.assistant.service.tool;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

/**
 * Typed, validated access to the arguments the model passed to a tool.
 */
public final class ToolArguments {

    static final int MAX_RANGE_DAYS = 731;

    private final Map<String, Object> values;

    public ToolArguments(Map<String, Object> values) {
        this.values = values == null ? Map.of() : values;
    }

    public Map<String, Object> raw() {
        return values;
    }

    public LocalDate date(String name) throws ToolInputException {
        Object value = values.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ToolInputException("'" + name + "' is required (YYYY-MM-DD)");
        }
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException e) {
            throw new ToolInputException("'" + name + "' must be a date in the format YYYY-MM-DD");
        }
    }

    /** Validated period {@code from}..{@code to} (inclusive, at most two years). */
    public Period period() throws ToolInputException {
        LocalDate from = date("from");
        LocalDate to = date("to");
        if (from.isAfter(to)) {
            throw new ToolInputException("'from' must not be after 'to'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new ToolInputException("The period must not be longer than two years");
        }
        return new Period(from, to);
    }

    public String optionalText(String name) {
        Object value = values.get(name);
        return value instanceof String text && !text.isBlank() ? text.strip() : null;
    }

    public String choice(String name, Set<String> allowed, String defaultValue) throws ToolInputException {
        String value = optionalText(name);
        if (value == null) {
            return defaultValue;
        }
        if (!allowed.contains(value)) {
            throw new ToolInputException("'" + name + "' must be one of " + allowed);
        }
        return value;
    }

    public int integer(String name, int defaultValue, int min, int max) throws ToolInputException {
        Object value = values.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)) {
            throw new ToolInputException("'" + name + "' must be a number");
        }
        int result = number.intValue();
        if (result < min || result > max) {
            throw new ToolInputException("'" + name + "' must be between " + min + " and " + max);
        }
        return result;
    }

    public YearMonth optionalMonth(String name, YearMonth defaultValue) throws ToolInputException {
        String value = optionalText(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new ToolInputException("'" + name + "' must be a month in the format YYYY-MM");
        }
    }

    /** Inclusive date range. */
    public record Period(LocalDate from, LocalDate to) {
        @Override
        public String toString() {
            return from + " – " + to;
        }
    }
}
