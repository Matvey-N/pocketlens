package com.pocketlens.assistant.service.tool;

import com.pocketlens.budget.service.BudgetService;
import com.pocketlens.budget.service.BudgetStatus;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Budgets with spending and projection ("Will I stay within my grocery budget?").
 */
@Component
public class BudgetStatusTool implements AssistantTool {

    private final BudgetService budgetService;

    public BudgetStatusTool(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Override
    public String name() {
        return "get_budget_status";
    }

    @Override
    public String description() {
        return "The user's monthly budgets with amount spent, remaining amount, projected month-end spending and "
                + "status (ON_TRACK, AT_RISK, EXCEEDED). Defaults to the current month.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schemas.object(Map.of("month", Map.of("type", "string", "description", "Month as YYYY-MM")),
                List.of());
    }

    @Override
    public ToolResult execute(ToolContext context, ToolArguments arguments) throws ToolInputException {
        YearMonth month = arguments.optionalMonth("month", YearMonth.from(context.today()));
        List<Map<String, Object>> budgets = budgetService.statuses(context.userId(), month).stream()
                .map(BudgetStatusTool::toMap)
                .toList();
        return new ToolResult(Map.of("month", month.toString(), "budgets", budgets), "Budget status " + month,
                List.of());
    }

    private static Map<String, Object> toMap(BudgetStatus s) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("category", s.categoryName());
        row.put("currency", s.currency());
        row.put("limit", s.limit());
        row.put("spent", s.spent());
        row.put("remaining", s.remaining());
        row.put("projectedSpending", s.projectedSpending());
        row.put("status", s.health().name());
        row.put("projectedOverrunDate", s.projectedOverrunDate() == null ? null : s.projectedOverrunDate().toString());
        return row;
    }
}
