package com.pocketlens.categorization.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record GlobalRuleRequest(@NotBlank @Size(min = 2, max = 120) String pattern, @NotNull UUID categoryId) {
}
