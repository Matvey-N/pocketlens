package com.pocketlens.importing.service;

import com.pocketlens.importing.service.format.ParsedTransaction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Computes a de-duplication key per transaction (US-1.2). Identical rows inside one file (e.g. two coffees of the
 * same price on the same day) get different keys through an occurrence counter, while re-importing the same file
 * produces exactly the same keys.
 */
@Component
public class FingerprintCalculator {

    public List<String> fingerprints(UUID userId, List<ParsedTransaction> rows) {
        Map<String, Integer> occurrences = new HashMap<>();
        return rows.stream().map(row -> {
            String base = userId + "|" + row.date() + "|" + row.amount().toPlainString() + "|" + row.currency()
                    + "|" + row.description().strip().toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
            int occurrence = occurrences.merge(base, 1, Integer::sum);
            return sha256(base + "|" + occurrence);
        }).toList();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
