package com.pocketlens.assistant.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.assistant.domain.Conversation;
import com.pocketlens.assistant.domain.Message;
import com.pocketlens.assistant.persistence.ConversationRepository;
import com.pocketlens.assistant.persistence.MessageRepository;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.support.TestData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationServiceTest {

    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final MessageRepository messages = mock(MessageRepository.class);
    private final ConversationService service = new ConversationService(conversations, messages, TestData.clock());
    private final Conversation conversation = TestData.withId(new Conversation(USER_ID, TestData.NOW),
            UUID.randomUUID());

    @Test
    void startsListsAndDeletesConversations() {
        when(conversations.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.start(USER_ID).getUserId()).isEqualTo(USER_ID);

        when(conversations.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of(conversation));
        assertThat(service.list(USER_ID)).containsExactly(conversation);

        when(conversations.findByIdAndUserId(conversation.getId(), USER_ID)).thenReturn(Optional.of(conversation));
        service.delete(USER_ID, conversation.getId());
        verify(conversations).delete(conversation);
    }

    @Test
    void readsMessagesOfOwnConversationsOnly() {
        Message message = TestData.withId(Message.user(conversation.getId(), "hi", TestData.NOW), UUID.randomUUID());
        when(conversations.findByIdAndUserId(conversation.getId(), USER_ID)).thenReturn(Optional.of(conversation));
        when(messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId())).thenReturn(List.of(message));
        when(messages.findByIdAndConversationId(message.getId(), conversation.getId())).thenReturn(Optional.of(message));

        assertThat(service.messages(USER_ID, conversation.getId())).containsExactly(message);
        assertThat(service.message(USER_ID, conversation.getId(), message.getId())).isEqualTo(message);

        UUID foreign = UUID.randomUUID();
        when(conversations.findByIdAndUserId(foreign, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.messages(USER_ID, foreign)).isInstanceOf(NotFoundException.class);
        UUID missingMessage = UUID.randomUUID();
        when(messages.findByIdAndConversationId(missingMessage, conversation.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.message(USER_ID, conversation.getId(), missingMessage))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void conversationTitleComesFromFirstQuestion() {
        Conversation c = new Conversation(USER_ID, TestData.NOW);
        c.touch("  How   much did I spend?  ", TestData.NOW);
        c.touch("Second question", TestData.NOW.plusSeconds(5));
        assertThat(c.getTitle()).isEqualTo("How much did I spend?");
        assertThat(c.getUpdatedAt()).isEqualTo(TestData.NOW.plusSeconds(5));
        Conversation longOne = new Conversation(USER_ID, TestData.NOW);
        longOne.touch("x".repeat(200), TestData.NOW);
        assertThat(longOne.getTitle()).hasSize(80).endsWith("…");
    }
}
