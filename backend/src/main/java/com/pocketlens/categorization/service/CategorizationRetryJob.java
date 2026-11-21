package com.pocketlens.categorization.service;

import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retries the categorisation of transactions that were imported while the AI service was unavailable (UC-1 A2).
 */
@Component
public class CategorizationRetryJob {

    private static final Logger log = LoggerFactory.getLogger(CategorizationRetryJob.class);
    private static final int BATCH = 200;

    private final TransactionRepository transactions;
    private final CategorizationService categorizationService;
    private final AiCategorizer aiCategorizer;

    public CategorizationRetryJob(TransactionRepository transactions, CategorizationService categorizationService,
                                  AiCategorizer aiCategorizer) {
        this.transactions = transactions;
        this.categorizationService = categorizationService;
        this.aiCategorizer = aiCategorizer;
    }

    @Scheduled(fixedDelayString = "${pocketlens.jobs.categorization-retry-delay:PT30M}",
            initialDelayString = "${pocketlens.jobs.categorization-retry-initial-delay:PT2M}")
    @Transactional
    public void retryWaiting() {
        if (!aiCategorizer.isAvailable()) {
            return;
        }
        List<Transaction> waiting = transactions.findWaitingForCategorization(PageRequest.of(0, BATCH));
        if (waiting.isEmpty()) {
            return;
        }
        Map<UUID, List<Transaction>> byUser = waiting.stream().collect(Collectors.groupingBy(Transaction::getUserId));
        byUser.forEach(categorizationService::categorize);
        log.info("categorization_retry processed={} users={}", waiting.size(), byUser.size());
    }
}
