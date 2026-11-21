package com.pocketlens.subscription.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionServiceTest {

    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final SubscriptionService service = new SubscriptionService(transactions, new SubscriptionDetector(),
            TestData.clock());

    @Test
    void detectsSubscriptionsFromExpensesOnly() {
        Transaction s1 = TestData.transaction("2026-09-03", "-10.99", "SPOTIFY");
        Transaction s2 = TestData.transaction("2026-10-03", "-10.99", "SPOTIFY");
        Transaction s3 = TestData.transaction("2026-11-03", "-10.99", "SPOTIFY");
        Transaction t1 = TestData.transaction("2026-09-05", "-100", "TOP-UP");
        Transaction t2 = TestData.transaction("2026-10-05", "-100", "TOP-UP");
        Transaction t3 = TestData.transaction("2026-11-05", "-100", "TOP-UP");
        TestData.category("Transfers", CategoryKind.TRANSFER);
        List.of(t1, t2, t3).forEach(t -> t.categorize(TestData.category("Transfers", CategoryKind.TRANSFER),
                com.pocketlens.categorization.domain.CategorizationSource.RULE, null, false));
        when(transactions.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(s1, s2, s3, t1, t2, t3));

        assertThat(service.detect(USER_ID)).extracting(Subscription::merchant).containsExactly("SPOTIFY");
    }
}
