package com.pocketlens.assistant.api.dto;

import com.pocketlens.assistant.domain.Message;
import com.pocketlens.assistant.domain.ToolCallRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A message. For assistant messages {@code interpretation} explains how the question was understood and
 * {@code sourceTransactionCount} tells how many transactions can be shown with "Show list".
 */
public record MessageResponse(UUID id, String role, String content, List<String> interpretation,
                              List<ToolCallRecord> toolCalls, int sourceTransactionCount, Instant createdAt) {

    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getRole().name(), m.getContent(), m.getInterpretation(),
                m.getToolCalls(), m.getSourceTransactionIds().size(), m.getCreatedAt());
    }
}
