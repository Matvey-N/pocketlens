package com.pocketlens.assistant.service.tool;

import com.pocketlens.transaction.persistence.TransactionAggregates;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Expenses and income per month (trends, "Did I spend more than last month?").
 */
@Component
public class MonthlyTotalsTool implements AssistantTool {

    private final TransactionAggregates aggregates;

    public MonthlyTotalsTool(TransactionAggregates aggregates) {
        this.aggregates = aggregates;
    }

    @Override
    public String name() {
        return "get_monthly_totals";
    }

    @Override
    public String description() {
        return "Total expenses and income per calendar month and currency in a period (transfers excluded).";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schemas.period(Map.of());
    }

    @Override
    public ToolResult execute(ToolContext context, ToolArguments arguments) throws ToolInputException {
        ToolArguments.Period period = arguments.period();
        List<Map<String, Object>> months = aggregates.monthlyTotals(context.userId(), period.from(), period.to())
                .stream()
                .map(m -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month", YearMonth.of(m.year(), m.month()).toString());
                    row.put("currency", m.currency());
                    row.put("expenses", m.expenses());
                    row.put("income", m.income());
                    return row;
                })
                .toList();
        return new ToolResult(Map.of("months", months), "Monthly expenses and income " + period, List.of());
    }
}
