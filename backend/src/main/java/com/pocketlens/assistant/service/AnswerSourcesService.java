package com.pocketlens.assistant.service;

import com.pocketlens.assistant.domain.Message;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the transactions an assistant answer is based on ("Show list", US-4.3).
 */
@Service
public class AnswerSourcesService {

    private final ConversationService conversationService;
    private final TransactionService transactionService;

    public AnswerSourcesService(ConversationService conversationService, TransactionService transactionService) {
        this.conversationService = conversationService;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public AnswerSources load(UUID userId, UUID conversationId, UUID messageId) {
        Message message = conversationService.message(userId, conversationId, messageId);
        List<Transaction> transactions = transactionService.findByIds(userId, message.getSourceTransactionIds())
                .stream()
                .sorted(Comparator.comparing(Transaction::getBookingDate).reversed())
                .toList();
        Map<String, List<Transaction>> byCurrency = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCurrency, TreeMap::new, Collectors.toList()));
        List<AnswerSources.Total> totals = byCurrency.entrySet().stream()
                .map(e -> new AnswerSources.Total(e.getKey(),
                        e.getValue().stream().map(t -> t.getAmount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size()))
                .toList();
        return new AnswerSources(transactions, totals);
    }
}
