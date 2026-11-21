package com.pocketlens.categorization.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Removes personal identifiers (IBANs, card and account numbers, e-mail addresses, phone numbers) from text
 * before it is sent to the LLM provider (NFR-S3).
 */
@Component
public class PiiSanitizer {

    private static final Pattern IBAN = Pattern.compile(
            "\\b[A-Z]{2}\\d{2}(?:\\s?[A-Z0-9]{4}){2,7}(?:\\s?[A-Z0-9]{1,4})?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){12,19}\\b");
    private static final Pattern MASKED_CARD = Pattern.compile("\\b\\d{4,6}\\*+\\d{2,4}\\b|\\*{2,}\\d{2,4}\\b");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d ()-]{7,}\\d");
    private static final Pattern LONG_NUMBER = Pattern.compile("\\b\\d{6,}\\b");

    public String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String value = IBAN.matcher(text).replaceAll("[IBAN]");
        value = EMAIL.matcher(value).replaceAll("[EMAIL]");
        value = MASKED_CARD.matcher(value).replaceAll("[CARD]");
        value = CARD.matcher(value).replaceAll("[NUMBER]");
        value = PHONE.matcher(value).replaceAll("[NUMBER]");
        value = LONG_NUMBER.matcher(value).replaceAll("[NUMBER]");
        return value;
    }

    /** {@code true} if the text still contains something that looks like an identifier. */
    public boolean containsPii(String text) {
        return text != null && (IBAN.matcher(text).find() || CARD.matcher(text).find()
                || EMAIL.matcher(text).find() || LONG_NUMBER.matcher(text).find());
    }
}
