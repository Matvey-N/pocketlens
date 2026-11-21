package com.pocketlens.category.api;

import com.pocketlens.category.api.dto.CategoryRequest;
import com.pocketlens.category.api.dto.CategoryResponse;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
import com.pocketlens.common.security.CurrentUserId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list(@CurrentUserId UUID userId) {
        return categoryService.listVisible(userId).stream().map(CategoryResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@CurrentUserId UUID userId,
                                                   @Valid @RequestBody CategoryRequest request) {
        Category created = categoryService.create(userId, request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/categories/" + created.getId()))
                .body(CategoryResponse.from(created));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@CurrentUserId UUID userId, @PathVariable UUID id,
                                   @Valid @RequestBody CategoryRequest request) {
        return CategoryResponse.from(categoryService.update(userId, id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        categoryService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
