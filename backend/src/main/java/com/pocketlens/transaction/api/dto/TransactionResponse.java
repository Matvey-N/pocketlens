package com.pocketlens.transaction.api.dto;

import com.pocketlens.category.domain.Category;
import com.pocketlens.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(UUID id, LocalDate date, BigDecimal amount, String currency, String description,
                                  String merchant, CategoryRef category, String categorizationSource,
                                  BigDecimal confidence, boolean needsReview) {

    /** Short reference to the category of a transaction. */
    public record CategoryRef(UUID id, String name, String color) {
        static CategoryRef from(Category category) {
            return category == null ? null : new CategoryRef(category.getId(), category.getName(), category.getColor());
        }
    }

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getBookingDate(), t.getAmount(), t.getCurrency(),
                t.getDescription(), t.getMerchant(), CategoryRef.from(t.getCategory()),
                t.getCategorizationSource().name(), t.getConfidence(), t.isNeedsReview());
    }
}
