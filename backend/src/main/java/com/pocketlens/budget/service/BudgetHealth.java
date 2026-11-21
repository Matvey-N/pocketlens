package com.pocketlens.budget.service;

/**
 * State of a budget in a month.
 */
public enum BudgetHealth {
    /** Spending pace stays within the limit. */
    ON_TRACK,
    /** At the current pace the limit will be exceeded before the month ends (warning, US-3.2). */
    AT_RISK,
    /** The limit is already exceeded. */
    EXCEEDED
}
