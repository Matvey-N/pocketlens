package com.pocketlens.categorization.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.ai.domain.AiUsageRecord;
import com.pocketlens.ai.llm.LlmClient;
import com.pocketlens.ai.llm.LlmContent;
import com.pocketlens.ai.llm.LlmRequest;
import com.pocketlens.ai.llm.LlmResponse;
import com.pocketlens.ai.service.AiUsageService;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiCategorizerTest {

    private final LlmClient llm = mock(LlmClient.class);
    private final AiUsageService usage = mock(AiUsageService.class);
    private final AiCategorizer categorizer = new AiCategorizer(llm, new PiiSanitizer(), usage, TestData.properties());
    private final Category eatingOut = TestData.expenseCategory("Eating out");
    private final Category income = TestData.category("Income", CategoryKind.INCOME);
    private final List<Category> categories = List.of(eatingOut, income);

    @Test
    void mapsToolOutputToSuggestions() {
        Transaction cafe = TestData.transaction("2026-11-02", "-4.50", "CAFE BLOOM");
        Transaction odd = TestData.transaction("2026-11-02", "-9.00", "ODD SHOP");
        when(llm.send(any())).thenReturn(toolResponse(List.of(
                Map.of("index", 0, "category", "eating out", "confidence", 0.93),
                Map.of("index", 1, "category", "Income", "confidence", 0.9),
                Map.of("index", 7, "category", "Eating out", "confidence", 0.9),
                Map.of("index", 0, "category", "Unknown", "confidence", 0.9))));

        Map<UUID, AiSuggestion> result = categorizer.suggest(USER_ID, List.of(cafe, odd), categories);

        assertThat(result).containsOnlyKeys(cafe.getId());
        assertThat(result.get(cafe.getId()).category()).isEqualTo(eatingOut);
        assertThat(result.get(cafe.getId()).confidence()).isEqualByComparingTo(new BigDecimal("0.93"));
        verify(usage).record(eq(USER_ID), eq(AiUsageRecord.Purpose.CATEGORIZATION), any());
    }

    @Test
    void requestContainsNoPersonalIdentifiers() {
        Transaction transfer = TestData.transaction("2026-11-02", "-50.00", "PAYMENT LT12 1000 0111 0100 1000");
        when(llm.send(any())).thenReturn(toolResponse(List.of()));

        categorizer.suggest(USER_ID, List.of(transfer), categories);

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm).send(captor.capture());
        LlmRequest request = captor.getValue();
        String prompt = ((LlmContent.Text) request.messages().get(0).content().get(0)).text();
        assertThat(prompt).doesNotContain("LT12").contains("[IBAN]").contains("outgoing payment");
        assertThat(request.forcedTool()).isEqualTo(AiCategorizer.TOOL_NAME);
        assertThat(new PiiSanitizer().containsPii(prompt)).isFalse();
    }

    @Test
    void sendsLargeInputsInBatches() {
        List<Transaction> many = new ArrayList<>();
        for (int i = 0; i < 85; i++) {
            many.add(TestData.transaction("2026-11-02", "-1.00", "SHOP " + i));
        }
        when(llm.send(any())).thenReturn(toolResponse(List.of()));

        categorizer.suggest(USER_ID, many, categories);

        verify(llm, times(3)).send(any());
    }

    @Test
    void ignoresMalformedToolOutput() {
        Transaction cafe = TestData.transaction("2026-11-02", "-4.50", "CAFE BLOOM");
        when(llm.send(any())).thenReturn(new LlmResponse(List.of(new LlmContent.ToolUse("t1",
                AiCategorizer.TOOL_NAME, Map.of("assignments", "nonsense"))), "tool_use", 10, 5));
        assertThat(categorizer.suggest(USER_ID, List.of(cafe), categories)).isEmpty();

        when(llm.send(any())).thenReturn(new LlmResponse(List.of(new LlmContent.ToolUse("t1",
                AiCategorizer.TOOL_NAME, Map.of("assignments", List.of("x", Map.of("index", "0"))))), "tool_use", 1, 1));
        assertThat(categorizer.suggest(USER_ID, List.of(cafe), categories)).isEmpty();
    }

    @Test
    void reportsAvailabilityOfTheProvider() {
        when(llm.isAvailable()).thenReturn(true);
        assertThat(categorizer.isAvailable()).isTrue();
    }

    private static LlmResponse toolResponse(List<Map<String, Object>> assignments) {
        return new LlmResponse(List.of(new LlmContent.ToolUse("t1", AiCategorizer.TOOL_NAME,
                Map.of("assignments", assignments))), "tool_use", 100, 20);
    }
}
