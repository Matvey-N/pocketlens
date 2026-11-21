package com.pocketlens.category.api.dto;

import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.category.service.CategoryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 40) String name,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex colour like #4F7CAC") String color,
        @NotNull CategoryKind kind) {

    public CategoryCommand toCommand() {
        return new CategoryCommand(name, color, kind);
    }
}
