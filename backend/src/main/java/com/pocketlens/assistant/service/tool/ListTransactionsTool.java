package com.pocketlens.assistant.service.tool;

import com.pocketlens.categorization.service.PiiSanitizer;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionCriteria;
import com.pocketlens.transaction.persistence.TransactionRepository;
import com.pocketlens.transaction.persistence.TransactionSpecifications;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Individual expenses: the largest or the latest in a period. Only the normalised, sanitised merchant name is
 * returned, never the raw bank description.
 */
@Component
public class ListTransactionsTool implements AssistantTool {

    private static final Set<String> ORDERS = Set.of("largest", "latest");
    private static final int MAX_LIMIT = 20;

    private final TransactionRepository transactions;
    private final CategoryResolver categoryResolver;
    private final PiiSanitizer sanitizer;

    public ListTransactionsTool(TransactionRepository transactions, CategoryResolver categoryResolver,
                                PiiSanitizer sanitizer) {
        this.transactions = transactions;
        this.categoryResolver = categoryResolver;
        this.sanitizer = sanitizer;
    }

    @Override
    public String name() {
        return "list_transactions";
    }

    @Override
    public String description() {
        return "Lists individual expenses in a period: the largest (order=largest) or the most recent "
                + "(order=latest), optionally filtered by category or merchant. At most 20 rows.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schemas.period(Map.of(
                "category", Schemas.CATEGORY,
                "merchant", Schemas.MERCHANT,
                "order", Map.of("type", "string", "enum", List.of("largest", "latest")),
                "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_LIMIT)));
    }

    @Override
    public ToolResult execute(ToolContext context, ToolArguments arguments) throws ToolInputException {
        ToolArguments.Period period = arguments.period();
        String order = arguments.choice("order", ORDERS, "largest");
        int limit = arguments.integer("limit", 10, 1, MAX_LIMIT);
        String merchant = arguments.optionalText("merchant");
        Optional<CategoryResolver.Resolved> category =
                categoryResolver.resolve(context.userId(), arguments.optionalText("category"));
        TransactionCriteria.Builder builder = TransactionCriteria.forUser(context.userId())
                .period(period.from(), period.to()).merchant(merchant).expensesOnly();
        category.ifPresent(c -> {
            if (c.category() == null) {
                builder.uncategorized(true);
            } else {
                builder.category(c.category().getId());
            }
        });
        Sort sort = "largest".equals(order)
                ? Sort.by(Sort.Direction.ASC, "amount")
                : Sort.by(Sort.Direction.DESC, "bookingDate");
        List<Transaction> rows = transactions.findAll(TransactionSpecifications.matching(builder.build()),
                PageRequest.of(0, limit, sort)).getContent();
        List<Map<String, Object>> data = rows.stream().map(this::toMap).toList();
        String interpretation = ("largest".equals(order) ? "Largest" : "Latest") + " " + limit + " expenses "
                + period + category.map(c -> ", category: " + c.label()).orElse("")
                + (merchant == null ? "" : ", merchant contains: " + merchant);
        return new ToolResult(Map.of("transactions", data), interpretation,
                rows.stream().map(Transaction::getId).toList());
    }

    private Map<String, Object> toMap(Transaction t) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", t.getBookingDate().toString());
        row.put("merchant", sanitizer.sanitize(t.getMerchant()));
        row.put("amount", t.getAmount().negate());
        row.put("currency", t.getCurrency());
        row.put("category", t.getCategory() == null ? "Uncategorised" : t.getCategory().getName());
        return row;
    }
}
