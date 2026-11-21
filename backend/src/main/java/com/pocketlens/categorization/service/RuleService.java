package com.pocketlens.categorization.service;

import com.pocketlens.categorization.domain.MerchantRule;
import com.pocketlens.categorization.persistence.MerchantRuleRepository;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User rules learned from corrections (US-2.3) and global rules managed by administrators (US-7.1).
 */
@Service
public class RuleService {

    private static final String RESOURCE = "Rule";

    private final MerchantRuleRepository rules;
    private final MerchantNormalizer normalizer;
    private final CategoryService categoryService;
    private final Clock clock;

    public RuleService(MerchantRuleRepository rules, MerchantNormalizer normalizer, CategoryService categoryService,
                       Clock clock) {
        this.rules = rules;
        this.normalizer = normalizer;
        this.categoryService = categoryService;
        this.clock = clock;
    }

    /** Creates or updates the user's rule for a merchant key. */
    @Transactional
    public MerchantRule upsertUserRule(UUID userId, String merchant, Category category) {
        return rules.findByUserIdAndPattern(userId, merchant)
                .map(rule -> {
                    rule.changeCategory(category);
                    return rule;
                })
                .orElseGet(() -> rules.save(new MerchantRule(userId, merchant, category, clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<MerchantRule> listUserRules(UUID userId) {
        return rules.findByUserIdOrderByPattern(userId);
    }

    @Transactional
    public void deleteUserRule(UUID userId, UUID ruleId) {
        MerchantRule rule = rules.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new NotFoundException(RESOURCE, ruleId));
        rules.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<MerchantRule> listGlobalRules() {
        return rules.findByUserIdIsNullOrderByPattern();
    }

    @Transactional
    public MerchantRule createGlobalRule(String pattern, UUID categoryId) {
        String key = normalizer.normalize(pattern);
        if (rules.findByUserIdIsNullAndPattern(key).isPresent()) {
            throw new ConflictException("A global rule for '" + key + "' already exists");
        }
        Category category = categoryService.listDefaults().stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Default category", categoryId));
        return rules.save(new MerchantRule(null, key, category, clock.instant()));
    }

    @Transactional
    public void deleteGlobalRule(UUID ruleId) {
        MerchantRule rule = rules.findById(ruleId)
                .filter(MerchantRule::isGlobal)
                .orElseThrow(() -> new NotFoundException(RESOURCE, ruleId));
        rules.delete(rule);
    }
}
