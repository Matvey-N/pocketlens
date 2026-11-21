package com.pocketlens.dashboard.service;

import com.pocketlens.budget.service.BudgetService;
import com.pocketlens.budget.service.BudgetStatus;
import com.pocketlens.common.config.CacheConfig;
import com.pocketlens.transaction.persistence.CategoryTotal;
import com.pocketlens.transaction.persistence.MonthlyTotal;
import com.pocketlens.transaction.persistence.TransactionAggregates;
import com.pocketlens.transaction.persistence.TransactionCriteria;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the dashboard: spending by category, six-month trend and budget states, per currency (US-3.4).
 * The result is cached per user and month and evicted on every data change.
 */
@Service
public class DashboardService {

    static final String UNCATEGORIZED = "Uncategorised";
    static final String UNCATEGORIZED_COLOR = "#9E9E9E";
    private static final int TREND_MONTHS = 6;

    private final TransactionAggregates aggregates;
    private final TransactionRepository transactions;
    private final BudgetService budgetService;

    public DashboardService(TransactionAggregates aggregates, TransactionRepository transactions,
                            BudgetService budgetService) {
        this.aggregates = aggregates;
        this.transactions = transactions;
        this.budgetService = budgetService;
    }

    @Transactional
    @Cacheable(cacheNames = CacheConfig.DASHBOARD_CACHE, key = "#userId.toString() + ':' + #month.toString()")
    public DashboardView build(UUID userId, YearMonth month) {
        YearMonth firstTrendMonth = month.minusMonths(TREND_MONTHS - 1L);
        List<CategoryTotal> byCategory = aggregates.spendingByCategory(TransactionCriteria.forUser(userId)
                .period(month.atDay(1), month.atEndOfMonth()).build());
        List<MonthlyTotal> monthly = aggregates.monthlyTotals(userId, firstTrendMonth.atDay(1), month.atEndOfMonth());

        TreeSet<String> currencies = new TreeSet<>();
        byCategory.forEach(t -> currencies.add(t.currency()));
        monthly.stream().filter(m -> YearMonth.of(m.year(), m.month()).equals(month))
                .forEach(m -> currencies.add(m.currency()));

        List<DashboardView.CurrencySummary> summaries = new ArrayList<>();
        for (String currency : currencies) {
            summaries.add(summary(currency, month, firstTrendMonth, byCategory, monthly));
        }
        List<BudgetStatus> budgets = budgetService.statuses(userId, month);
        return new DashboardView(month.toString(), summaries, budgets,
                transactions.countByUserIdAndNeedsReviewTrue(userId));
    }

    private static DashboardView.CurrencySummary summary(String currency, YearMonth month, YearMonth firstTrendMonth,
                                                         List<CategoryTotal> byCategory,
                                                         List<MonthlyTotal> monthly) {
        List<DashboardView.CategorySlice> slices = byCategory.stream()
                .filter(t -> currency.equals(t.currency()))
                .map(t -> new DashboardView.CategorySlice(t.categoryId(),
                        t.categoryId() == null ? UNCATEGORIZED : t.categoryName(),
                        t.categoryId() == null ? UNCATEGORIZED_COLOR : t.color(), t.total(), t.count()))
                .toList();
        Map<YearMonth, MonthlyTotal> byMonth = monthly.stream()
                .filter(m -> currency.equals(m.currency()))
                .collect(Collectors.toMap(m -> YearMonth.of(m.year(), m.month()), m -> m, (a, b) -> a, TreeMap::new));
        List<DashboardView.MonthPoint> trend = new ArrayList<>();
        for (YearMonth m = firstTrendMonth; !m.isAfter(month); m = m.plusMonths(1)) {
            MonthlyTotal total = byMonth.get(m);
            trend.add(new DashboardView.MonthPoint(m.toString(),
                    total == null ? BigDecimal.ZERO : total.expenses(),
                    total == null ? BigDecimal.ZERO : total.income()));
        }
        DashboardView.MonthPoint current = trend.get(trend.size() - 1);
        return new DashboardView.CurrencySummary(currency, current.expenses(), current.income(),
                current.income().subtract(current.expenses()), slices, trend);
    }
}
