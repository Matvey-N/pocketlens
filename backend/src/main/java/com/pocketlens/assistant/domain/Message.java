package com.pocketlens.assistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A message of a conversation. Assistant messages carry the interpretation of the question (US-4.2) and the ids
 * of the transactions behind the answer (US-4.3).
 */
@Entity
@Table(name = "messages")
public class Message {

    public enum Role { USER, ASSISTANT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interpretation", columnDefinition = "jsonb")
    private List<String> interpretation = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_calls", columnDefinition = "jsonb")
    private List<ToolCallRecord> toolCalls = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_transaction_ids", columnDefinition = "jsonb")
    private List<UUID> sourceTransactionIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Message() {
    }

    private Message(UUID conversationId, Role role, String content, Instant createdAt) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Message user(UUID conversationId, String content, Instant now) {
        return new Message(conversationId, Role.USER, content, now);
    }

    public static Message assistant(UUID conversationId, String content, List<String> interpretation,
                                    List<ToolCallRecord> toolCalls, List<UUID> sourceTransactionIds, Instant now) {
        Message message = new Message(conversationId, Role.ASSISTANT, content, now);
        message.interpretation = new ArrayList<>(interpretation);
        message.toolCalls = new ArrayList<>(toolCalls);
        message.sourceTransactionIds = new ArrayList<>(sourceTransactionIds);
        return message;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<String> getInterpretation() {
        return interpretation == null ? List.of() : List.copyOf(interpretation);
    }

    public List<ToolCallRecord> getToolCalls() {
        return toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public List<UUID> getSourceTransactionIds() {
        return sourceTransactionIds == null ? List.of() : List.copyOf(sourceTransactionIds);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
