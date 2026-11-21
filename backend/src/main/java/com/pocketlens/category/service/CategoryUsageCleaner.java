package com.pocketlens.category.service;

import java.util.UUID;

/**
 * Releases everything that references a category before the category is deleted. Implemented by the modules
 * that own those references (Dependency Inversion: the category module does not depend on them).
 */
public interface CategoryUsageCleaner {

    void releaseCategory(UUID categoryId);
}
