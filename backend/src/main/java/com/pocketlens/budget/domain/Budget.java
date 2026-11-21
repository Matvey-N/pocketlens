package com.pocketlens.budget.domain;

import com.pocketlens.category.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Monthly spending limit of one category in one currency (US-3.1). Applies to every month.
 */
@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "monthly_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Budget() {
    }

    public Budget(UUID userId, Category category, BigDecimal monthlyLimit, String currency, Instant createdAt) {
        this.userId = userId;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public void changeLimit(BigDecimal newLimit) {
        this.monthlyLimit = newLimit;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
