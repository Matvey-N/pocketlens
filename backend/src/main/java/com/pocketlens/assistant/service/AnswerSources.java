package com.pocketlens.assistant.service;

import com.pocketlens.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.util.List;

/**
 * Transactions behind an answer with their sums per currency (US-4.3).
 */
public record AnswerSources(List<Transaction> transactions, List<Total> totals) {

    /** Sum of the absolute amounts in one currency. */
    public record Total(String currency, BigDecimal amount, long count) {
    }
}
