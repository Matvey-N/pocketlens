package com.pocketlens.budget.service;

import com.pocketlens.budget.domain.Budget;
import com.pocketlens.budget.domain.BudgetWarning;
import com.pocketlens.budget.persistence.BudgetRepository;
import com.pocketlens.budget.persistence.BudgetWarningRepository;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.config.CacheConfig;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.transaction.persistence.CategoryTotal;
import com.pocketlens.transaction.persistence.TransactionAggregates;
import com.pocketlens.transaction.persistence.TransactionCriteria;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monthly budgets with early warnings (US-3.1, US-3.2).
 */
@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);
    private static final String RESOURCE = "Budget";

    private final BudgetRepository budgets;
    private final BudgetWarningRepository warnings;
    private final TransactionAggregates aggregates;
    private final CategoryService categoryService;
    private final Clock clock;

    public BudgetService(BudgetRepository budgets, BudgetWarningRepository warnings, TransactionAggregates aggregates,
                         CategoryService categoryService, Clock clock) {
        this.budgets = budgets;
        this.warnings = warnings;
        this.aggregates = aggregates;
        this.categoryService = categoryService;
        this.clock = clock;
    }

    /**
     * Budgets of the user with spending and projection for the month. For the current month, budgets at risk are
     * recorded as warnings (once per budget and day).
     */
    @Transactional
    public List<BudgetStatus> statuses(UUID userId, YearMonth month) {
        List<Budget> userBudgets = budgets.findByUserId(userId);
        if (userBudgets.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(clock);
        Map<String, BigDecimal> spentByKey = aggregates.spendingByCategory(TransactionCriteria.forUser(userId)
                        .period(month.atDay(1), month.atEndOfMonth()).build())
                .stream()
                .filter(t -> t.categoryId() != null)
                .collect(Collectors.toMap(t -> key(t.categoryId(), t.currency()), CategoryTotal::total, BigDecimal::add));
        return userBudgets.stream().map(budget -> {
            BigDecimal spent = spentByKey.getOrDefault(key(budget.getCategory().getId(), budget.getCurrency()),
                    BigDecimal.ZERO);
            BudgetProjection projection = BudgetProjection.calculate(budget.getMonthlyLimit(), spent, month, today);
            if (projection.health() == BudgetHealth.AT_RISK) {
                recordWarning(budget, today, projection.projectedOverrunDate());
            }
            return toStatus(budget, projection);
        }).toList();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public Budget create(UUID userId, UUID categoryId, BigDecimal limit, String currency) {
        Category category = categoryService.getVisible(userId, categoryId);
        if (category.getKind() != CategoryKind.EXPENSE) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Budgets can only be set for expense categories");
        }
        String code = currency.toUpperCase(Locale.ROOT);
        if (budgets.existsByUserIdAndCategoryIdAndCurrency(userId, categoryId, code)) {
            throw new ConflictException("A budget for " + category.getName() + " in " + code + " already exists");
        }
        return budgets.save(new Budget(userId, category, limit, code, clock.instant()));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public Budget updateLimit(UUID userId, UUID budgetId, BigDecimal limit) {
        Budget budget = getOwned(userId, budgetId);
        budget.changeLimit(limit);
        return budget;
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public void delete(UUID userId, UUID budgetId) {
        budgets.delete(getOwned(userId, budgetId));
    }

    private Budget getOwned(UUID userId, UUID budgetId) {
        return budgets.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new NotFoundException(RESOURCE, budgetId));
    }

    private void recordWarning(Budget budget, LocalDate today, LocalDate projectedOverrun) {
        if (!warnings.existsByBudgetIdAndWarnedOn(budget.getId(), today)) {
            warnings.save(new BudgetWarning(budget.getId(), today, projectedOverrun));
            log.info("budget_warning budgetId={} projectedOverrun={}", budget.getId(), projectedOverrun);
        }
    }

    private static BudgetStatus toStatus(Budget budget, BudgetProjection p) {
        Category c = budget.getCategory();
        return new BudgetStatus(budget.getId(), c.getId(), c.getName(), c.getColor(), budget.getCurrency(),
                budget.getMonthlyLimit(), p.spent(), p.remaining(), p.projectedSpending(), p.percentUsed(),
                p.health(), p.projectedOverrunDate());
    }

    private static String key(UUID categoryId, String currency) {
        return categoryId + "|" + currency;
    }
}
