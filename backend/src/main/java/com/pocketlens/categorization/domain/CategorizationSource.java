package com.pocketlens.categorization.domain;

/**
 * How the category of a transaction was assigned.
 */
public enum CategorizationSource {
    /** Not categorised yet (e.g. the AI service was unavailable). */
    NONE,
    /** Matched a merchant rule (user or global). */
    RULE,
    /** Suggested by the AI classifier. */
    AI,
    /** Set by the user. */
    MANUAL
}
