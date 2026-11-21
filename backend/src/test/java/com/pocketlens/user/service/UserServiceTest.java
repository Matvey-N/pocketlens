package com.pocketlens.user.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.assistant.domain.Conversation;
import com.pocketlens.assistant.domain.Message;
import com.pocketlens.assistant.persistence.ConversationRepository;
import com.pocketlens.assistant.persistence.MessageRepository;
import com.pocketlens.budget.domain.Budget;
import com.pocketlens.budget.persistence.BudgetRepository;
import com.pocketlens.categorization.domain.MerchantRule;
import com.pocketlens.categorization.persistence.MerchantRuleRepository;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.persistence.CategoryRepository;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.support.TestData;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import com.pocketlens.user.domain.User;
import com.pocketlens.user.persistence.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final MerchantRuleRepository rules = mock(MerchantRuleRepository.class);
    private final BudgetRepository budgets = mock(BudgetRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final MessageRepository messages = mock(MessageRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(users, categories, rules, budgets, transactions,
            conversations, messages, encoder, TestData.clock());
    private final User user = TestData.user();

    @Test
    void exportsAllPersonalData() {
        Category own = TestData.userCategory(USER_ID, "Pets");
        Category global = TestData.expenseCategory("Groceries");
        Transaction lidl = TestData.transaction("2026-11-01", "-12.30", "LIDL");
        lidl.categorizeManually(global);
        Conversation conversation = TestData.withId(new Conversation(USER_ID, TestData.NOW), UUID.randomUUID());
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categories.findVisibleTo(USER_ID)).thenReturn(List.of(own, global));
        when(rules.findByUserIdOrderByPattern(USER_ID)).thenReturn(List.of(new MerchantRule(USER_ID, "LIDL", global,
                TestData.NOW)));
        when(budgets.findByUserId(USER_ID)).thenReturn(List.of(new Budget(USER_ID, global, BigDecimal.TEN, "EUR",
                TestData.NOW)));
        when(transactions.findByUserIdOrderByBookingDateDesc(USER_ID)).thenReturn(List.of(lidl,
                TestData.transaction("2026-11-02", "-1", "X")));
        when(conversations.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of(conversation));
        when(messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId()))
                .thenReturn(List.of(Message.user(conversation.getId(), "hi", TestData.NOW)));

        UserDataExport export = service.export(USER_ID);

        assertThat(export.profile()).containsEntry("email", "anna@example.com");
        assertThat(export.categories()).hasSize(1);
        assertThat(export.rules()).hasSize(1);
        assertThat(export.budgets()).hasSize(1);
        assertThat(export.transactions()).hasSize(2);
        assertThat(export.transactions().get(0)).containsEntry("category", "Groceries");
        assertThat(export.conversations()).hasSize(1);
        assertThat(export.exportedAt()).isEqualTo(TestData.NOW);
    }

    @Test
    void deletesAccountAfterPasswordConfirmation() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(encoder.matches("right", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.delete(USER_ID, "wrong")).isInstanceOf(ApiException.class);
        verify(users, never()).delete(user);
        service.delete(USER_ID, "right");
        verify(users).delete(user);
    }

    @Test
    void unknownUserIsNotFound() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(USER_ID)).isInstanceOf(NotFoundException.class);
    }
}
