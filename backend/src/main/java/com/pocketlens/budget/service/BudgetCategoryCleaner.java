package com.pocketlens.budget.service;

import com.pocketlens.budget.persistence.BudgetRepository;
import com.pocketlens.category.service.CategoryUsageCleaner;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Deletes the budgets of a category that is being deleted.
 */
@Component
public class BudgetCategoryCleaner implements CategoryUsageCleaner {

    private final BudgetRepository budgets;

    public BudgetCategoryCleaner(BudgetRepository budgets) {
        this.budgets = budgets;
    }

    @Override
    public void releaseCategory(UUID categoryId) {
        budgets.deleteByCategoryId(categoryId);
    }
}
