package com.pocketlens.ai.service;

import com.pocketlens.ai.domain.AiUsageRecord;
import com.pocketlens.ai.llm.LlmResponse;
import com.pocketlens.ai.persistence.AiUsageRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Records token usage of LLM calls.
 */
@Service
public class AiUsageService {

    private final AiUsageRepository repository;
    private final Clock clock;

    public AiUsageService(AiUsageRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(UUID userId, AiUsageRecord.Purpose purpose, LlmResponse response) {
        repository.save(new AiUsageRecord(userId, purpose, response.inputTokens(), response.outputTokens(),
                clock.instant()));
    }
}
