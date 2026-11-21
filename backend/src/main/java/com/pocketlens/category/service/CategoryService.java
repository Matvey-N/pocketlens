package com.pocketlens.category.service;

import com.pocketlens.category.domain.Category;
import com.pocketlens.category.persistence.CategoryRepository;
import com.pocketlens.common.config.CacheConfig;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default and custom categories (US-2.4, US-7.1).
 */
@Service
public class CategoryService {

    private static final String RESOURCE = "Category";

    private final CategoryRepository categories;
    private final List<CategoryUsageCleaner> usageCleaners;

    public CategoryService(CategoryRepository categories, List<CategoryUsageCleaner> usageCleaners) {
        this.categories = categories;
        this.usageCleaners = List.copyOf(usageCleaners);
    }

    @Transactional(readOnly = true)
    public List<Category> listVisible(UUID userId) {
        return categories.findVisibleTo(userId);
    }

    /** Returns a category the user may use (default or own) or throws 404. */
    @Transactional(readOnly = true)
    public Category getVisible(UUID userId, UUID categoryId) {
        return categories.findById(categoryId)
                .filter(c -> c.isVisibleTo(userId))
                .orElseThrow(() -> new NotFoundException(RESOURCE, categoryId));
    }

    @Transactional(readOnly = true)
    public Category getBySystemKey(String systemKey) {
        return categories.findBySystemKey(systemKey)
                .orElseThrow(() -> new IllegalStateException("Default category missing: " + systemKey));
    }

    @Transactional
    public Category create(UUID userId, CategoryCommand command) {
        String name = command.name().strip();
        if (categories.nameTaken(userId, name, CategoryRepository.NO_ID)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        return categories.save(new Category(userId, null, name, command.color(), command.kind()));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public Category update(UUID userId, UUID categoryId, CategoryCommand command) {
        Category category = getOwned(userId, categoryId);
        String name = command.name().strip();
        if (categories.nameTaken(userId, name, categoryId)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        category.update(name, command.color(), command.kind());
        return category;
    }

    /** Deletes a custom category; its transactions return to review, its rules and budgets are removed. */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public void delete(UUID userId, UUID categoryId) {
        Category category = getOwned(userId, categoryId);
        usageCleaners.forEach(cleaner -> cleaner.releaseCategory(category.getId()));
        categories.delete(category);
    }

    // --- Administration of default categories (US-7.1) ---

    @Transactional(readOnly = true)
    public List<Category> listDefaults() {
        return categories.findByUserIdIsNullOrderByName();
    }

    @Transactional
    public Category createDefault(CategoryCommand command) {
        String name = command.name().strip();
        if (categories.defaultNameTaken(name, CategoryRepository.NO_ID)) {
            throw new ConflictException("A default category named '" + name + "' already exists");
        }
        return categories.save(new Category(null, null, name, command.color(), command.kind()));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD_CACHE, allEntries = true)
    public Category updateDefault(UUID categoryId, CategoryCommand command) {
        Category category = categories.findById(categoryId)
                .filter(Category::isDefault)
                .orElseThrow(() -> new NotFoundException(RESOURCE, categoryId));
        String name = command.name().strip();
        if (categories.defaultNameTaken(name, categoryId)) {
            throw new ConflictException("A default category named '" + name + "' already exists");
        }
        category.update(name, command.color(), command.kind());
        return category;
    }

    private Category getOwned(UUID userId, UUID categoryId) {
        return categories.findById(categoryId)
                .filter(c -> userId.equals(c.getUserId()))
                .orElseThrow(() -> new NotFoundException(RESOURCE, categoryId));
    }
}
