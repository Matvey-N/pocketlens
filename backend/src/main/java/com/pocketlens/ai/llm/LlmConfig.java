package com.pocketlens.ai.llm;

import com.pocketlens.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the LLM client: Anthropic when an API key is configured, otherwise a disabled client.
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public LlmClient llmClient(AppProperties properties) {
        if (properties.ai().enabled()) {
            log.info("AI features enabled, model={}", properties.ai().model());
            return new AnthropicLlmClient(properties.ai());
        }
        log.warn("AI_API_KEY is not set: AI categorisation and the assistant are disabled");
        return new DisabledLlmClient();
    }
}
