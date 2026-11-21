package com.pocketlens.transaction.service;

import com.pocketlens.category.service.CategoryUsageCleaner;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Returns the transactions of a category that is being deleted to the review queue.
 */
@Component
public class TransactionCategoryCleaner implements CategoryUsageCleaner {

    private final TransactionRepository transactions;

    public TransactionCategoryCleaner(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    public void releaseCategory(UUID categoryId) {
        transactions.clearCategory(categoryId);
    }
}
