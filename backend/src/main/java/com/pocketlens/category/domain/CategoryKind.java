package com.pocketlens.category.domain;

/**
 * Kind of a category. Only EXPENSE categories count towards spending and budgets.
 */
public enum CategoryKind {
    EXPENSE,
    INCOME,
    TRANSFER
}
