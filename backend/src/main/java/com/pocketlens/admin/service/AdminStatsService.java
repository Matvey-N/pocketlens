package com.pocketlens.admin.service;

import com.pocketlens.ai.domain.AiUsageRecord;
import com.pocketlens.ai.llm.LlmClient;
import com.pocketlens.ai.persistence.AiUsageRepository;
import com.pocketlens.assistant.domain.Message;
import com.pocketlens.assistant.persistence.MessageRepository;
import com.pocketlens.transaction.persistence.TransactionRepository;
import com.pocketlens.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStatsService {

    private static final Duration PERIOD = Duration.ofDays(30);

    private final UserRepository users;
    private final TransactionRepository transactions;
    private final MessageRepository messages;
    private final AiUsageRepository aiUsage;
    private final LlmClient llmClient;
    private final Clock clock;

    public AdminStatsService(UserRepository users, TransactionRepository transactions, MessageRepository messages,
                             AiUsageRepository aiUsage, LlmClient llmClient, Clock clock) {
        this.users = users;
        this.transactions = transactions;
        this.messages = messages;
        this.aiUsage = aiUsage;
        this.llmClient = llmClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminStats stats() {
        Instant since = clock.instant().minus(PERIOD);
        long aiCalls = aiUsage.countByPurposeAndCreatedAtAfter(AiUsageRecord.Purpose.ASSISTANT, since)
                + aiUsage.countByPurposeAndCreatedAtAfter(AiUsageRecord.Purpose.CATEGORIZATION, since);
        return new AdminStats(users.count(), transactions.count(),
                messages.countByRoleAndCreatedAtAfter(Message.Role.ASSISTANT, since), aiCalls,
                aiUsage.sumInputTokensSince(since), aiUsage.sumOutputTokensSince(since), llmClient.isAvailable());
    }
}
