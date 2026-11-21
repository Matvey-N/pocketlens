package com.pocketlens.user.service;

import com.pocketlens.assistant.persistence.ConversationRepository;
import com.pocketlens.assistant.persistence.MessageRepository;
import com.pocketlens.budget.persistence.BudgetRepository;
import com.pocketlens.categorization.persistence.MerchantRuleRepository;
import com.pocketlens.category.persistence.CategoryRepository;
import com.pocketlens.common.config.CacheConfig;
import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.transaction.persistence.TransactionRepository;
import com.pocketlens.user.domain.User;
import com.pocketlens.user.persistence.UserRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile, data export and account deletion (US-6.3, US-6.4).
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final CategoryRepository categories;
    private final MerchantRuleRepository rules;
    private final BudgetRepository budgets;
    private final TransactionRepository transactions;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @SuppressWarnings("java:S107") // export needs read access to every module's data
    public UserService(UserRepository users, CategoryRepository categories, MerchantRuleRepository rules,
                       BudgetRepository budgets, TransactionRepository transactions,
                       ConversationRepository conversations, MessageRepository messages,
                       PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.categories = categories;
        this.rules = rules;
        this.budgets = budgets;
        this.transactions = transactions;
        this.conversations = conversations;
        this.messages = messages;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    @Transactional(readOnly = true)
    public UserDataExport export(UUID userId) {
        User user = get(userId);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("email", user.getEmail());
        profile.put("displayName", user.getDisplayName());
        profile.put("privacyNoticeVersion", user.getPrivacyNoticeVersion());
        profile.put("consentGivenAt", user.getConsentGivenAt().toString());
        profile.put("createdAt", user.getCreatedAt().toString());

        List<Map<String, Object>> ownCategories = categories.findVisibleTo(userId).stream()
                .filter(c -> userId.equals(c.getUserId()))
                .map(c -> Map.<String, Object>of("name", c.getName(), "color", c.getColor(), "kind", c.getKind().name()))
                .toList();
        List<Map<String, Object>> ownRules = rules.findByUserIdOrderByPattern(userId).stream()
                .map(r -> Map.<String, Object>of("merchant", r.getPattern(), "category", r.getCategory().getName()))
                .toList();
        List<Map<String, Object>> ownBudgets = budgets.findByUserId(userId).stream()
                .map(b -> Map.<String, Object>of("category", b.getCategory().getName(),
                        "monthlyLimit", b.getMonthlyLimit(), "currency", b.getCurrency()))
                .toList();
        List<Map<String, Object>> ownTransactions = transactions.findByUserIdOrderByBookingDateDesc(userId).stream()
                .map(UserService::toMap)
                .toList();
        List<Map<String, Object>> ownConversations = conversations.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(c -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("title", c.getTitle());
                    map.put("createdAt", c.getCreatedAt().toString());
                    map.put("messages", messages.findByConversationIdOrderByCreatedAtAsc(c.getId()).stream()
                            .map(m -> Map.of("role", m.getRole().name(), "content", m.getContent(),
                                    "createdAt", m.getCreatedAt().toString()))
                            .toList());
                    return map;
                })
                .toList();
        log.info("data_exported userId={}", userId);
        return new UserDataExport(clock.instant(), profile, ownCategories, ownRules, ownBudgets, ownTransactions,
                ownConversations);
    }

    /** Deletes the account and, through database cascades, every record that belongs to it (GDPR Art. 17). */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public void delete(UUID userId, String password) {
        User user = get(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "The password is incorrect");
        }
        users.delete(user);
        log.info("account_deleted userId={}", userId);
    }

    private static Map<String, Object> toMap(Transaction t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", t.getBookingDate().toString());
        map.put("amount", t.getAmount());
        map.put("currency", t.getCurrency());
        map.put("description", t.getDescription());
        map.put("category", t.getCategory() == null ? null : t.getCategory().getName());
        map.put("categorizationSource", t.getCategorizationSource().name());
        return map;
    }
}
