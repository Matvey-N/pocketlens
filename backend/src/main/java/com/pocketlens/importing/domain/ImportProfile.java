package com.pocketlens.importing.domain;

import com.pocketlens.importing.service.format.ColumnMapping;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A saved column mapping for a bank without a built-in format (US-1.4). Mapped once, reused for every import.
 */
@Entity
@Table(name = "import_profiles")
public class ImportProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 1)
    private String delimiter;

    @Column(name = "date_column", nullable = false, length = 80)
    private String dateColumn;

    @Column(name = "date_format", nullable = false, length = 40)
    private String dateFormat;

    @Column(name = "description_column", nullable = false, length = 80)
    private String descriptionColumn;

    @Column(name = "amount_column", length = 80)
    private String amountColumn;

    @Column(name = "debit_column", length = 80)
    private String debitColumn;

    @Column(name = "credit_column", length = 80)
    private String creditColumn;

    @Column(name = "currency_column", length = 80)
    private String currencyColumn;

    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    @Column(name = "decimal_separator", nullable = false, length = 1)
    private String decimalSeparator;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ImportProfile() {
    }

    public ImportProfile(UUID userId, String name, ColumnMapping mapping, Instant createdAt) {
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
        apply(mapping);
    }

    public void update(String newName, ColumnMapping mapping) {
        this.name = newName;
        apply(mapping);
    }

    private void apply(ColumnMapping mapping) {
        this.delimiter = String.valueOf(mapping.delimiter());
        this.dateColumn = mapping.date();
        this.dateFormat = mapping.dateFormat();
        this.descriptionColumn = mapping.description();
        this.amountColumn = mapping.amount();
        this.debitColumn = mapping.debit();
        this.creditColumn = mapping.credit();
        this.currencyColumn = mapping.currency();
        this.defaultCurrency = mapping.defaultCurrency();
        this.decimalSeparator = String.valueOf(mapping.decimalSeparator());
    }

    public ColumnMapping toMapping() {
        return new ColumnMapping(delimiter.charAt(0), dateColumn, dateFormat, descriptionColumn, amountColumn,
                debitColumn, creditColumn, currencyColumn, defaultCurrency, decimalSeparator.charAt(0));
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
