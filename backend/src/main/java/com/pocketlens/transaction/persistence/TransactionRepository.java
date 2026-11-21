package com.pocketlens.transaction.persistence;

import com.pocketlens.transaction.domain.Transaction;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /** Page of transactions with their category loaded in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    List<Transaction> findAll(Specification<Transaction> spec);

    @EntityGraph(attributePaths = "category")
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    @Query("select t.fingerprint from Transaction t where t.userId = :userId and t.fingerprint in :fingerprints")
    Set<String> findExistingFingerprints(UUID userId, Collection<String> fingerprints);

    /** Transactions of a merchant that may still be re-categorised automatically. */
    @Query("select t from Transaction t where t.userId = :userId and t.merchant = :merchant "
            + "and t.categorizationSource <> com.pocketlens.categorization.domain.CategorizationSource.MANUAL")
    List<Transaction> findAutoCategorizedByMerchant(UUID userId, String merchant);

    /** Transactions waiting for categorisation (the AI was unavailable during import). */
    @Query("select t from Transaction t where t.category is null "
            + "and t.categorizationSource = com.pocketlens.categorization.domain.CategorizationSource.NONE "
            + "order by t.createdAt")
    List<Transaction> findWaitingForCategorization(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("select t from Transaction t where t.userId = :userId and t.amount < 0 and t.bookingDate >= :from "
            + "order by t.bookingDate")
    List<Transaction> findExpensesSince(UUID userId, LocalDate from);

    @EntityGraph(attributePaths = "category")
    List<Transaction> findByUserIdOrderByBookingDateDesc(UUID userId);

    long countByUserIdAndNeedsReviewTrue(UUID userId);

    @Modifying
    @Query("update Transaction t set t.category = null, "
            + "t.categorizationSource = com.pocketlens.categorization.domain.CategorizationSource.NONE, "
            + "t.confidence = null, t.needsReview = true where t.category.id = :categoryId")
    int clearCategory(UUID categoryId);
}
