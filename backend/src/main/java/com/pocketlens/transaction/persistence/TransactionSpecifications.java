package com.pocketlens.transaction.persistence;

import com.pocketlens.category.domain.Category;
import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.transaction.domain.Transaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Translates {@link TransactionCriteria} into JPA predicates. The same predicates are used for paging,
 * aggregation and id queries, so a figure and the list of transactions behind it always match (US-4.3).
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> matching(TransactionCriteria criteria) {
        return (root, query, cb) -> cb.and(predicates(criteria, root, cb).toArray(Predicate[]::new));
    }

    static List<Predicate> predicates(TransactionCriteria c, Root<Transaction> root, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("userId"), c.userId()));
        if (c.from() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("bookingDate"), c.from()));
        }
        if (c.to() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("bookingDate"), c.to()));
        }
        if (c.categoryId() != null) {
            predicates.add(cb.equal(root.get("category").get("id"), c.categoryId()));
        }
        if (Boolean.TRUE.equals(c.uncategorized())) {
            predicates.add(cb.isNull(root.get("category")));
        }
        if (c.merchant() != null && !c.merchant().isBlank()) {
            predicates.add(cb.like(cb.upper(root.<String>get("merchant")), likePattern(c.merchant())));
        }
        if (c.text() != null && !c.text().isBlank()) {
            String pattern = likePattern(c.text());
            predicates.add(cb.or(cb.like(cb.upper(root.<String>get("merchant")), pattern),
                    cb.like(cb.upper(root.<String>get("description")), pattern)));
        }
        if (c.currency() != null && !c.currency().isBlank()) {
            predicates.add(cb.equal(root.get("currency"), c.currency().toUpperCase(Locale.ROOT)));
        }
        if (c.needsReview() != null) {
            predicates.add(cb.equal(root.get("needsReview"), c.needsReview()));
        }
        if (c.expensesOnly()) {
            Join<Transaction, Category> category = root.join("category", JoinType.LEFT);
            predicates.add(cb.lessThan(root.<BigDecimal>get("amount"), BigDecimal.ZERO));
            predicates.add(cb.or(cb.isNull(category.get("id")), cb.equal(category.get("kind"), CategoryKind.EXPENSE)));
        }
        if (c.ids() != null) {
            predicates.add(c.ids().isEmpty() ? cb.disjunction() : root.get("id").in(c.ids()));
        }
        return predicates;
    }

    private static String likePattern(String value) {
        String escaped = value.strip().toUpperCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
