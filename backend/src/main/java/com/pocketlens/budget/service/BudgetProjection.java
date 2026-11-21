package com.pocketlens.budget.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Linear projection of month-end spending from the pace so far (US-3.2).
 *
 * @param projectedSpending   expected spending at the end of the month (equals spent for past months)
 * @param projectedOverrunDate first day on which the limit is expected to be exceeded, if the budget is at risk
 */
public record BudgetProjection(BudgetHealth health, BigDecimal spent, BigDecimal remaining,
                               BigDecimal projectedSpending, int percentUsed, LocalDate projectedOverrunDate) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * @param today the current date; months before it are evaluated as finished, the current month by pace
     */
    public static BudgetProjection calculate(BigDecimal limit, BigDecimal spent, YearMonth month, LocalDate today) {
        BigDecimal remaining = limit.subtract(spent);
        int percent = limit.signum() == 0 ? 0
                : spent.multiply(HUNDRED).divide(limit, 0, RoundingMode.HALF_UP).intValue();
        if (spent.compareTo(limit) > 0) {
            return new BudgetProjection(BudgetHealth.EXCEEDED, spent, remaining, spent, percent, null);
        }
        if (!month.equals(YearMonth.from(today))) {
            return new BudgetProjection(BudgetHealth.ON_TRACK, spent, remaining, spent, percent, null);
        }
        int daysInMonth = month.lengthOfMonth();
        int elapsedDays = today.getDayOfMonth();
        BigDecimal dailyPace = spent.divide(BigDecimal.valueOf(elapsedDays), 6, RoundingMode.HALF_UP);
        BigDecimal projected = dailyPace.multiply(BigDecimal.valueOf(daysInMonth)).setScale(2, RoundingMode.HALF_UP);
        if (projected.compareTo(limit) <= 0 || dailyPace.signum() == 0) {
            return new BudgetProjection(BudgetHealth.ON_TRACK, spent, remaining, projected, percent, null);
        }
        int overrunDay = limit.divide(dailyPace, 0, RoundingMode.FLOOR).intValue() + 1;
        LocalDate overrunDate = month.atDay(Math.min(Math.max(overrunDay, elapsedDays), daysInMonth));
        return new BudgetProjection(BudgetHealth.AT_RISK, spent, remaining, projected, percent, overrunDate);
    }
}
