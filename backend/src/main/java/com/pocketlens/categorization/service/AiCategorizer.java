package com.pocketlens.categorization.service;

import com.pocketlens.ai.domain.AiUsageRecord;
import com.pocketlens.ai.llm.LlmClient;
import com.pocketlens.ai.llm.LlmContent;
import com.pocketlens.ai.llm.LlmMessage;
import com.pocketlens.ai.llm.LlmRequest;
import com.pocketlens.ai.llm.LlmResponse;
import com.pocketlens.ai.llm.LlmTool;
import com.pocketlens.ai.service.AiUsageService;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.common.config.AppProperties;
import com.pocketlens.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Asks the LLM to categorise transactions of unknown merchants (US-2.2). Only the normalised, sanitised merchant
 * name and the direction of the payment are sent — no names, IBANs, account numbers or exact amounts.
 */
@Component
public class AiCategorizer {

    static final String TOOL_NAME = "assign_categories";
    private static final int MAX_TOKENS = 2048;

    private final LlmClient llmClient;
    private final PiiSanitizer sanitizer;
    private final AiUsageService usageService;
    private final int batchSize;

    public AiCategorizer(LlmClient llmClient, PiiSanitizer sanitizer, AiUsageService usageService,
                         AppProperties properties) {
        this.llmClient = llmClient;
        this.sanitizer = sanitizer;
        this.usageService = usageService;
        this.batchSize = properties.ai().categorizationBatchSize();
    }

    public boolean isAvailable() {
        return llmClient.isAvailable();
    }

    /**
     * Returns a suggestion per transaction id. Transactions the model did not answer for are missing in the map.
     *
     * @throws com.pocketlens.ai.llm.AiUnavailableException if the provider cannot be reached
     */
    public Map<UUID, AiSuggestion> suggest(UUID userId, List<Transaction> transactions, List<Category> categories) {
        Map<String, Category> byName = categories.stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(Locale.ROOT), Function.identity(),
                        (a, b) -> a.isDefault() ? b : a));
        Map<UUID, AiSuggestion> result = new HashMap<>();
        for (int start = 0; start < transactions.size(); start += batchSize) {
            List<Transaction> batch = transactions.subList(start, Math.min(start + batchSize, transactions.size()));
            LlmResponse response = llmClient.send(buildRequest(batch, categories));
            usageService.record(userId, AiUsageRecord.Purpose.CATEGORIZATION, response);
            response.toolUses().stream()
                    .filter(use -> TOOL_NAME.equals(use.name()))
                    .findFirst()
                    .ifPresent(use -> collect(use, batch, byName, result));
        }
        return result;
    }

    LlmRequest buildRequest(List<Transaction> batch, List<Category> categories) {
        String categoryList = categories.stream()
                .map(c -> "- " + c.getName() + " (" + c.getKind().name().toLowerCase(Locale.ROOT) + ")")
                .collect(Collectors.joining("\n"));
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            Transaction t = batch.get(i);
            items.append(i).append(". ").append(sanitizer.sanitize(t.getMerchant()))
                    .append(" | ").append(t.isExpense() ? "outgoing payment" : "incoming payment").append('\n');
        }
        String system = """
                You categorise bank transactions of a personal finance app. Choose exactly one category from the \
                list for every item, using the category names exactly as written. Give a confidence between 0 and 1; \
                use a value below 0.8 when the merchant is ambiguous or unknown. Incoming payments are usually \
                income or transfers. Use "Other" when nothing fits.""";
        String user = "Categories:\n" + categoryList + "\n\nTransactions (index. merchant | direction):\n" + items;
        return new LlmRequest(system, List.of(LlmMessage.user(user)), List.of(tool()), TOOL_NAME, MAX_TOKENS);
    }

    private static LlmTool tool() {
        Map<String, Object> assignment = Map.of(
                "type", "object",
                "properties", Map.of(
                        "index", Map.of("type", "integer"),
                        "category", Map.of("type", "string"),
                        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1)),
                "required", List.of("index", "category", "confidence"));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("assignments", Map.of("type", "array", "items", assignment)),
                "required", List.of("assignments"));
        return new LlmTool(TOOL_NAME, "Assign a category to each transaction by its index", schema);
    }

    private static void collect(LlmContent.ToolUse use, List<Transaction> batch, Map<String, Category> byName,
                                Map<UUID, AiSuggestion> result) {
        if (!(use.input().get("assignments") instanceof List<?> assignments)) {
            return;
        }
        for (Object item : assignments) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Integer index = map.get("index") instanceof Number n ? n.intValue() : null;
            String name = map.get("category") instanceof String s ? s.toLowerCase(Locale.ROOT).strip() : null;
            double confidence = map.get("confidence") instanceof Number n ? n.doubleValue() : 0.0;
            if (index == null || index < 0 || index >= batch.size() || name == null) {
                continue;
            }
            Category category = byName.get(name);
            if (category == null || !directionFits(batch.get(index), category)) {
                continue;
            }
            BigDecimal clamped = BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, confidence)))
                    .setScale(2, RoundingMode.HALF_UP);
            result.put(batch.get(index).getId(), new AiSuggestion(category, clamped));
        }
    }

    /** An outgoing payment cannot be income; anything else is accepted. */
    private static boolean directionFits(Transaction transaction, Category category) {
        return !(transaction.isExpense() && category.getKind() == CategoryKind.INCOME);
    }
}
