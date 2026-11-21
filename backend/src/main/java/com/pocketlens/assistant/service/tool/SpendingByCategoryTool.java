package com.pocketlens.assistant.service.tool;

import com.pocketlens.transaction.persistence.TransactionAggregates;
import com.pocketlens.transaction.persistence.TransactionCriteria;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Spending per category in a period (e.g. "Where did my money go in October?").
 */
@Component
public class SpendingByCategoryTool implements AssistantTool {

    private final TransactionAggregates aggregates;

    public SpendingByCategoryTool(TransactionAggregates aggregates) {
        this.aggregates = aggregates;
    }

    @Override
    public String name() {
        return "get_spending_by_category";
    }

    @Override
    public String description() {
        return "Spending per category in a period, sorted from the largest. One entry per category and currency.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schemas.period(Map.of());
    }

    @Override
    public ToolResult execute(ToolContext context, ToolArguments arguments) throws ToolInputException {
        ToolArguments.Period period = arguments.period();
        TransactionCriteria criteria = TransactionCriteria.forUser(context.userId())
                .period(period.from(), period.to())
                .build();
        List<Map<String, Object>> categories = aggregates.spendingByCategory(criteria).stream()
                .map(t -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("category", t.categoryId() == null ? "Uncategorised" : t.categoryName());
                    row.put("currency", t.currency());
                    row.put("spent", t.total());
                    row.put("transactions", t.count());
                    return row;
                })
                .toList();
        TransactionCriteria expenses = TransactionCriteria.forUser(context.userId())
                .period(period.from(), period.to()).expensesOnly().build();
        return new ToolResult(Map.of("period", period.toString(), "categories", categories),
                "Spending by category " + period, aggregates.ids(expenses, SpendingTotalTool.MAX_IDS));
    }
}
