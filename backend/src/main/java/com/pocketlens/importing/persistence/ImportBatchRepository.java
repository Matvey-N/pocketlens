package com.pocketlens.importing.persistence;

import com.pocketlens.importing.domain.ImportBatch;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {

    List<ImportBatch> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}
