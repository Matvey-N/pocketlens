package com.pocketlens.transaction.persistence;

import com.pocketlens.category.domain.Category;
import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.transaction.domain.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Aggregation queries executed by the database. Figures shown to the user and to the assistant are computed here,
 * never by the language model.
 */
@Repository
public class TransactionAggregates {

    private final EntityManager entityManager;

    public TransactionAggregates(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Spending per category and currency (expense criteria are applied automatically). */
    public List<CategoryTotal> spendingByCategory(TransactionCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Transaction> root = query.from(Transaction.class);
        Join<Transaction, Category> category = root.join("category", JoinType.LEFT);
        List<Predicate> predicates = TransactionSpecifications.predicates(criteria, root, cb);
        predicates.add(cb.lessThan(root.<BigDecimal>get("amount"), BigDecimal.ZERO));
        predicates.add(cb.or(cb.isNull(category.get("id")), cb.equal(category.get("kind"), CategoryKind.EXPENSE)));
        Expression<BigDecimal> total = cb.sum(cb.neg(root.<BigDecimal>get("amount")));
        Expression<Long> count = cb.count(root);
        query.multiselect(category.get("id"), category.get("name"), category.get("color"), root.get("currency"),
                        total, count)
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(category.get("id"), category.get("name"), category.get("color"), root.get("currency"))
                .orderBy(cb.desc(total));
        return entityManager.createQuery(query).getResultList().stream()
                .map(t -> new CategoryTotal(t.get(0, UUID.class), t.get(1, String.class), t.get(2, String.class),
                        t.get(3, String.class), t.get(4, BigDecimal.class), t.get(5, Long.class)))
                .toList();
    }

    /** Signed sum of amounts per currency for the criteria. */
    public List<CurrencyTotal> sumByCurrency(TransactionCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Transaction> root = query.from(Transaction.class);
        List<Predicate> predicates = TransactionSpecifications.predicates(criteria, root, cb);
        query.multiselect(root.get("currency"), cb.sum(root.<BigDecimal>get("amount")), cb.count(root))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(root.get("currency"))
                .orderBy(cb.asc(root.get("currency")));
        return entityManager.createQuery(query).getResultList().stream()
                .map(t -> new CurrencyTotal(t.get(0, String.class), t.get(1, BigDecimal.class), t.get(2, Long.class)))
                .toList();
    }

    /** Ids of the transactions matching the criteria (at most {@code limit}). */
    public List<UUID> ids(TransactionCriteria criteria, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UUID> query = cb.createQuery(UUID.class);
        Root<Transaction> root = query.from(Transaction.class);
        query.select(root.<UUID>get("id"))
                .where(TransactionSpecifications.predicates(criteria, root, cb).toArray(Predicate[]::new))
                .orderBy(cb.desc(root.get("bookingDate")));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }

    /** Monthly expenses and income per currency; transfers are excluded. */
    public List<MonthlyTotal> monthlyTotals(UUID userId, LocalDate from, LocalDate to) {
        List<Object[]> rows = entityManager.createQuery("""
                        select year(t.bookingDate), month(t.bookingDate), t.currency,
                               sum(case when t.amount < 0 and (c.id is null or c.kind = :expense)
                                        then -t.amount else 0 end),
                               sum(case when t.amount > 0 and (c.id is null or c.kind <> :transfer)
                                        then t.amount else 0 end)
                        from Transaction t left join t.category c
                        where t.userId = :userId and t.bookingDate between :from and :to
                        group by year(t.bookingDate), month(t.bookingDate), t.currency
                        order by year(t.bookingDate), month(t.bookingDate), t.currency
                        """, Object[].class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("expense", CategoryKind.EXPENSE)
                .setParameter("transfer", CategoryKind.TRANSFER)
                .getResultList();
        return rows.stream()
                .map(r -> new MonthlyTotal(((Number) r[0]).intValue(), ((Number) r[1]).intValue(), (String) r[2],
                        toBigDecimal(r[3]), toBigDecimal(r[4])))
                .toList();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
