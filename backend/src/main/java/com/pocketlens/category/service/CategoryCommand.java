package com.pocketlens.category.service;

import com.pocketlens.category.domain.CategoryKind;

/**
 * Input for creating or updating a category.
 */
public record CategoryCommand(String name, String color, CategoryKind kind) {
}
