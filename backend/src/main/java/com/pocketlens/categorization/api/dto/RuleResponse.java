package com.pocketlens.categorization.api.dto;

import com.pocketlens.categorization.domain.MerchantRule;
import java.time.Instant;
import java.util.UUID;

public record RuleResponse(UUID id, String pattern, UUID categoryId, String categoryName, boolean global,
                           Instant createdAt) {

    public static RuleResponse from(MerchantRule rule) {
        return new RuleResponse(rule.getId(), rule.getPattern(), rule.getCategory().getId(),
                rule.getCategory().getName(), rule.isGlobal(), rule.getCreatedAt());
    }
}
