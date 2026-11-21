package com.pocketlens.category.api.dto;

import com.pocketlens.category.domain.Category;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String color, String kind, boolean isDefault, String systemKey) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor(),
                category.getKind().name(), category.isDefault(), category.getSystemKey());
    }
}
