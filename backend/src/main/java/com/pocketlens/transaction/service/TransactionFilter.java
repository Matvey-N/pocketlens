package com.pocketlens.transaction.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filter of the transaction list (query parameters of GET /transactions).
 */
public record TransactionFilter(LocalDate from, LocalDate to, UUID categoryId, Boolean uncategorized,
                                Boolean needsReview, String q, String currency) {
}
