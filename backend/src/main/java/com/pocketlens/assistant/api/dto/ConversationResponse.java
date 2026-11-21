package com.pocketlens.assistant.api.dto;

import com.pocketlens.assistant.domain.Conversation;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(UUID id, String title, Instant createdAt, Instant updatedAt) {

    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
