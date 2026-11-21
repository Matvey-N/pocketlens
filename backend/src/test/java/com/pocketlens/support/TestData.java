package com.pocketlens.support;

import com.pocketlens.category.domain.Category;
import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.common.config.AppProperties;
import com.pocketlens.transaction.domain.Transaction;
import com.pocketlens.user.domain.Role;
import com.pocketlens.user.domain.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test fixtures shared by unit tests.
 */
public final class TestData {

    public static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    public static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    private TestData() {
    }

    public static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static AppProperties properties() {
        return properties("");
    }

    public static AppProperties properties(String aiKey) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-0123", "pocketlens",
                        Duration.ofMinutes(15), Duration.ofDays(7), false),
                new AppProperties.Cors(List.of()),
                new AppProperties.Ai(aiKey, "https://api.anthropic.com", "test-model", 1024, Duration.ofSeconds(5),
                        0.8, 40),
                new AppProperties.Assistant(1000, 20, 4, 10),
                new AppProperties.Imports(5_242_880, 20_000));
    }

    public static <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static User user() {
        return withId(new User("anna@example.com", "hash", "Anna", Role.USER, "2026-11", NOW), USER_ID);
    }

    public static Category category(String name, CategoryKind kind) {
        return withId(new Category(null, name.toLowerCase().replace(' ', '_'), name, "#123456", kind),
                UUID.randomUUID());
    }

    public static Category expenseCategory(String name) {
        return category(name, CategoryKind.EXPENSE);
    }

    public static Category userCategory(UUID userId, String name) {
        return withId(new Category(userId, null, name, "#654321", CategoryKind.EXPENSE), UUID.randomUUID());
    }

    public static Transaction transaction(String date, String amount, String merchant) {
        return transaction(USER_ID, date, amount, "EUR", merchant);
    }

    public static Transaction transaction(UUID userId, String date, String amount, String currency,
                                          String merchant) {
        return withId(new Transaction(userId, null, LocalDate.parse(date), new BigDecimal(amount), currency,
                merchant + " 0425", merchant, UUID.randomUUID().toString(), NOW), UUID.randomUUID());
    }
}
