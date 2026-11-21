package com.pocketlens.categorization.service;

import static com.pocketlens.support.TestData.OTHER_USER_ID;
import static com.pocketlens.support.TestData.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategorizationRetryJobTest {

    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final CategorizationService categorization = mock(CategorizationService.class);
    private final AiCategorizer ai = mock(AiCategorizer.class);
    private final CategorizationRetryJob job = new CategorizationRetryJob(transactions, categorization, ai);

    @Test
    void retriesWaitingTransactionsPerUser() {
        Transaction a = TestData.transaction(USER_ID, "2026-11-01", "-1.00", "EUR", "A");
        Transaction b = TestData.transaction(OTHER_USER_ID, "2026-11-01", "-2.00", "EUR", "B");
        when(ai.isAvailable()).thenReturn(true);
        when(transactions.findWaitingForCategorization(any())).thenReturn(List.of(a, b));

        job.retryWaiting();

        verify(categorization).categorize(USER_ID, List.of(a));
        verify(categorization).categorize(OTHER_USER_ID, List.of(b));
    }

    @Test
    void doesNothingWithoutAiOrWaitingTransactions() {
        when(ai.isAvailable()).thenReturn(false);
        job.retryWaiting();
        verify(transactions, never()).findWaitingForCategorization(any());

        when(ai.isAvailable()).thenReturn(true);
        when(transactions.findWaitingForCategorization(any())).thenReturn(List.of());
        job.retryWaiting();
        verify(categorization, never()).categorize(any(), any());
    }
}
