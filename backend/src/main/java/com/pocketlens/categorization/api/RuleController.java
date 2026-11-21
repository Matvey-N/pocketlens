package com.pocketlens.categorization.api;

import com.pocketlens.categorization.api.dto.RuleResponse;
import com.pocketlens.categorization.service.RuleService;
import com.pocketlens.common.security.CurrentUserId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user's own merchant rules (created from corrections).
 */
@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public List<RuleResponse> list(@CurrentUserId UUID userId) {
        return ruleService.listUserRules(userId).stream().map(RuleResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        ruleService.deleteUserRule(userId, id);
        return ResponseEntity.noContent().build();
    }
}
