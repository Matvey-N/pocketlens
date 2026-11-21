package com.pocketlens.budget.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequest(
        @NotNull UUID categoryId,
        @NotNull @DecimalMin(value = "1.00") @DecimalMax("1000000.00") @Digits(integer = 7, fraction = 2)
        BigDecimal monthlyLimit,
        @NotNull @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter ISO currency code") String currency) {
}
