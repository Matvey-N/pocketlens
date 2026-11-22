package com.pocketlens.assistant.service;

import com.pocketlens.ai.domain.AiUsageRecord;
import com.pocketlens.ai.llm.LlmClient;
import com.pocketlens.ai.llm.LlmContent;
import com.pocketlens.ai.llm.LlmMessage;
import com.pocketlens.ai.llm.LlmRequest;
import com.pocketlens.ai.llm.LlmResponse;
import com.pocketlens.ai.service.AiUsageService;
import com.pocketlens.assistant.domain.Conversation;
import com.pocketlens.assistant.domain.Message;
import com.pocketlens.assistant.domain.ToolCallRecord;
import com.pocketlens.assistant.persistence.ConversationRepository;
import com.pocketlens.assistant.persistence.MessageRepository;
import com.pocketlens.assistant.service.tool.AssistantTool;
import com.pocketlens.assistant.service.tool.ToolArguments;
import com.pocketlens.assistant.service.tool.ToolContext;
import com.pocketlens.assistant.service.tool.ToolInputException;
import com.pocketlens.assistant.service.tool.ToolRegistry;
import com.pocketlens.assistant.service.tool.ToolResult;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.config.AppProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answers spending questions with tool calling (UC-2). The model only interprets the question and chooses tools;
 * every figure comes from a user-scoped database query. The answer carries the interpretation (US-4.2) and the ids
 * of the transactions used (US-4.3).
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private static final int MAX_TOKENS = 1024;
    private static final int MAX_SOURCE_IDS = 1000;
    static final String FALLBACK_ANSWER = "I could not complete the analysis for this question. "
            + "Please try to ask it in a simpler way or for a shorter period.";

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final PromptGuard promptGuard;
    private final SlidingWindowRateLimiter rateLimiter;
    private final ConversationService conversationService;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final CategoryService categoryService;
    private final AiUsageService usageService;
    private final JsonMapper jsonMapper;
    private final AppProperties.Assistant settings;
    private final Clock clock;

    @SuppressWarnings("java:S107") // orchestrating service: dependencies are explicit collaborators
    public AssistantService(LlmClient llmClient, ToolRegistry toolRegistry, PromptGuard promptGuard,
                            SlidingWindowRateLimiter rateLimiter, ConversationService conversationService,
                            ConversationRepository conversations, MessageRepository messages,
                            CategoryService categoryService, AiUsageService usageService, JsonMapper jsonMapper,
                            AppProperties properties, Clock clock) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.promptGuard = promptGuard;
        this.rateLimiter = rateLimiter;
        this.conversationService = conversationService;
        this.conversations = conversations;
        this.messages = messages;
        this.categoryService = categoryService;
        this.usageService = usageService;
        this.jsonMapper = jsonMapper;
        this.settings = properties.assistant();
        this.clock = clock;
    }

    /**
     * Stores the question, runs the tool-calling loop and stores the answer. If the provider is unavailable the
     * question stays in the conversation and {@code AI_UNAVAILABLE} is returned (UC-2 A2).
     */
    public AssistantAnswer ask(UUID userId, UUID conversationId, String rawQuestion) {
        String question = promptGuard.check(rawQuestion);
        Conversation conversation = conversationService.get(userId, conversationId);
        rateLimiter.acquire(userId);

        List<LlmMessage> dialogue = new ArrayList<>(history(conversationId));
        Message stored = messages.save(Message.user(conversationId, question, clock.instant()));
        conversation.touch(question, clock.instant());
        conversations.save(conversation);
        dialogue.add(LlmMessage.user(question));

        ToolContext context = new ToolContext(userId, LocalDate.now(clock));
        LoopResult result = runToolLoop(userId, context, dialogue);

        Message answer = messages.save(Message.assistant(conversationId, result.answer(), result.interpretations(),
                result.calls(), result.sourceIds(), clock.instant()));
        log.info("assistant_answered userId={} conversationId={} toolCalls={} sources={}", userId, conversationId,
                result.calls().size(), result.sourceIds().size());
        return new AssistantAnswer(stored, answer);
    }

    private LoopResult runToolLoop(UUID userId, ToolContext context, List<LlmMessage> dialogue) {
        String system = systemPrompt(context);
        List<String> interpretations = new ArrayList<>();
        List<ToolCallRecord> calls = new ArrayList<>();
        Set<UUID> sourceIds = new LinkedHashSet<>();
        for (int iteration = 0; iteration < settings.maxToolIterations(); iteration++) {
            LlmResponse response = llmClient.send(new LlmRequest(system, dialogue, toolRegistry.definitions(), null,
                    MAX_TOKENS));
            usageService.record(userId, AiUsageRecord.Purpose.ASSISTANT, response);
            if (!response.wantsTools()) {
                String text = response.text();
                return new LoopResult(text.isBlank() ? FALLBACK_ANSWER : text, interpretations, calls,
                        List.copyOf(sourceIds));
            }
            dialogue.add(new LlmMessage(LlmMessage.Role.ASSISTANT, response.content()));
            List<LlmContent> results = new ArrayList<>();
            for (LlmContent.ToolUse use : response.toolUses()) {
                results.add(execute(use, context, interpretations, calls, sourceIds));
            }
            dialogue.add(new LlmMessage(LlmMessage.Role.USER, results));
        }
        log.warn("assistant_tool_loop_exhausted userId={}", userId);
        return new LoopResult(FALLBACK_ANSWER, interpretations, calls, List.copyOf(sourceIds));
    }

    private LlmContent execute(LlmContent.ToolUse use, ToolContext context, List<String> interpretations,
                               List<ToolCallRecord> calls, Set<UUID> sourceIds) {
        Optional<AssistantTool> tool = toolRegistry.find(use.name());
        if (tool.isEmpty()) {
            calls.add(new ToolCallRecord(use.name(), use.input(), null, true));
            return new LlmContent.ToolResult(use.id(), "Unknown tool " + use.name(), true);
        }
        try {
            ToolResult result = tool.get().execute(context, new ToolArguments(use.input()));
            interpretations.add(result.interpretation());
            calls.add(new ToolCallRecord(use.name(), use.input(), result.interpretation(), false));
            result.transactionIds().stream().limit(MAX_SOURCE_IDS - (long) sourceIds.size()).forEach(sourceIds::add);
            return new LlmContent.ToolResult(use.id(), toJson(result.data()), false);
        } catch (ToolInputException e) {
            calls.add(new ToolCallRecord(use.name(), use.input(), null, true));
            return new LlmContent.ToolResult(use.id(), e.getMessage(), true);
        }
    }

    private List<LlmMessage> history(UUID conversationId) {
        List<Message> latest = new ArrayList<>(messages.findLatest(conversationId,
                PageRequest.of(0, settings.historyMessages())));
        Collections.reverse(latest);
        List<LlmMessage> history = new ArrayList<>();
        for (Message m : latest) {
            LlmMessage.Role role = m.getRole() == Message.Role.USER ? LlmMessage.Role.USER : LlmMessage.Role.ASSISTANT;
            // The API requires alternating roles starting with the user; skip messages that would break this.
            boolean expectedRole = history.isEmpty()
                    ? role == LlmMessage.Role.USER
                    : history.get(history.size() - 1).role() != role;
            if (expectedRole) {
                history.add(new LlmMessage(role, List.of(new LlmContent.Text(m.getContent()))));
            }
        }
        if (!history.isEmpty() && history.get(history.size() - 1).role() == LlmMessage.Role.USER) {
            history.remove(history.size() - 1);
        }
        return history;
    }

    String systemPrompt(ToolContext context) {
        String categories = categoryService.listVisible(context.userId()).stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));
        return """
                You are the PocketLens assistant. You answer questions about the user's own transactions, spending, \
                budgets and subscriptions.
                Rules:
                - Today is %s. Resolve relative periods ("last month", "this year") into exact dates.
                - Always use the tools for figures. Never estimate, invent or add up numbers yourself; if you need a \
                total, call a tool that returns it.
                - Amounts in different currencies must never be added together; report them separately.
                - If the tools return no data, say so.
                - You cannot give investment, tax, loan or legal advice and you cannot move money. If asked, explain \
                briefly what you can do instead.
                - Answer in the language of the question, briefly (at most 5 sentences), with amounts and currency.
                - The user's categories are: %s.""".formatted(context.today(), categories);
    }

    private String toJson(Object data) {
        return jsonMapper.writeValueAsString(data);
    }

    private record LoopResult(String answer, List<String> interpretations, List<ToolCallRecord> calls,
                              List<UUID> sourceIds) {
    }
}
