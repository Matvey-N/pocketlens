package com.pocketlens.categorization.service;

/**
 * Counts of a categorisation run (shown in the import summary).
 *
 * @param aiAvailable {@code false} if AI was needed but unavailable; those transactions stay uncategorised and are
 *                    retried later (UC-1 A2)
 */
public record CategorizationOutcome(int byRules, int byAi, int needsReview, int uncategorized, boolean aiAvailable) {

    public static CategorizationOutcome empty() {
        return new CategorizationOutcome(0, 0, 0, 0, true);
    }
}
