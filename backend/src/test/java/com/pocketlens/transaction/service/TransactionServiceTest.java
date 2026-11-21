package com.pocketlens.transaction.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.categorization.domain.CategorizationSource;
import com.pocketlens.categorization.service.RuleService;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class TransactionServiceTest {

    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final CategoryService categories = mock(CategoryService.class);
    private final RuleService rules = mock(RuleService.class);
    private final TransactionService service = new TransactionService(transactions, categories, rules);
    private final Category eatingOut = TestData.expenseCategory("Eating out");

    @Test
    @SuppressWarnings("unchecked")
    void searchesWithUserScopedFilter() {
        Transaction t = TestData.transaction("2026-11-01", "-4.50", "CAFE BLOOM");
        when(transactions.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(t)));
        TransactionFilter filter = new TransactionFilter(null, null, null, null, true, "cafe", "eur");

        assertThat(service.search(USER_ID, filter, PageRequest.of(0, 20)).getContent()).containsExactly(t);
    }

    @Test
    void correctionWithRuleRecategorisesSameMerchant() {
        Transaction target = TestData.transaction("2026-11-01", "-4.50", "CAFE BLOOM");
        Transaction sibling = TestData.transaction("2026-10-01", "-3.50", "CAFE BLOOM");
        when(transactions.findByIdAndUserId(target.getId(), USER_ID)).thenReturn(Optional.of(target));
        when(categories.getVisible(USER_ID, eatingOut.getId())).thenReturn(eatingOut);
        when(transactions.findAutoCategorizedByMerchant(USER_ID, "CAFE BLOOM")).thenReturn(List.of(sibling));

        CategoryUpdateResult result = service.updateCategory(USER_ID, target.getId(), eatingOut.getId(), true);

        assertThat(result.ruleCreated()).isTrue();
        assertThat(result.recategorizedCount()).isEqualTo(1);
        assertThat(target.getCategorizationSource()).isEqualTo(CategorizationSource.MANUAL);
        assertThat(sibling.getCategory()).isEqualTo(eatingOut);
        assertThat(sibling.getCategorizationSource()).isEqualTo(CategorizationSource.RULE);
        verify(rules).upsertUserRule(USER_ID, "CAFE BLOOM", eatingOut);
    }

    @Test
    void correctionWithoutRuleChangesOnlyOneTransaction() {
        Transaction target = TestData.transaction("2026-11-01", "-4.50", "CAFE BLOOM");
        when(transactions.findByIdAndUserId(target.getId(), USER_ID)).thenReturn(Optional.of(target));
        when(categories.getVisible(USER_ID, eatingOut.getId())).thenReturn(eatingOut);

        CategoryUpdateResult result = service.updateCategory(USER_ID, target.getId(), eatingOut.getId(), false);

        assertThat(result.ruleCreated()).isFalse();
        assertThat(target.getCategory()).isEqualTo(eatingOut);
        assertThat(target.isNeedsReview()).isFalse();
        verify(rules, never()).upsertUserRule(any(), any(), any());
    }

    @Test
    void foreignTransactionsAreNotFound() {
        UUID id = UUID.randomUUID();
        when(transactions.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(USER_ID, id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void helpersDelegateToRepository() {
        when(transactions.countByUserIdAndNeedsReviewTrue(USER_ID)).thenReturn(4L);
        assertThat(service.countNeedsReview(USER_ID)).isEqualTo(4);
        when(transactions.findAll(any(Specification.class))).thenReturn(List.of());
        assertThat(service.findByIds(USER_ID, List.of(UUID.randomUUID()))).isEmpty();
        service.releaseCategory(eatingOut.getId());
        verify(transactions).clearCategory(eatingOut.getId());
    }
}
