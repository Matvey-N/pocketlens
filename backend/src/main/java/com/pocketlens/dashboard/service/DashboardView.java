package com.pocketlens.dashboard.service;

import com.pocketlens.budget.service.BudgetStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Read model of the dashboard (US-3.4). Totals are grouped per currency: amounts in different currencies are
 * never added up, because the product does not convert currencies.
 *
 * @param month      {@code yyyy-MM}
 * @param currencies one block per currency that has transactions in the shown period
 */
public record DashboardView(String month, List<CurrencySummary> currencies, List<BudgetStatus> budgets,
                            long needsReviewCount) implements Serializable {

    /** Figures of one currency. */
    public record CurrencySummary(String currency, BigDecimal expenses, BigDecimal income, BigDecimal net,
                                  List<CategorySlice> byCategory, List<MonthPoint> trend)
            implements Serializable {
    }

    /** Spending of a category in the month. {@code categoryId == null} = uncategorised. */
    public record CategorySlice(UUID categoryId, String name, String color, BigDecimal amount, long count)
            implements Serializable {
    }

    /** Totals of a month ({@code yyyy-MM}) for the trend chart. */
    public record MonthPoint(String month, BigDecimal expenses, BigDecimal income) implements Serializable {
    }
}
