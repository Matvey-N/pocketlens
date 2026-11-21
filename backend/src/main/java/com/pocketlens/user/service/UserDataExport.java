package com.pocketlens.user.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * All personal data of a user in a portable format (GDPR Art. 15 and 20, US-6.3).
 */
public record UserDataExport(Instant exportedAt, Map<String, Object> profile, List<Map<String, Object>> categories,
                             List<Map<String, Object>> rules, List<Map<String, Object>> budgets,
                             List<Map<String, Object>> transactions, List<Map<String, Object>> conversations) {
}
