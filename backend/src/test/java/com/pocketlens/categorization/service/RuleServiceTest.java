package com.pocketlens.categorization.service;

import static com.pocketlens.support.TestData.NOW;
import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.categorization.domain.MerchantRule;
import com.pocketlens.categorization.persistence.MerchantRuleRepository;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.support.TestData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RuleServiceTest {

    private final MerchantRuleRepository rules = mock(MerchantRuleRepository.class);
    private final CategoryService categories = mock(CategoryService.class);
    private final RuleService service = new RuleService(rules, new MerchantNormalizer(), categories, TestData.clock());
    private final Category groceries = TestData.expenseCategory("Groceries");
    private final Category eatingOut = TestData.expenseCategory("Eating out");

    @Test
    void createsUserRuleOrUpdatesExistingOne() {
        when(rules.findByUserIdAndPattern(USER_ID, "CAFE BLOOM")).thenReturn(Optional.empty());
        when(rules.save(any())).thenAnswer(i -> i.getArgument(0));
        MerchantRule created = service.upsertUserRule(USER_ID, "CAFE BLOOM", eatingOut);
        assertThat(created.getCategory()).isEqualTo(eatingOut);
        assertThat(created.isGlobal()).isFalse();

        MerchantRule existing = new MerchantRule(USER_ID, "LIDL", eatingOut, NOW);
        when(rules.findByUserIdAndPattern(USER_ID, "LIDL")).thenReturn(Optional.of(existing));
        assertThat(service.upsertUserRule(USER_ID, "LIDL", groceries).getCategory()).isEqualTo(groceries);
    }

    @Test
    void listsAndDeletesUserRules() {
        MerchantRule rule = TestData.withId(new MerchantRule(USER_ID, "LIDL", groceries, NOW), UUID.randomUUID());
        when(rules.findByUserIdOrderByPattern(USER_ID)).thenReturn(List.of(rule));
        when(rules.findByIdAndUserId(rule.getId(), USER_ID)).thenReturn(Optional.of(rule));

        assertThat(service.listUserRules(USER_ID)).containsExactly(rule);
        service.deleteUserRule(USER_ID, rule.getId());
        verify(rules).delete(rule);

        UUID missing = UUID.randomUUID();
        when(rules.findByIdAndUserId(missing, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteUserRule(USER_ID, missing)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void managesGlobalRules() {
        when(categories.listDefaults()).thenReturn(List.of(groceries));
        when(rules.findByUserIdIsNullAndPattern("ALDI")).thenReturn(Optional.empty());
        when(rules.save(any())).thenAnswer(i -> i.getArgument(0));

        MerchantRule rule = service.createGlobalRule(" aldi ", groceries.getId());
        assertThat(rule.isGlobal()).isTrue();
        assertThat(rule.getPattern()).isEqualTo("ALDI");

        when(rules.findByUserIdIsNullAndPattern("ALDI")).thenReturn(Optional.of(rule));
        assertThatThrownBy(() -> service.createGlobalRule("ALDI", groceries.getId()))
                .isInstanceOf(ConflictException.class);
        when(rules.findByUserIdIsNullAndPattern("NEW")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createGlobalRule("NEW", UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);

        when(rules.findByUserIdIsNullOrderByPattern()).thenReturn(List.of(rule));
        assertThat(service.listGlobalRules()).containsExactly(rule);
    }

    @Test
    void deletesOnlyGlobalRulesAsAdmin() {
        MerchantRule global = TestData.withId(new MerchantRule(null, "ALDI", groceries, NOW), UUID.randomUUID());
        MerchantRule user = TestData.withId(new MerchantRule(USER_ID, "X", groceries, NOW), UUID.randomUUID());
        when(rules.findById(global.getId())).thenReturn(Optional.of(global));
        when(rules.findById(user.getId())).thenReturn(Optional.of(user));

        service.deleteGlobalRule(global.getId());
        verify(rules).delete(global);
        assertThatThrownBy(() -> service.deleteGlobalRule(user.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void releasesRulesOfDeletedCategory() {
        service.releaseCategory(groceries.getId());
        verify(rules).deleteByCategoryId(groceries.getId());
    }
}
