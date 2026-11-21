package com.pocketlens.assistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A chat with the assistant (US-4.4).
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    private static final int MAX_TITLE = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = MAX_TITLE)
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Conversation() {
    }

    public Conversation(UUID userId, Instant now) {
        this.userId = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Uses the first question as the title. */
    public void touch(String question, Instant now) {
        if (title == null && question != null) {
            String clean = question.strip().replaceAll("\\s+", " ");
            title = clean.length() > MAX_TITLE ? clean.substring(0, MAX_TITLE - 1) + "…" : clean;
        }
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
