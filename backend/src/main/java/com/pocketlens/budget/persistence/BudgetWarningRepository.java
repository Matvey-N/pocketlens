package com.pocketlens.budget.persistence;

import com.pocketlens.budget.domain.BudgetWarning;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetWarningRepository extends JpaRepository<BudgetWarning, UUID> {

    boolean existsByBudgetIdAndWarnedOn(UUID budgetId, LocalDate warnedOn);
}
