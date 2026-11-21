package com.pocketlens.admin.api;

import com.pocketlens.admin.service.AdminStats;
import com.pocketlens.admin.service.AdminStatsService;
import com.pocketlens.categorization.api.dto.GlobalRuleRequest;
import com.pocketlens.categorization.api.dto.RuleResponse;
import com.pocketlens.categorization.domain.MerchantRule;
import com.pocketlens.categorization.service.RuleService;
import com.pocketlens.category.api.dto.CategoryRequest;
import com.pocketlens.category.api.dto.CategoryResponse;
import com.pocketlens.category.domain.Category;
import com.pocketlens.category.service.CategoryService;
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

/**
 * Administration of default categories and global merchant rules (US-7.1) and usage statistics (US-7.2).
 * Requires the ADMIN role (enforced in the security configuration).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final CategoryService categoryService;
    private final RuleService ruleService;
    private final AdminStatsService statsService;

    public AdminController(CategoryService categoryService, RuleService ruleService, AdminStatsService statsService) {
        this.categoryService = categoryService;
        this.ruleService = ruleService;
        this.statsService = statsService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryService.listDefaults().stream().map(CategoryResponse::from).toList();
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.createDefault(request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + category.getId()))
                .body(CategoryResponse.from(category));
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return CategoryResponse.from(categoryService.updateDefault(id, request.toCommand()));
    }

    @GetMapping("/rules")
    public List<RuleResponse> rules() {
        return ruleService.listGlobalRules().stream().map(RuleResponse::from).toList();
    }

    @PostMapping("/rules")
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody GlobalRuleRequest request) {
        MerchantRule rule = ruleService.createGlobalRule(request.pattern(), request.categoryId());
        return ResponseEntity.created(URI.create("/api/v1/admin/rules/" + rule.getId())).body(RuleResponse.from(rule));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        ruleService.deleteGlobalRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return statsService.stats();
    }
}
