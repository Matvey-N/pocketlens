package com.pocketlens.assistant.service.tool;

import com.pocketlens.transaction.persistence.CurrencyTotal;
import com.pocketlens.transaction.persistence.TransactionAggregates;
import com.pocketlens.transaction.persistence.TransactionCriteria;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Total spending in a period, optionally for one category and/or merchant.
 */
@Component
public class SpendingTotalTool implements AssistantTool {

    static final int MAX_IDS = 1000;

    private final TransactionAggregates aggregates;
    private final CategoryResolver categoryResolver;

    public SpendingTotalTool(TransactionAggregates aggregates, CategoryResolver categoryResolver) {
        this.aggregates = aggregates;
        this.categoryResolver = categoryResolver;
    }

    @Override
    public String name() {
        return "get_spending_total";
    }

    @Override
    public String description() {
        return "Total amount spent in a period, optionally filtered by category and/or merchant. "
                + "Returns one total per currency and the number of transactions.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schemas.period(Map.of("category", Schemas.CATEGORY, "merchant", Schemas.MERCHANT));
    }

    @Override
    public ToolResult execute(ToolContext context, ToolArguments arguments) throws ToolInputException {
        ToolArguments.Period period = arguments.period();
        Optional<CategoryResolver.Resolved> category =
                categoryResolver.resolve(context.userId(), arguments.optionalText("category"));
        String merchant = arguments.optionalText("merchant");
        TransactionCriteria.Builder builder = TransactionCriteria.forUser(context.userId())
                .period(period.from(), period.to())
                .merchant(merchant)
                .expensesOnly();
        category.ifPresent(c -> {
            if (c.category() == null) {
                builder.uncategorized(true);
            } else {
                builder.category(c.category().getId());
            }
        });
        TransactionCriteria criteria = builder.build();
        List<Map<String, Object>> totals = aggregates.sumByCurrency(criteria).stream()
                .map(SpendingTotalTool::toMap)
                .toList();
        StringBuilder interpretation = new StringBuilder("Spending ").append(period);
        category.ifPresent(c -> interpretation.append(", category: ").append(c.label()));
        if (merchant != null) {
            interpretation.append(", merchant contains: ").append(merchant.toUpperCase(java.util.Locale.ROOT));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("period", period.toString());
        data.put("totals", totals);
        return new ToolResult(data, interpretation.toString(), aggregates.ids(criteria, MAX_IDS));
    }

    private static Map<String, Object> toMap(CurrencyTotal total) {
        return Map.of("currency", total.currency(), "spent", total.total().negate(), "transactions", total.count());
    }
}
