package com.pocketlens.budget.service;

import static com.pocketlens.support.TestData.NOW;
import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.budget.domain.Budget;
import com.pocketlens.budget.domain.BudgetWarning;
import com.pocketlens.budget.persistence.BudgetRepository;
import com.pocketlens.budget.persistence.BudgetWarningRepository;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.persistence.CategoryTotal;
import com.pocketlens.transaction.persistence.TransactionAggregates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BudgetServiceTest {

    private final BudgetRepository budgets = mock(BudgetRepository.class);
    private final BudgetWarningRepository warnings = mock(BudgetWarningRepository.class);
    private final TransactionAggregates aggregates = mock(TransactionAggregates.class);
    private final CategoryService categories = mock(CategoryService.class);
    private final BudgetService service = new BudgetService(budgets, warnings, aggregates, categories,
            TestData.clock());
    private final Category groceries = TestData.expenseCategory("Groceries");
    private final Category transport = TestData.expenseCategory("Transport");

    @Test
    void calculatesStatusesAndRecordsOneWarningPerDay() {
        Budget food = TestData.withId(new Budget(USER_ID, groceries, new BigDecimal("300"), "EUR", NOW),
                UUID.randomUUID());
        Budget travel = TestData.withId(new Budget(USER_ID, transport, new BigDecimal("100"), "EUR", NOW),
                UUID.randomUUID());
        when(budgets.findByUserId(USER_ID)).thenReturn(List.of(food, travel));
        when(aggregates.spendingByCategory(any())).thenReturn(List.of(
                new CategoryTotal(groceries.getId(), "Groceries", "#1", "EUR", new BigDecimal("250"), 10),
                new CategoryTotal(null, null, null, "EUR", new BigDecimal("40"), 2),
                new CategoryTotal(transport.getId(), "Transport", "#2", "USD", new BigDecimal("90"), 1)));
        when(warnings.existsByBudgetIdAndWarnedOn(food.getId(), TestData.TODAY)).thenReturn(false);

        List<BudgetStatus> statuses = service.statuses(USER_ID, YearMonth.of(2026, 11));

        assertThat(statuses).hasSize(2);
        assertThat(statuses.get(0).health()).isEqualTo(BudgetHealth.AT_RISK);
        assertThat(statuses.get(1).spent()).isEqualByComparingTo("0");
        ArgumentCaptor<BudgetWarning> warning = ArgumentCaptor.forClass(BudgetWarning.class);
        verify(warnings).save(warning.capture());
        assertThat(warning.getValue().getWarnedOn()).isEqualTo(TestData.TODAY);
        assertThat(warning.getValue().getProjectedOverrunDate()).isAfter(LocalDate.of(2026, 11, 15));

        when(warnings.existsByBudgetIdAndWarnedOn(food.getId(), TestData.TODAY)).thenReturn(true);
        service.statuses(USER_ID, YearMonth.of(2026, 11));
        verify(warnings).save(any());
    }

    @Test
    void returnsNothingWithoutBudgets() {
        when(budgets.findByUserId(USER_ID)).thenReturn(List.of());
        assertThat(service.statuses(USER_ID, YearMonth.of(2026, 11))).isEmpty();
        verify(aggregates, never()).spendingByCategory(any());
    }

    @Test
    void createsBudgetForExpenseCategoryOnly() {
        when(categories.getVisible(USER_ID, groceries.getId())).thenReturn(groceries);
        when(budgets.save(any())).thenAnswer(i -> i.getArgument(0));
        Budget created = service.create(USER_ID, groceries.getId(), new BigDecimal("300"), "eur");
        assertThat(created.getCurrency()).isEqualTo("EUR");

        when(budgets.existsByUserIdAndCategoryIdAndCurrency(USER_ID, groceries.getId(), "EUR")).thenReturn(true);
        assertThatThrownBy(() -> service.create(USER_ID, groceries.getId(), BigDecimal.TEN, "EUR"))
                .isInstanceOf(ConflictException.class);

        Category income = TestData.category("Income", CategoryKind.INCOME);
        when(categories.getVisible(USER_ID, income.getId())).thenReturn(income);
        assertThatThrownBy(() -> service.create(USER_ID, income.getId(), BigDecimal.TEN, "EUR"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void updatesAndDeletesOwnBudgets() {
        Budget food = TestData.withId(new Budget(USER_ID, groceries, new BigDecimal("300"), "EUR", NOW),
                UUID.randomUUID());
        when(budgets.findByIdAndUserId(food.getId(), USER_ID)).thenReturn(Optional.of(food));

        assertThat(service.updateLimit(USER_ID, food.getId(), new BigDecimal("350")).getMonthlyLimit())
                .isEqualByComparingTo("350");
        service.delete(USER_ID, food.getId());
        verify(budgets).delete(food);

        UUID missing = UUID.randomUUID();
        when(budgets.findByIdAndUserId(missing, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(USER_ID, missing)).isInstanceOf(NotFoundException.class);

        service.releaseCategory(groceries.getId());
        verify(budgets).deleteByCategoryId(groceries.getId());
    }
}
