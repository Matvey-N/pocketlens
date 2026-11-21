package com.pocketlens.budget.api;

import com.pocketlens.budget.api.dto.BudgetLimitRequest;
import com.pocketlens.budget.api.dto.BudgetRequest;
import com.pocketlens.budget.api.dto.BudgetResponse;
import com.pocketlens.budget.domain.Budget;
import com.pocketlens.budget.service.BudgetService;
import com.pocketlens.budget.service.BudgetStatus;
import com.pocketlens.common.security.CurrentUserId;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Monthly budgets (US-3.1, US-3.2).
 */
@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final Clock clock;

    public BudgetController(BudgetService budgetService, Clock clock) {
        this.budgetService = budgetService;
        this.clock = clock;
    }

    /** Budgets with spending of the month (default: current month). */
    @GetMapping
    public List<BudgetStatus> list(@CurrentUserId UUID userId,
                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgetService.statuses(userId, month == null ? YearMonth.now(clock) : month);
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@CurrentUserId UUID userId, @Valid @RequestBody BudgetRequest request) {
        Budget budget = budgetService.create(userId, request.categoryId(), request.monthlyLimit(), request.currency());
        return ResponseEntity.created(URI.create("/api/v1/budgets/" + budget.getId())).body(BudgetResponse.from(budget));
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@CurrentUserId UUID userId, @PathVariable UUID id,
                                 @Valid @RequestBody BudgetLimitRequest request) {
        return BudgetResponse.from(budgetService.updateLimit(userId, id, request.monthlyLimit()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        budgetService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
