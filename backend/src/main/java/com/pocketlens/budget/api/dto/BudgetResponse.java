package com.pocketlens.budget.api.dto;

import com.pocketlens.budget.domain.Budget;
import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(UUID id, UUID categoryId, String categoryName, BigDecimal monthlyLimit, String currency) {

    public static BudgetResponse from(Budget b) {
        return new BudgetResponse(b.getId(), b.getCategory().getId(), b.getCategory().getName(), b.getMonthlyLimit(),
                b.getCurrency());
    }
}
