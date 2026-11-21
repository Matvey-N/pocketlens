package com.pocketlens.budget.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BudgetLimitRequest(
        @NotNull @DecimalMin(value = "1.00") @DecimalMax("1000000.00") @Digits(integer = 7, fraction = 2)
        BigDecimal monthlyLimit) {
}
