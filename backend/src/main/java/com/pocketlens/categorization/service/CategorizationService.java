package com.pocketlens.categorization.service;

import com.pocketlens.ai.llm.AiUnavailableException;
import com.pocketlens.categorization.domain.CategorizationSource;
import com.pocketlens.categorization.domain.MerchantRule;
import com.pocketlens.categorization.persistence.MerchantRuleRepository;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryKeys;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.config.AppProperties;
import com.pocketlens.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Categorises transactions: merchant rules first (deterministic, free), then the AI classifier for unknown
 * merchants. AI suggestions below the confidence threshold are marked "Needs review" (US-2.1, US-2.2).
 */
@Service
public class CategorizationService {

    private static final Logger log = LoggerFactory.getLogger(CategorizationService.class);

    private final MerchantRuleRepository rules;
    private final RuleEngine ruleEngine;
    private final AiCategorizer aiCategorizer;
    private final CategoryService categoryService;
    private final BigDecimal threshold;

    public CategorizationService(MerchantRuleRepository rules, RuleEngine ruleEngine, AiCategorizer aiCategorizer,
                                 CategoryService categoryService, AppProperties properties) {
        this.rules = rules;
        this.ruleEngine = ruleEngine;
        this.aiCategorizer = aiCategorizer;
        this.categoryService = categoryService;
        this.threshold = BigDecimal.valueOf(properties.ai().confidenceThreshold());
    }

    /** Categorises the given (new or waiting) transactions of one user. Manual categories are never changed. */
    @Transactional
    public CategorizationOutcome categorize(UUID userId, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return CategorizationOutcome.empty();
        }
        List<MerchantRule> applicable = rules.findApplicableTo(userId);
        List<Transaction> unknown = new ArrayList<>();
        int byRules = 0;
        for (Transaction transaction : transactions) {
            if (transaction.isManuallyCategorized()) {
                continue;
            }
            Optional<MerchantRule> rule = ruleEngine.match(transaction.getMerchant(), applicable);
            if (rule.isPresent()) {
                transaction.categorize(rule.get().getCategory(), CategorizationSource.RULE, BigDecimal.ONE, false);
                byRules++;
            } else {
                unknown.add(transaction);
            }
        }
        if (unknown.isEmpty()) {
            return new CategorizationOutcome(byRules, 0, 0, 0, true);
        }
        if (!aiCategorizer.isAvailable()) {
            unknown.forEach(t -> t.categorize(null, CategorizationSource.NONE, null, true));
            return new CategorizationOutcome(byRules, 0, unknown.size(), unknown.size(), false);
        }
        return applyAi(userId, unknown, byRules);
    }

    private CategorizationOutcome applyAi(UUID userId, List<Transaction> unknown, int byRules) {
        Map<UUID, AiSuggestion> suggestions;
        try {
            suggestions = aiCategorizer.suggest(userId, unknown, categoryService.listVisible(userId));
        } catch (AiUnavailableException e) {
            log.warn("ai_categorization_unavailable userId={} pending={}", userId, unknown.size());
            unknown.forEach(t -> t.categorize(null, CategorizationSource.NONE, null, true));
            return new CategorizationOutcome(byRules, 0, unknown.size(), unknown.size(), false);
        }
        int byAi = 0;
        int needsReview = 0;
        int uncategorized = 0;
        for (Transaction transaction : unknown) {
            AiSuggestion suggestion = suggestions.get(transaction.getId());
            if (suggestion == null) {
                Category other = categoryService.getBySystemKey(CategoryKeys.OTHER);
                transaction.categorize(other, CategorizationSource.AI, BigDecimal.ZERO, true);
                needsReview++;
                uncategorized++;
                continue;
            }
            boolean review = suggestion.confidence().compareTo(threshold) < 0;
            transaction.categorize(suggestion.category(), CategorizationSource.AI, suggestion.confidence(), review);
            byAi++;
            if (review) {
                needsReview++;
            }
        }
        log.info("ai_categorization_done userId={} byAi={} needsReview={}", userId, byAi, needsReview);
        return new CategorizationOutcome(byRules, byAi, needsReview, uncategorized, true);
    }
}
