package com.pocketlens.budget.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A warning issued for a budget on a day (at most one per budget and day, US-3.2). Kept to measure KPI K3:
 * how many days before the actual overrun the user was warned.
 */
@Entity
@Table(name = "budget_warnings")
public class BudgetWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Column(name = "warned_on", nullable = false)
    private LocalDate warnedOn;

    @Column(name = "projected_overrun_date")
    private LocalDate projectedOverrunDate;

    protected BudgetWarning() {
    }

    public BudgetWarning(UUID budgetId, LocalDate warnedOn, LocalDate projectedOverrunDate) {
        this.budgetId = budgetId;
        this.warnedOn = warnedOn;
        this.projectedOverrunDate = projectedOverrunDate;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBudgetId() {
        return budgetId;
    }

    public LocalDate getWarnedOn() {
        return warnedOn;
    }

    public LocalDate getProjectedOverrunDate() {
        return projectedOverrunDate;
    }
}
