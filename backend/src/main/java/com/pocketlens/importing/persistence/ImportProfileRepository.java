package com.pocketlens.importing.persistence;

import com.pocketlens.importing.domain.ImportProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportProfileRepository extends JpaRepository<ImportProfile, UUID> {

    List<ImportProfile> findByUserIdOrderByName(UUID userId);

    Optional<ImportProfile> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
