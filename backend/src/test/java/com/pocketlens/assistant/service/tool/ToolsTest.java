package com.pocketlens.assistant.service.tool;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pocketlens.budget.service.BudgetHealth;
import com.pocketlens.budget.service.BudgetService;
import com.pocketlens.budget.service.BudgetStatus;
import com.pocketlens.categorization.service.PiiSanitizer;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.subscription.service.Cadence;
import com.pocketlens.subscription.service.Subscription;
import com.pocketlens.subscription.service.SubscriptionService;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.CategoryTotal;
import com.pocketlens.transaction.persistence.CurrencyTotal;
import com.pocketlens.transaction.persistence.MonthlyTotal;
import com.pocketlens.transaction.persistence.TransactionAggregates;
import com.pocketlens.transaction.persistence.TransactionCriteria;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class ToolsTest {

    private static final ToolContext CONTEXT = new ToolContext(USER_ID, TestData.TODAY);
    private static final Map<String, Object> OCTOBER = Map.of("from", "2026-10-01", "to", "2026-10-31");

    private final TransactionAggregates aggregates = mock(TransactionAggregates.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final CategoryService categories = mock(CategoryService.class);
    private final CategoryResolver resolver = new CategoryResolver(categories);
    private final Category groceries = TestData.expenseCategory("Groceries");

    @BeforeEach
    void setUp() {
        when(categories.listVisible(USER_ID)).thenReturn(List.of(groceries, TestData.expenseCategory("Transport")));
    }

    @Test
    void spendingTotalFiltersByCategoryAndMerchantAndReturnsSources() throws ToolInputException {
        UUID tx = UUID.randomUUID();
        when(aggregates.sumByCurrency(any())).thenReturn(List.of(new CurrencyTotal("EUR", new BigDecimal("-42.00"), 3)));
        when(aggregates.ids(any(), anyInt())).thenReturn(List.of(tx));
        Map<String, Object> args = new java.util.HashMap<>(OCTOBER);
        args.put("category", "groceries");
        args.put("merchant", "lidl");

        ToolResult result = new SpendingTotalTool(aggregates, resolver).execute(CONTEXT, new ToolArguments(args));

        ArgumentCaptor<TransactionCriteria> criteria = ArgumentCaptor.forClass(TransactionCriteria.class);
        org.mockito.Mockito.verify(aggregates).sumByCurrency(criteria.capture());
        assertThat(criteria.getValue().categoryId()).isEqualTo(groceries.getId());
        assertThat(criteria.getValue().merchant()).isEqualTo("lidl");
        assertThat(criteria.getValue().expensesOnly()).isTrue();
        assertThat(criteria.getValue().userId()).isEqualTo(USER_ID);
        assertThat(result.transactionIds()).containsExactly(tx);
        assertThat(result.interpretation()).contains("Groceries").contains("LIDL").contains("2026-10-01");
        assertThat(result.data().toString()).contains("spent=42.00");
    }

    @Test
    void spendingTotalSupportsUncategorisedAndRejectsUnknownCategory() throws ToolInputException {
        when(aggregates.sumByCurrency(any())).thenReturn(List.of());
        Map<String, Object> args = new java.util.HashMap<>(OCTOBER);
        args.put("category", "Uncategorised");
        SpendingTotalTool tool = new SpendingTotalTool(aggregates, resolver);
        tool.execute(CONTEXT, new ToolArguments(args));
        ArgumentCaptor<TransactionCriteria> criteria = ArgumentCaptor.forClass(TransactionCriteria.class);
        org.mockito.Mockito.verify(aggregates).sumByCurrency(criteria.capture());
        assertThat(criteria.getValue().uncategorized()).isTrue();

        args.put("category", "Yachts");
        assertThatThrownBy(() -> tool.execute(CONTEXT, new ToolArguments(args)))
                .isInstanceOf(ToolInputException.class).hasMessageContaining("Groceries");
        assertThat(tool.name()).isEqualTo("get_spending_total");
        assertThat(tool.inputSchema()).containsKey("properties");
        assertThat(tool.description()).isNotBlank();
    }

    @Test
    void spendingByCategoryReturnsSortedCategories() throws ToolInputException {
        when(aggregates.spendingByCategory(any())).thenReturn(List.of(
                new CategoryTotal(groceries.getId(), "Groceries", "#1", "EUR", new BigDecimal("120"), 8),
                new CategoryTotal(null, null, null, "EUR", new BigDecimal("10"), 1)));
        when(aggregates.ids(any(), anyInt())).thenReturn(List.of(UUID.randomUUID()));
        SpendingByCategoryTool tool = new SpendingByCategoryTool(aggregates);

        ToolResult result = tool.execute(CONTEXT, new ToolArguments(OCTOBER));

        assertThat(result.data().toString()).contains("Groceries").contains("Uncategorised");
        assertThat(result.transactionIds()).hasSize(1);
        assertThat(tool.name()).isEqualTo("get_spending_by_category");
        assertThat(tool.description()).isNotBlank();
        assertThat(tool.inputSchema()).containsEntry("type", "object");
    }

    @Test
    void listTransactionsReturnsSanitisedRows() throws ToolInputException {
        Transaction t = TestData.transaction("2026-10-05", "-99.00", "TRANSFER LT12 1000 0111 0100 1000");
        t.categorize(groceries, com.pocketlens.categorization.domain.CategorizationSource.RULE, BigDecimal.ONE, false);
        Transaction u = TestData.transaction("2026-10-06", "-5.00", "KIOSK");
        when(transactions.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t, u)));
        ListTransactionsTool tool = new ListTransactionsTool(transactions, resolver, new PiiSanitizer());
        Map<String, Object> args = new java.util.HashMap<>(OCTOBER);
        args.put("order", "latest");
        args.put("limit", 2);
        args.put("category", "Groceries");
        args.put("merchant", "x");

        ToolResult result = tool.execute(CONTEXT, new ToolArguments(args));

        assertThat(result.data().toString()).doesNotContain("LT12").contains("[IBAN]").contains("Uncategorised");
        assertThat(result.transactionIds()).containsExactly(t.getId(), u.getId());
        assertThat(result.interpretation()).startsWith("Latest 2 expenses").contains("Groceries");

        args.put("order", "largest");
        args.put("category", "Uncategorised");
        assertThat(tool.execute(CONTEXT, new ToolArguments(args)).interpretation()).startsWith("Largest");
        assertThat(tool.name()).isEqualTo("list_transactions");
        assertThat(tool.description()).isNotBlank();
        assertThat(tool.inputSchema()).containsKey("properties");
    }

    @Test
    void monthlyTotalsAndBudgetsAndSubscriptions() throws ToolInputException {
        when(aggregates.monthlyTotals(eq(USER_ID), any(), any())).thenReturn(List.of(
                new MonthlyTotal(2026, 10, "EUR", new BigDecimal("500"), new BigDecimal("2000"))));
        MonthlyTotalsTool monthly = new MonthlyTotalsTool(aggregates);
        assertThat(monthly.execute(CONTEXT, new ToolArguments(OCTOBER)).data().toString()).contains("2026-10");
        assertThat(monthly.name()).isEqualTo("get_monthly_totals");
        assertThat(monthly.description()).isNotBlank();
        assertThat(monthly.inputSchema()).isNotEmpty();

        BudgetService budgetService = mock(BudgetService.class);
        when(budgetService.statuses(USER_ID, YearMonth.of(2026, 11))).thenReturn(List.of(new BudgetStatus(
                UUID.randomUUID(), groceries.getId(), "Groceries", "#1", "EUR", new BigDecimal("300"),
                new BigDecimal("250"), new BigDecimal("50"), new BigDecimal("500"), 83, BudgetHealth.AT_RISK,
                LocalDate.of(2026, 11, 18))));
        BudgetStatusTool budgets = new BudgetStatusTool(budgetService);
        ToolResult budgetResult = budgets.execute(CONTEXT, new ToolArguments(Map.of()));
        assertThat(budgetResult.data().toString()).contains("AT_RISK").contains("2026-11-18");
        assertThat(budgets.name()).isEqualTo("get_budget_status");
        assertThat(budgets.description()).isNotBlank();
        assertThat(budgets.inputSchema()).isNotEmpty();

        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        UUID tx = UUID.randomUUID();
        when(subscriptionService.detect(USER_ID)).thenReturn(List.of(new Subscription("SPOTIFY", "EUR",
                Cadence.MONTHLY, new BigDecimal("11.99"), new BigDecimal("10.99"), true, new BigDecimal("11.99"),
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 12, 3), 4, List.of(tx))));
        SubscriptionsTool subscriptions = new SubscriptionsTool(subscriptionService, new PiiSanitizer());
        ToolResult subs = subscriptions.execute(CONTEXT, new ToolArguments(Map.of()));
        assertThat(subs.transactionIds()).containsExactly(tx);
        assertThat(subs.data().toString()).contains("SPOTIFY").contains("priceIncreased=true");
        assertThat(subscriptions.name()).isEqualTo("get_subscriptions");
        assertThat(subscriptions.description()).isNotBlank();
        assertThat(subscriptions.inputSchema()).isNotEmpty();
    }

    @Test
    void registryExposesToolDefinitions() {
        ToolRegistry registry = new ToolRegistry(List.of(new MonthlyTotalsTool(aggregates),
                new SpendingByCategoryTool(aggregates)));
        assertThat(registry.definitions()).extracting(d -> d.name())
                .containsExactly("get_monthly_totals", "get_spending_by_category");
        assertThat(registry.find("get_monthly_totals")).isPresent();
        assertThat(registry.find("nope")).isEmpty();
    }

    @Test
    void resolverReturnsEmptyForNoFilter() throws ToolInputException {
        assertThat(resolver.resolve(USER_ID, null)).isEmpty();
        assertThat(resolver.resolve(USER_ID, "uncategorized")).get()
                .extracting(CategoryResolver.Resolved::label).isEqualTo("Uncategorised");
        assertThat(resolver.resolve(USER_ID, " TRANSPORT ")).get()
                .extracting(CategoryResolver.Resolved::label).isEqualTo("Transport");
    }
}
