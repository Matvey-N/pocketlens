package com.pocketlens.categorization.service;

import com.pocketlens.category.domain.Category;
import java.math.BigDecimal;

/**
 * Category suggested by the AI classifier with its confidence (0–1).
 */
public record AiSuggestion(Category category, BigDecimal confidence) {
}
