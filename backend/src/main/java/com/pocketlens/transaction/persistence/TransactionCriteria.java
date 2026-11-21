package com.pocketlens.transaction.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

/**
 * Filter for transaction queries. Only {@code userId} is mandatory; every query is scoped by it (NFR-S2).
 *
 * @param categoryId   only this category; combine with {@code uncategorized=false}
 * @param uncategorized only transactions without a category
 * @param merchant     substring of the normalised merchant (case-insensitive)
 * @param text         substring of description or merchant (case-insensitive)
 * @param expensesOnly only expenses (negative amounts outside income/transfer categories)
 * @param ids          only these transaction ids
 */
public record TransactionCriteria(UUID userId, LocalDate from, LocalDate to, UUID categoryId, Boolean uncategorized,
                                  String merchant, String text, String currency, Boolean needsReview,
                                  boolean expensesOnly, Collection<UUID> ids) {

    public static Builder forUser(UUID userId) {
        return new Builder(userId);
    }

    /** Fluent builder, because most criteria are optional. */
    public static final class Builder {
        private final UUID userId;
        private LocalDate from;
        private LocalDate to;
        private UUID categoryId;
        private Boolean uncategorized;
        private String merchant;
        private String text;
        private String currency;
        private Boolean needsReview;
        private boolean expensesOnly;
        private Collection<UUID> ids;

        private Builder(UUID userId) {
            this.userId = userId;
        }

        public Builder period(LocalDate fromDate, LocalDate toDate) {
            this.from = fromDate;
            this.to = toDate;
            return this;
        }

        public Builder category(UUID id) {
            this.categoryId = id;
            return this;
        }

        public Builder uncategorized(Boolean value) {
            this.uncategorized = value;
            return this;
        }

        public Builder merchant(String value) {
            this.merchant = value;
            return this;
        }

        public Builder text(String value) {
            this.text = value;
            return this;
        }

        public Builder currency(String value) {
            this.currency = value;
            return this;
        }

        public Builder needsReview(Boolean value) {
            this.needsReview = value;
            return this;
        }

        public Builder expensesOnly() {
            this.expensesOnly = true;
            return this;
        }

        public Builder ids(Collection<UUID> value) {
            this.ids = value;
            return this;
        }

        public TransactionCriteria build() {
            return new TransactionCriteria(userId, from, to, categoryId, uncategorized, merchant, text, currency,
                    needsReview, expensesOnly, ids);
        }
    }
}
