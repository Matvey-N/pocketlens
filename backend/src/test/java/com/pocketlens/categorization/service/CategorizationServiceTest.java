package com.pocketlens.categorization.service;

import static com.pocketlens.support.TestData.NOW;
import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.ai.llm.AiUnavailableException;
import com.pocketlens.categorization.domain.CategorizationSource;
import com.pocketlens.categorization.domain.MerchantRule;
import com.pocketlens.categorization.persistence.MerchantRuleRepository;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryKeys;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategorizationServiceTest {

    private final MerchantRuleRepository rules = mock(MerchantRuleRepository.class);
    private final AiCategorizer ai = mock(AiCategorizer.class);
    private final CategoryService categories = mock(CategoryService.class);
    private final Category groceries = TestData.expenseCategory("Groceries");
    private final Category eatingOut = TestData.expenseCategory("Eating out");
    private final Category other = TestData.expenseCategory("Other");
    private CategorizationService service;

    @BeforeEach
    void setUp() {
        service = new CategorizationService(rules, new RuleEngine(), ai, categories, TestData.properties());
        when(rules.findApplicableTo(USER_ID)).thenReturn(List.of(new MerchantRule(null, "LIDL", groceries, NOW)));
        when(categories.listVisible(USER_ID)).thenReturn(List.of(groceries, eatingOut, other));
        when(categories.getBySystemKey(CategoryKeys.OTHER)).thenReturn(other);
    }

    @Test
    void knownMerchantsAreCategorisedByRulesWithoutAi() {
        Transaction lidl = TestData.transaction("2026-11-01", "-12.30", "LIDL VILNIUS");

        CategorizationOutcome outcome = service.categorize(USER_ID, List.of(lidl));

        assertThat(outcome.byRules()).isEqualTo(1);
        assertThat(lidl.getCategory()).isEqualTo(groceries);
        assertThat(lidl.getCategorizationSource()).isEqualTo(CategorizationSource.RULE);
        assertThat(lidl.isNeedsReview()).isFalse();
        verify(ai, never()).suggest(any(), anyList(), anyList());
    }

    @Test
    void unknownMerchantsAreCategorisedByAiAndLowConfidenceNeedsReview() {
        Transaction cafe = TestData.transaction("2026-11-02", "-4.50", "CAFE BLOOM");
        Transaction shop = TestData.transaction("2026-11-03", "-20.00", "XYZ STORE");
        Transaction unknown = TestData.transaction("2026-11-04", "-3.00", "ABC");
        when(ai.isAvailable()).thenReturn(true);
        when(ai.suggest(eq(USER_ID), anyList(), anyList())).thenReturn(Map.of(
                cafe.getId(), new AiSuggestion(eatingOut, new BigDecimal("0.95")),
                shop.getId(), new AiSuggestion(groceries, new BigDecimal("0.55"))));

        CategorizationOutcome outcome = service.categorize(USER_ID, List.of(cafe, shop, unknown));

        assertThat(outcome.byAi()).isEqualTo(2);
        assertThat(outcome.needsReview()).isEqualTo(2);
        assertThat(cafe.getCategory()).isEqualTo(eatingOut);
        assertThat(cafe.isNeedsReview()).isFalse();
        assertThat(shop.isNeedsReview()).isTrue();
        assertThat(unknown.getCategory()).isEqualTo(other);
        assertThat(unknown.isNeedsReview()).isTrue();
    }

    @Test
    void transactionsStayUncategorisedWhenAiIsNotConfigured() {
        Transaction cafe = TestData.transaction("2026-11-02", "-4.50", "CAFE BLOOM");
        when(ai.isAvailable()).thenReturn(false);

        CategorizationOutcome outcome = service.categorize(USER_ID, List.of(cafe));

        assertThat(outcome.aiAvailable()).isFalse();
        assertThat(outcome.uncategorized()).isEqualTo(1);
        assertThat(cafe.getCategory()).isNull();
        assertThat(cafe.getCategorizationSource()).isEqualTo(CategorizationSource.NONE);
    }

    @Test
    void transactionsStayUncategorisedWhenAiFails() {
        Transaction cafe = TestData.transaction("2026-11-02", "-4.50", "CAFE BLOOM");
        when(ai.isAvailable()).thenReturn(true);
        when(ai.suggest(eq(USER_ID), anyList(), anyList())).thenThrow(new AiUnavailableException("down"));

        CategorizationOutcome outcome = service.categorize(USER_ID, List.of(cafe));

        assertThat(outcome.aiAvailable()).isFalse();
        assertThat(cafe.getCategorizationSource()).isEqualTo(CategorizationSource.NONE);
        assertThat(cafe.isNeedsReview()).isTrue();
    }

    @Test
    void manualCategoriesAreNeverChanged() {
        Transaction lidl = TestData.transaction("2026-11-01", "-12.30", "LIDL VILNIUS");
        lidl.categorizeManually(eatingOut);

        CategorizationOutcome outcome = service.categorize(USER_ID, List.of(lidl));

        assertThat(outcome.byRules()).isZero();
        assertThat(lidl.getCategory()).isEqualTo(eatingOut);
    }

    @Test
    void emptyInputDoesNothing() {
        assertThat(service.categorize(USER_ID, List.of())).isEqualTo(CategorizationOutcome.empty());
    }
}
