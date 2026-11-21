package com.pocketlens.categorization.domain;

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
import java.time.Instant;
import java.util.UUID;

/**
 * "Merchant contains pattern → category". Global rules ({@code userId == null}) are managed by administrators;
 * user rules are created from corrections (US-2.3) and always win over global rules.
 */
@Entity
@Table(name = "merchant_rules")
public class MerchantRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String pattern;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MerchantRule() {
    }

    public MerchantRule(UUID userId, String pattern, Category category, Instant createdAt) {
        this.userId = userId;
        this.pattern = pattern;
        this.category = category;
        this.createdAt = createdAt;
    }

    public boolean isGlobal() {
        return userId == null;
    }

    /** The pattern must start at the beginning of a word: "IKI" matches "IKI VILNIUS" but not "WIKIPEDIA". */
    public boolean matches(String merchant) {
        return merchant != null && (merchant.startsWith(pattern) || merchant.contains(" " + pattern));
    }

    public void changeCategory(Category newCategory) {
        this.category = newCategory;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPattern() {
        return pattern;
    }

    public Category getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
