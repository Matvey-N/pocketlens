package com.pocketlens.transaction.persistence;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spending of one category in one currency (positive number). {@code categoryId == null} means uncategorised.
 */
public record CategoryTotal(UUID categoryId, String categoryName, String color, String currency,
                            BigDecimal total, long count) {
}
