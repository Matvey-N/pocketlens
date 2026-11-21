package com.pocketlens.transaction.service;

import com.pocketlens.categorization.domain.CategorizationSource;
import com.pocketlens.categorization.service.RuleService;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.config.CacheConfig;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionCriteria;
import com.pocketlens.transaction.persistence.TransactionRepository;
import com.pocketlens.transaction.persistence.TransactionSpecifications;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listing and correcting transactions (US-2.3, US-3.4).
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final String RESOURCE = "Transaction";

    private final TransactionRepository transactions;
    private final CategoryService categoryService;
    private final RuleService ruleService;

    public TransactionService(TransactionRepository transactions, CategoryService categoryService,
                              RuleService ruleService) {
        this.transactions = transactions;
        this.categoryService = categoryService;
        this.ruleService = ruleService;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> search(UUID userId, TransactionFilter filter, Pageable pageable) {
        TransactionCriteria criteria = TransactionCriteria.forUser(userId)
                .period(filter.from(), filter.to())
                .category(filter.categoryId())
                .uncategorized(filter.uncategorized())
                .needsReview(filter.needsReview())
                .text(filter.q())
                .currency(filter.currency())
                .build();
        return transactions.findAll(TransactionSpecifications.matching(criteria), pageable);
    }

    @Transactional(readOnly = true)
    public Transaction get(UUID userId, UUID transactionId) {
        return transactions.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException(RESOURCE, transactionId));
    }

    @Transactional(readOnly = true)
    public List<Transaction> findByIds(UUID userId, List<UUID> ids) {
        return transactions.findAll(TransactionSpecifications.matching(
                TransactionCriteria.forUser(userId).ids(ids).build()));
    }

    /**
     * Sets the category chosen by the user. With {@code applyToMerchant} a rule is created and all transactions of
     * the same merchant that were not corrected manually are re-categorised (US-2.3).
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public CategoryUpdateResult updateCategory(UUID userId, UUID transactionId, UUID categoryId,
                                               boolean applyToMerchant) {
        Transaction transaction = get(userId, transactionId);
        Category category = categoryService.getVisible(userId, categoryId);
        transaction.categorizeManually(category);
        if (!applyToMerchant) {
            return new CategoryUpdateResult(transaction, false, 0);
        }
        ruleService.upsertUserRule(userId, transaction.getMerchant(), category);
        List<Transaction> sameMerchant = transactions.findAutoCategorizedByMerchant(userId, transaction.getMerchant());
        sameMerchant.forEach(t -> t.categorize(category, CategorizationSource.RULE, BigDecimal.ONE, false));
        log.info("category_corrected userId={} ruleCreated=true recategorized={}", userId, sameMerchant.size());
        return new CategoryUpdateResult(transaction, true, sameMerchant.size());
    }

    @Transactional(readOnly = true)
    public long countNeedsReview(UUID userId) {
        return transactions.countByUserIdAndNeedsReviewTrue(userId);
    }
}
