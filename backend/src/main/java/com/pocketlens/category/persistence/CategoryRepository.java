package com.pocketlens.category.persistence;

import com.pocketlens.category.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** Placeholder id for "exclude nothing" in the name checks. */
    UUID NO_ID = new UUID(0L, 0L);

    /** Default categories plus the user's own categories. */
    @Query("select c from Category c where c.userId is null or c.userId = :userId order by c.name")
    List<Category> findVisibleTo(UUID userId);

    List<Category> findByUserIdIsNullOrderByName();

    Optional<Category> findBySystemKey(String systemKey);

    @Query("select count(c) > 0 from Category c where (c.userId is null or c.userId = :userId) "
            + "and lower(c.name) = lower(:name) and c.id <> :excludeId")
    boolean nameTaken(UUID userId, String name, UUID excludeId);

    @Query("select count(c) > 0 from Category c where c.userId is null "
            + "and lower(c.name) = lower(:name) and c.id <> :excludeId")
    boolean defaultNameTaken(String name, UUID excludeId);
}
