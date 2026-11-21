package com.pocketlens.transaction.service;

import com.pocketlens.transaction.domain.Transaction;

/**
 * Result of a category correction.
 *
 * @param ruleCreated       a rule "merchant → category" was created or updated
 * @param recategorizedCount other transactions of the same merchant that received the new category
 */
public record CategoryUpdateResult(Transaction transaction, boolean ruleCreated, int recategorizedCount) {
}
