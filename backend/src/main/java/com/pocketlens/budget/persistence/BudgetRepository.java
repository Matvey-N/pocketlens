package com.pocketlens.budget.persistence;

import com.pocketlens.budget.domain.Budget;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @Query("select b from Budget b join fetch b.category where b.userId = :userId order by b.category.name")
    List<Budget> findByUserId(UUID userId);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndCategoryIdAndCurrency(UUID userId, UUID categoryId, String currency);

    @Modifying
    @Query("delete from Budget b where b.category.id = :categoryId")
    int deleteByCategoryId(UUID categoryId);
}
