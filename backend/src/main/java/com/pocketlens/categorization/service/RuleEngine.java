package com.pocketlens.categorization.service;

import com.pocketlens.categorization.domain.MerchantRule;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Finds the rule for a merchant. User rules win over global rules; among rules of the same owner the longest
 * (most specific) pattern wins.
 */
@Component
public class RuleEngine {

    private static final Comparator<MerchantRule> PRIORITY = Comparator
            .comparing(MerchantRule::isGlobal)
            .thenComparing(r -> -r.getPattern().length())
            .thenComparing(MerchantRule::getPattern);

    public Optional<MerchantRule> match(String merchant, List<MerchantRule> rules) {
        return rules.stream()
                .filter(r -> r.matches(merchant))
                .min(PRIORITY);
    }
}
