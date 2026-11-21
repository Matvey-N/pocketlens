package com.pocketlens.ai.persistence;

import com.pocketlens.ai.domain.AiUsageRecord;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AiUsageRepository extends JpaRepository<AiUsageRecord, UUID> {

    long countByPurposeAndCreatedAtAfter(AiUsageRecord.Purpose purpose, Instant after);

    @Query("select coalesce(sum(r.inputTokens), 0) from AiUsageRecord r where r.createdAt > :after")
    long sumInputTokensSince(Instant after);

    @Query("select coalesce(sum(r.outputTokens), 0) from AiUsageRecord r where r.createdAt > :after")
    long sumOutputTokensSince(Instant after);
}
