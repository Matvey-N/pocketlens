package com.pocketlens.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pocketlens.ai.domain.AiUsageRecord;
import com.pocketlens.ai.llm.LlmClient;
import com.pocketlens.ai.persistence.AiUsageRepository;
import com.pocketlens.assistant.domain.Message;
import com.pocketlens.assistant.persistence.MessageRepository;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.persistence.TransactionRepository;
import com.pocketlens.user.persistence.UserRepository;
import org.junit.jupiter.api.Test;

class AdminStatsServiceTest {

    @Test
    void aggregatesUsageOfTheLast30Days() {
        UserRepository users = mock(UserRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        MessageRepository messages = mock(MessageRepository.class);
        AiUsageRepository usage = mock(AiUsageRepository.class);
        LlmClient llm = mock(LlmClient.class);
        when(users.count()).thenReturn(5L);
        when(transactions.count()).thenReturn(1200L);
        when(messages.countByRoleAndCreatedAtAfter(eq(Message.Role.ASSISTANT), any())).thenReturn(40L);
        when(usage.countByPurposeAndCreatedAtAfter(eq(AiUsageRecord.Purpose.ASSISTANT), any())).thenReturn(90L);
        when(usage.countByPurposeAndCreatedAtAfter(eq(AiUsageRecord.Purpose.CATEGORIZATION), any())).thenReturn(10L);
        when(usage.sumInputTokensSince(any())).thenReturn(50_000L);
        when(usage.sumOutputTokensSince(any())).thenReturn(4_000L);
        when(llm.isAvailable()).thenReturn(true);

        AdminStats stats = new AdminStatsService(users, transactions, messages, usage, llm, TestData.clock()).stats();

        assertThat(stats).isEqualTo(new AdminStats(5, 1200, 40, 100, 50_000, 4_000, true));
    }
}
