package com.pocketlens.budget.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Budget with its spending in a month.
 */
public record BudgetStatus(UUID budgetId, UUID categoryId, String categoryName, String categoryColor,
                           String currency, BigDecimal limit, BigDecimal spent, BigDecimal remaining,
                           BigDecimal projectedSpending, int percentUsed, BudgetHealth health,
                           LocalDate projectedOverrunDate) implements Serializable {
}
