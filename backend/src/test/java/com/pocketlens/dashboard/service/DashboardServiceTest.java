package com.pocketlens.dashboard.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pocketlens.budget.service.BudgetService;
import com.pocketlens.transaction.persistence.CategoryTotal;
import com.pocketlens.transaction.persistence.MonthlyTotal;
import com.pocketlens.transaction.persistence.TransactionAggregates;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private final TransactionAggregates aggregates = mock(TransactionAggregates.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final BudgetService budgets = mock(BudgetService.class);
    private final DashboardService service = new DashboardService(aggregates, transactions, budgets);

    @Test
    void groupsFiguresPerCurrencyAndFillsTheTrend() {
        YearMonth november = YearMonth.of(2026, 11);
        UUID groceries = UUID.randomUUID();
        when(aggregates.spendingByCategory(any())).thenReturn(List.of(
                new CategoryTotal(groceries, "Groceries", "#4CAF50", "EUR", new BigDecimal("120.00"), 8),
                new CategoryTotal(null, null, null, "EUR", new BigDecimal("30.00"), 2),
                new CategoryTotal(groceries, "Groceries", "#4CAF50", "USD", new BigDecimal("15.00"), 1)));
        when(aggregates.monthlyTotals(eq(USER_ID), any(), any())).thenReturn(List.of(
                new MonthlyTotal(2026, 9, "EUR", new BigDecimal("400"), new BigDecimal("2000")),
                new MonthlyTotal(2026, 11, "EUR", new BigDecimal("150"), new BigDecimal("2000")),
                new MonthlyTotal(2026, 11, "USD", new BigDecimal("15"), BigDecimal.ZERO)));
        when(budgets.statuses(USER_ID, november)).thenReturn(List.of());
        when(transactions.countByUserIdAndNeedsReviewTrue(USER_ID)).thenReturn(3L);

        DashboardView view = service.build(USER_ID, november);

        assertThat(view.month()).isEqualTo("2026-11");
        assertThat(view.needsReviewCount()).isEqualTo(3);
        assertThat(view.currencies()).extracting(DashboardView.CurrencySummary::currency).containsExactly("EUR", "USD");
        DashboardView.CurrencySummary eur = view.currencies().get(0);
        assertThat(eur.expenses()).isEqualByComparingTo("150");
        assertThat(eur.net()).isEqualByComparingTo("1850");
        assertThat(eur.byCategory()).extracting(DashboardView.CategorySlice::name)
                .containsExactly("Groceries", DashboardService.UNCATEGORIZED);
        assertThat(eur.trend()).hasSize(6);
        assertThat(eur.trend().get(3).expenses()).isEqualByComparingTo("400");
        assertThat(eur.trend().get(4).expenses()).isEqualByComparingTo("0");
    }

    @Test
    void emptyMonthHasNoCurrencyBlocks() {
        when(aggregates.spendingByCategory(any())).thenReturn(List.of());
        when(aggregates.monthlyTotals(eq(USER_ID), any(), any())).thenReturn(List.of(
                new MonthlyTotal(2026, 10, "EUR", BigDecimal.ONE, BigDecimal.ZERO)));
        assertThat(service.build(USER_ID, YearMonth.of(2026, 11)).currencies()).isEmpty();
    }
}
