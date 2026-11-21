package com.pocketlens.assistant.service.tool;

import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resolves a category name used by the model to one of the user's categories.
 */
@Component
public class CategoryResolver {

    static final String UNCATEGORISED = "uncategorised";

    private final CategoryService categoryService;

    public CategoryResolver(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * @return empty for "no filter"; a {@link Resolved} with {@code category == null} means "uncategorised"
     */
    public Optional<Resolved> resolve(UUID userId, String name) throws ToolInputException {
        if (name == null) {
            return Optional.empty();
        }
        String wanted = name.strip().toLowerCase(Locale.ROOT);
        if (UNCATEGORISED.equals(wanted) || "uncategorized".equals(wanted)) {
            return Optional.of(new Resolved(null));
        }
        var categories = categoryService.listVisible(userId);
        return Optional.of(categories.stream()
                .filter(c -> c.getName().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst()
                .map(Resolved::new)
                .orElseThrow(() -> new ToolInputException("Unknown category '" + name + "'. Available: "
                        + categories.stream().map(Category::getName).collect(Collectors.joining(", ")))));
    }

    /** A resolved category filter. */
    public record Resolved(Category category) {
        public String label() {
            return category == null ? "Uncategorised" : category.getName();
        }
    }
}
