package com.pocketlens.budget.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class BudgetProjectionTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final BigDecimal LIMIT = new BigDecimal("300.00");

    @Test
    void onTrackWhenPaceStaysWithinLimit() {
        BudgetProjection p = BudgetProjection.calculate(LIMIT, new BigDecimal("100.00"), NOVEMBER,
                LocalDate.of(2026, 11, 15));
        assertThat(p.health()).isEqualTo(BudgetHealth.ON_TRACK);
        assertThat(p.projectedSpending()).isEqualByComparingTo("200.00");
        assertThat(p.remaining()).isEqualByComparingTo("200.00");
        assertThat(p.percentUsed()).isEqualTo(33);
        assertThat(p.projectedOverrunDate()).isNull();
    }

    @Test
    void atRiskWithProjectedOverrunDate() {
        // 200 spent in 10 days = 20/day -> 300 is exceeded on day 16
        BudgetProjection p = BudgetProjection.calculate(LIMIT, new BigDecimal("200.00"), NOVEMBER,
                LocalDate.of(2026, 11, 10));
        assertThat(p.health()).isEqualTo(BudgetHealth.AT_RISK);
        assertThat(p.projectedSpending()).isEqualByComparingTo("600.00");
        assertThat(p.projectedOverrunDate()).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void exceededWhenSpentIsAboveLimit() {
        BudgetProjection p = BudgetProjection.calculate(LIMIT, new BigDecimal("310.00"), NOVEMBER,
                LocalDate.of(2026, 11, 20));
        assertThat(p.health()).isEqualTo(BudgetHealth.EXCEEDED);
        assertThat(p.remaining()).isEqualByComparingTo("-10.00");
    }

    @Test
    void pastMonthsAreEvaluatedWithoutProjection() {
        BudgetProjection p = BudgetProjection.calculate(LIMIT, new BigDecimal("250.00"), YearMonth.of(2026, 10),
                LocalDate.of(2026, 11, 2));
        assertThat(p.health()).isEqualTo(BudgetHealth.ON_TRACK);
        assertThat(p.projectedSpending()).isEqualByComparingTo("250.00");
    }

    @Test
    void zeroSpendingIsOnTrack() {
        BudgetProjection p = BudgetProjection.calculate(LIMIT, BigDecimal.ZERO, NOVEMBER, LocalDate.of(2026, 11, 1));
        assertThat(p.health()).isEqualTo(BudgetHealth.ON_TRACK);
        assertThat(BudgetProjection.calculate(BigDecimal.ZERO, BigDecimal.ZERO, NOVEMBER,
                LocalDate.of(2026, 11, 1)).percentUsed()).isZero();
    }
}
