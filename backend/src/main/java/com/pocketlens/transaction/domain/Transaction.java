package com.pocketlens.transaction.domain;

import com.pocketlens.categorization.domain.CategorizationSource;
import com.pocketlens.category.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One bank transaction of a user. Negative amounts are expenses, positive amounts are income.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 120)
    private String merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorization_source", nullable = false, length = 10)
    private CategorizationSource categorizationSource = CategorizationSource.NONE;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transaction() {
    }

    public Transaction(UUID userId, UUID importBatchId, LocalDate bookingDate, BigDecimal amount, String currency,
                       String description, String merchant, String fingerprint, Instant createdAt) {
        this.userId = userId;
        this.importBatchId = importBatchId;
        this.bookingDate = bookingDate;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.merchant = merchant;
        this.fingerprint = fingerprint;
        this.createdAt = createdAt;
    }

    public boolean isExpense() {
        return amount.signum() < 0;
    }

    public boolean isManuallyCategorized() {
        return categorizationSource == CategorizationSource.MANUAL;
    }

    /** Category assigned automatically by a rule or the AI classifier. */
    public void categorize(Category newCategory, CategorizationSource source, BigDecimal newConfidence,
                           boolean review) {
        this.category = newCategory;
        this.categorizationSource = source;
        this.confidence = newConfidence;
        this.needsReview = review;
    }

    /** Category chosen by the user; it is never overwritten automatically. */
    public void categorizeManually(Category newCategory) {
        categorize(newCategory, CategorizationSource.MANUAL, null, false);
    }

    /** Clears the category, e.g. after the category was deleted. */
    public void clearCategory() {
        categorize(null, CategorizationSource.NONE, null, true);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getMerchant() {
        return merchant;
    }

    public Category getCategory() {
        return category;
    }

    public CategorizationSource getCategorizationSource() {
        return categorizationSource;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
