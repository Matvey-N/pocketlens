package com.pocketlens.assistant.service;

import com.pocketlens.assistant.domain.Conversation;
import com.pocketlens.assistant.domain.Message;
import com.pocketlens.assistant.persistence.ConversationRepository;
import com.pocketlens.assistant.persistence.MessageRepository;
import com.pocketlens.common.error.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conversations with the assistant (US-4.4).
 */
@Service
public class ConversationService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final Clock clock;

    public ConversationService(ConversationRepository conversations, MessageRepository messages, Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Conversation> list(UUID userId) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public Conversation start(UUID userId) {
        return conversations.save(new Conversation(userId, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Conversation get(UUID userId, UUID conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation", conversationId));
    }

    @Transactional(readOnly = true)
    public List<Message> messages(UUID userId, UUID conversationId) {
        get(userId, conversationId);
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional(readOnly = true)
    public Message message(UUID userId, UUID conversationId, UUID messageId) {
        get(userId, conversationId);
        return messages.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> new NotFoundException("Message", messageId));
    }

    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        conversations.delete(get(userId, conversationId));
    }
}
