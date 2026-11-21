package com.pocketlens.categorization.service;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Turns a raw bank description into a stable merchant key, e.g. {@code "CAFE BLOOM 0425"} → {@code "CAFE BLOOM"}
 * and {@code "Card payment PAYPAL *STEAM"} → {@code "PAYPAL STEAM"}. Rules match on this key.
 */
@Component
public class MerchantNormalizer {

    private static final int MAX_LENGTH = 120;
    private static final Pattern NOISE_PREFIX = Pattern.compile(
            "^(CARD PAYMENT( TO)?|PAYMENT TO|PURCHASE( AT)?|POS( PURCHASE)?|CONTACTLESS( PAYMENT)?|DEBIT CARD)\\s+");
    private static final Pattern TOKEN_WITH_DIGITS = Pattern.compile("\\b\\S*\\d{3,}\\S*\\b");
    private static final Pattern NON_TEXT = Pattern.compile("[^\\p{L}\\p{N}&.' -]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    public String normalize(String description) {
        if (description == null) {
            return "";
        }
        String value = description.toUpperCase(Locale.ROOT).strip();
        value = NON_TEXT.matcher(value).replaceAll(" ");
        value = TOKEN_WITH_DIGITS.matcher(value).replaceAll(" ");
        value = SPACES.matcher(value).replaceAll(" ").strip();
        value = NOISE_PREFIX.matcher(value).replaceFirst("");
        value = value.replaceAll("^[.\\-' ]+|[.\\-' ]+$", "");
        if (value.isBlank()) {
            value = SPACES.matcher(description.toUpperCase(Locale.ROOT)).replaceAll(" ").strip();
        }
        return value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH).strip() : value;
    }
}
