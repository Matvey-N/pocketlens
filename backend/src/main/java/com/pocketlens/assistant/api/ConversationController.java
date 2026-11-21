package com.pocketlens.assistant.api;

import com.pocketlens.assistant.api.dto.AskRequest;
import com.pocketlens.assistant.api.dto.ConversationDetailResponse;
import com.pocketlens.assistant.api.dto.ConversationResponse;
import com.pocketlens.assistant.api.dto.ExchangeResponse;
import com.pocketlens.assistant.api.dto.MessageResponse;
import com.pocketlens.assistant.api.dto.SourceTransactionsResponse;
import com.pocketlens.assistant.domain.Conversation;
import com.pocketlens.assistant.service.AnswerSourcesService;
import com.pocketlens.assistant.service.AssistantService;
import com.pocketlens.assistant.service.ConversationService;
import com.pocketlens.common.security.CurrentUserId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI assistant conversations (UC-2, US-4.1 – US-4.4).
 */
@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final AssistantService assistantService;
    private final AnswerSourcesService answerSourcesService;

    public ConversationController(ConversationService conversationService, AssistantService assistantService,
                                  AnswerSourcesService answerSourcesService) {
        this.conversationService = conversationService;
        this.assistantService = assistantService;
        this.answerSourcesService = answerSourcesService;
    }

    @GetMapping
    public List<ConversationResponse> list(@CurrentUserId UUID userId) {
        return conversationService.list(userId).stream().map(ConversationResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> start(@CurrentUserId UUID userId) {
        Conversation conversation = conversationService.start(userId);
        return ResponseEntity.created(URI.create("/api/v1/conversations/" + conversation.getId()))
                .body(ConversationResponse.from(conversation));
    }

    @GetMapping("/{id}")
    public ConversationDetailResponse get(@CurrentUserId UUID userId, @PathVariable UUID id) {
        Conversation c = conversationService.get(userId, id);
        List<MessageResponse> messages = conversationService.messages(userId, id).stream()
                .map(MessageResponse::from).toList();
        return new ConversationDetailResponse(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt(), messages);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        conversationService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ExchangeResponse> ask(@CurrentUserId UUID userId, @PathVariable UUID id,
                                                @Valid @RequestBody AskRequest request) {
        ExchangeResponse response = ExchangeResponse.from(assistantService.ask(userId, id, request.content()));
        return ResponseEntity.created(URI.create("/api/v1/conversations/" + id + "/messages/" + response.answer().id()))
                .body(response);
    }

    /** "Show list": the transactions an answer is based on (US-4.3). */
    @GetMapping("/{id}/messages/{messageId}/transactions")
    public SourceTransactionsResponse sources(@CurrentUserId UUID userId, @PathVariable UUID id,
                                              @PathVariable UUID messageId) {
        return SourceTransactionsResponse.from(answerSourcesService.load(userId, id, messageId));
    }
}
