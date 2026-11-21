package com.pocketlens.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Token usage of one LLM call (for cost control, US-7.2). Contains no message content.
 */
@Entity
@Table(name = "ai_usage")
public class AiUsageRecord {

    public enum Purpose { CATEGORIZATION, ASSISTANT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiUsageRecord() {
    }

    public AiUsageRecord(UUID userId, Purpose purpose, int inputTokens, int outputTokens, Instant createdAt) {
        this.userId = userId;
        this.purpose = purpose;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
