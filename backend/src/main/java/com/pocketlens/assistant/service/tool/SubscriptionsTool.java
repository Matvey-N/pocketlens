package com.pocketlens.assistant.service.tool;

import com.pocketlens.categorization.service.PiiSanitizer;
import com.pocketlens.subscription.service.Subscription;
import com.pocketlens.subscription.service.SubscriptionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Detected subscriptions and recurring payments ("How much do my subscriptions cost?").
 */
@Component
public class SubscriptionsTool implements AssistantTool {

    private final SubscriptionService subscriptionService;
    private final PiiSanitizer sanitizer;

    public SubscriptionsTool(SubscriptionService subscriptionService, PiiSanitizer sanitizer) {
        this.subscriptionService = subscriptionService;
        this.sanitizer = sanitizer;
    }

    @Override
    public String name() {
        return "get_subscriptions";
    }

    @Override
    public String description() {
        return "Active recurring payments (subscriptions) with cadence, last amount, monthly cost, next expected "
                + "date and whether the price increased.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schemas.object(Map.of(), List.of());
    }

    @Override
    public ToolResult execute(ToolContext context, ToolArguments arguments) {
        List<Subscription> subscriptions = subscriptionService.detect(context.userId());
        List<Map<String, Object>> data = subscriptions.stream().map(this::toMap).toList();
        List<UUID> ids = subscriptions.stream().flatMap(s -> s.transactionIds().stream()).toList();
        return new ToolResult(Map.of("subscriptions", data), "Detected subscriptions (last 13 months)", ids);
    }

    private Map<String, Object> toMap(Subscription s) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("merchant", sanitizer.sanitize(s.merchant()));
        row.put("currency", s.currency());
        row.put("cadence", s.cadence().name());
        row.put("lastAmount", s.lastAmount());
        row.put("monthlyCost", s.monthlyCost());
        row.put("priceIncreased", s.priceIncreased());
        row.put("nextExpectedDate", s.nextExpectedDate().toString());
        return row;
    }
}
