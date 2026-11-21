package com.pocketlens.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pocketlens.ai.domain.AiUsageRecord;
import com.pocketlens.ai.llm.LlmResponse;
import com.pocketlens.ai.persistence.AiUsageRepository;
import com.pocketlens.support.TestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiUsageServiceTest {

    @Test
    void recordsTokenUsageWithoutContent() {
        AiUsageRepository repository = mock(AiUsageRepository.class);
        new AiUsageService(repository, TestData.clock()).record(TestData.USER_ID, AiUsageRecord.Purpose.ASSISTANT,
                new LlmResponse(List.of(), "end_turn", 120, 30));

        ArgumentCaptor<AiUsageRecord> saved = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getInputTokens()).isEqualTo(120);
        assertThat(saved.getValue().getOutputTokens()).isEqualTo(30);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(TestData.NOW);
    }
}
