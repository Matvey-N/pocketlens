package com.pocketlens.categorization.service;

import com.pocketlens.categorization.persistence.MerchantRuleRepository;
import com.pocketlens.category.service.CategoryUsageCleaner;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Deletes the merchant rules that point to a category that is being deleted.
 */
@Component
public class RuleCategoryCleaner implements CategoryUsageCleaner {

    private final MerchantRuleRepository rules;

    public RuleCategoryCleaner(MerchantRuleRepository rules) {
        this.rules = rules;
    }

    @Override
    public void releaseCategory(UUID categoryId) {
        rules.deleteByCategoryId(categoryId);
    }
}
