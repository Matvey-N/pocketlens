package com.pocketlens.categorization.service;

import static com.pocketlens.support.TestData.NOW;
import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocketlens.categorization.domain.MerchantRule;
import com.pocketlens.category.domain.Category;
import com.pocketlens.support.TestData;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();
    private final Category groceries = TestData.expenseCategory("Groceries");
    private final Category eatingOut = TestData.expenseCategory("Eating out");
    private final Category transport = TestData.expenseCategory("Transport");

    @Test
    void userRuleWinsOverGlobalRule() {
        List<MerchantRule> rules = List.of(
                new MerchantRule(null, "LIDL", groceries, NOW),
                new MerchantRule(USER_ID, "LIDL", eatingOut, NOW));
        assertThat(engine.match("LIDL VILNIUS", rules)).get()
                .extracting(MerchantRule::getCategory).isEqualTo(eatingOut);
    }

    @Test
    void longestPatternWins() {
        List<MerchantRule> rules = List.of(
                new MerchantRule(null, "BOLT", transport, NOW),
                new MerchantRule(null, "BOLT FOOD", eatingOut, NOW));
        assertThat(engine.match("BOLT FOOD ORDER", rules)).get()
                .extracting(MerchantRule::getCategory).isEqualTo(eatingOut);
        assertThat(engine.match("BOLT RIDE", rules)).get()
                .extracting(MerchantRule::getCategory).isEqualTo(transport);
    }

    @Test
    void patternMustStartAWord() {
        List<MerchantRule> rules = List.of(new MerchantRule(null, "IKI", groceries, NOW));
        assertThat(engine.match("IKI EXPRESS", rules)).isPresent();
        assertThat(engine.match("SHOP IKI", rules)).isPresent();
        assertThat(engine.match("WIKIPEDIA", rules)).isEmpty();
        assertThat(engine.match(null, rules)).isEmpty();
    }
}
