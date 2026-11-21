package com.pocketlens.transaction.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Category correction. {@code applyToMerchant} = "Always use this category for this merchant".
 */
public record UpdateTransactionRequest(@NotNull UUID categoryId, boolean applyToMerchant) {
}
