package com.pocketlens.assistant.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketlens.ai.llm.AiUnavailableException;
import com.pocketlens.ai.llm.LlmClient;
import com.pocketlens.ai.llm.LlmContent;
import com.pocketlens.ai.llm.LlmMessage;
import com.pocketlens.ai.llm.LlmRequest;
import com.pocketlens.ai.llm.LlmResponse;
import com.pocketlens.ai.service.AiUsageService;
import com.pocketlens.assistant.domain.Conversation;
import com.pocketlens.assistant.domain.Message;
import com.pocketlens.assistant.persistence.ConversationRepository;
import com.pocketlens.assistant.persistence.MessageRepository;
import com.pocketlens.assistant.service.tool.AssistantTool;
import com.pocketlens.assistant.service.tool.ToolArguments;
import com.pocketlens.assistant.service.tool.ToolContext;
import com.pocketlens.assistant.service.tool.ToolInputException;
import com.pocketlens.assistant.service.tool.ToolRegistry;
import com.pocketlens.assistant.service.tool.ToolResult;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.support.TestData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AssistantServiceTest {

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID TX_ID = UUID.randomUUID();

    private final LlmClient llm = mock(LlmClient.class);
    private final ConversationService conversationService = mock(ConversationService.class);
    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final MessageRepository messages = mock(MessageRepository.class);
    private final CategoryService categories = mock(CategoryService.class);
    private final AiUsageService usage = mock(AiUsageService.class);
    private final List<Message> saved = new ArrayList<>();
    private AssistantService service;

    @BeforeEach
    void setUp() {
        AssistantTool total = new AssistantTool() {
            @Override
            public String name() {
                return "get_spending_total";
            }

            @Override
            public String description() {
                return "total";
            }

            @Override
            public Map<String, Object> inputSchema() {
                return Map.of("type", "object");
            }

            @Override
            public ToolResult execute(ToolContext context, ToolArguments arguments) throws ToolInputException {
                arguments.period();
                return new ToolResult(Map.of("spent", 42), "Spending " + arguments.raw().get("from"), List.of(TX_ID));
            }
        };
        service = new AssistantService(llm, new ToolRegistry(List.of(total)), new PromptGuard(TestData.properties()),
                new SlidingWindowRateLimiter(TestData.properties(), TestData.clock()), conversationService,
                conversations, messages, categories, usage, new ObjectMapper(), TestData.properties(),
                TestData.clock());
        when(conversationService.get(USER_ID, CONVERSATION_ID))
                .thenReturn(new Conversation(USER_ID, TestData.NOW));
        when(messages.findLatest(any(), any())).thenReturn(List.of());
        when(messages.save(any())).thenAnswer(i -> {
            Message m = TestData.withId(i.getArgument(0), UUID.randomUUID());
            saved.add(m);
            return m;
        });
        when(categories.listVisible(USER_ID)).thenReturn(List.of(TestData.expenseCategory("Groceries")));
    }

    @Test
    void runsToolsAndReturnsAnswerWithInterpretationAndSources() {
        when(llm.send(any())).thenReturn(
                new LlmResponse(List.of(new LlmContent.Text("Let me check."),
                        new LlmContent.ToolUse("tu1", "get_spending_total",
                                Map.of("from", "2026-10-01", "to", "2026-10-31"))), "tool_use", 100, 20),
                new LlmResponse(List.of(new LlmContent.Text("You spent 42.00 EUR.")), "end_turn", 150, 10));

        AssistantAnswer answer = service.ask(USER_ID, CONVERSATION_ID, "How much did I spend in October?");

        assertThat(answer.answer().getContent()).isEqualTo("You spent 42.00 EUR.");
        assertThat(answer.answer().getInterpretation()).containsExactly("Spending 2026-10-01");
        assertThat(answer.answer().getSourceTransactionIds()).containsExactly(TX_ID);
        assertThat(answer.answer().getToolCalls()).singleElement()
                .satisfies(c -> assertThat(c.failed()).isFalse());
        assertThat(saved).extracting(Message::getRole).containsExactly(Message.Role.USER, Message.Role.ASSISTANT);

        ArgumentCaptor<LlmRequest> requests = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm, times(2)).send(requests.capture());
        LlmRequest second = requests.getAllValues().get(1);
        assertThat(second.messages()).hasSize(3);
        LlmContent.ToolResult result = (LlmContent.ToolResult) second.messages().get(2).content().get(0);
        assertThat(result.content()).contains("42");
        assertThat(second.system()).contains("2026-11-15").contains("Groceries");
        verify(usage, times(2)).record(any(), any(), any());
    }

    @Test
    void reportsInvalidToolInputAndUnknownToolsBackToTheModel() {
        when(llm.send(any())).thenReturn(
                new LlmResponse(List.of(
                        new LlmContent.ToolUse("tu1", "get_spending_total", Map.of("from", "October")),
                        new LlmContent.ToolUse("tu2", "delete_everything", Map.of())), "tool_use", 1, 1),
                new LlmResponse(List.of(new LlmContent.Text("Sorry.")), "end_turn", 1, 1));

        AssistantAnswer answer = service.ask(USER_ID, CONVERSATION_ID, "Spending?");

        assertThat(answer.answer().getToolCalls()).hasSize(2).allMatch(c -> c.failed());
        ArgumentCaptor<LlmRequest> requests = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm, times(2)).send(requests.capture());
        List<LlmContent> results = requests.getAllValues().get(1).messages().get(2).content();
        assertThat(results).allMatch(r -> ((LlmContent.ToolResult) r).isError());
    }

    @Test
    void stopsAfterTheIterationLimit() {
        when(llm.send(any())).thenReturn(new LlmResponse(List.of(new LlmContent.ToolUse("tu", "get_spending_total",
                Map.of("from", "2026-10-01", "to", "2026-10-31"))), "tool_use", 1, 1));

        AssistantAnswer answer = service.ask(USER_ID, CONVERSATION_ID, "Loop?");

        assertThat(answer.answer().getContent()).isEqualTo(AssistantService.FALLBACK_ANSWER);
        verify(llm, times(4)).send(any());
    }

    @Test
    void blankModelAnswerIsReplacedByFallback() {
        when(llm.send(any())).thenReturn(new LlmResponse(List.of(), "end_turn", 1, 1));
        assertThat(service.ask(USER_ID, CONVERSATION_ID, "Hi").answer().getContent())
                .isEqualTo(AssistantService.FALLBACK_ANSWER);
    }

    @Test
    void keepsTheQuestionWhenTheProviderIsUnavailable() {
        when(llm.send(any())).thenThrow(new AiUnavailableException("down"));

        assertThatThrownBy(() -> service.ask(USER_ID, CONVERSATION_ID, "Hi")).isInstanceOf(AiUnavailableException.class);
        assertThat(saved).extracting(Message::getRole).containsExactly(Message.Role.USER);
    }

    @Test
    void historyStartsWithUserAndAlternates() {
        Message q1 = Message.user(CONVERSATION_ID, "q1", TestData.NOW);
        Message a1 = Message.assistant(CONVERSATION_ID, "a1", List.of(), List.of(), List.of(), TestData.NOW);
        Message orphan = Message.user(CONVERSATION_ID, "unanswered", TestData.NOW);
        // repository returns newest first
        when(messages.findLatest(any(), any())).thenReturn(List.of(orphan, a1, q1, a1));
        when(llm.send(any())).thenReturn(new LlmResponse(List.of(new LlmContent.Text("ok")), "end_turn", 1, 1));

        service.ask(USER_ID, CONVERSATION_ID, "q2");

        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm).send(request.capture());
        assertThat(request.getValue().messages()).extracting(LlmMessage::role)
                .containsExactly(LlmMessage.Role.USER, LlmMessage.Role.ASSISTANT, LlmMessage.Role.USER);
    }
}
