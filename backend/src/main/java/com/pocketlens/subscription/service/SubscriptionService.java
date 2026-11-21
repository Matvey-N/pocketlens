package com.pocketlens.subscription.service;

import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recurring payments of the last 13 months (US-5.1, US-5.2).
 */
@Service
public class SubscriptionService {

    private static final int LOOKBACK_MONTHS = 13;

    private final TransactionRepository transactions;
    private final SubscriptionDetector detector;
    private final Clock clock;

    public SubscriptionService(TransactionRepository transactions, SubscriptionDetector detector, Clock clock) {
        this.transactions = transactions;
        this.detector = detector;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Subscription> detect(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        List<PaymentPoint> payments = transactions.findExpensesSince(userId, today.minusMonths(LOOKBACK_MONTHS))
                .stream()
                .filter(SubscriptionService::isSpending)
                .map(t -> new PaymentPoint(t.getId(), t.getMerchant(), t.getCurrency(), t.getBookingDate(),
                        t.getAmount().negate()))
                .toList();
        return detector.detect(payments, today);
    }

    private static boolean isSpending(Transaction t) {
        return t.getCategory() == null || t.getCategory().getKind() == CategoryKind.EXPENSE;
    }
}
