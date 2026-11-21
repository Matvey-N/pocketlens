package com.pocketlens.importing.service;

import java.util.UUID;

/**
 * Summary shown after an import (UC-1, step 7).
 *
 * @param rowsRead       data rows in the file
 * @param imported       new transactions stored
 * @param duplicates     rows skipped because they were imported before (US-1.2)
 * @param skipped        rows that are not booked transactions (e.g. declined) or have a zero amount
 * @param needsReview    transactions the user should check
 * @param aiAvailable    {@code false} if AI categorisation was postponed (UC-1 A2)
 */
public record ImportSummary(UUID batchId, String fileName, String format, int rowsRead, int imported,
                            int duplicates, int skipped, int categorizedByRules, int categorizedByAi,
                            int needsReview, int uncategorized, boolean aiAvailable) {
}
