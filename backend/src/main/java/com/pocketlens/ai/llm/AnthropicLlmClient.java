package com.pocketlens.ai.llm;

import com.pocketlens.common.config.AppProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client of the Anthropic Messages API (Claude) with tool use. Retries once on timeouts, rate limits and server
 * errors, then reports the provider as unavailable. Message contents are never logged.
 */
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);
    private static final String API_VERSION = "2023-06-01";
    private static final int MAX_ATTEMPTS = 2;
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String model;

    public AnthropicLlmClient(AppProperties.Ai settings) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(settings.timeout());
        requestFactory.setReadTimeout(settings.timeout());
        this.restClient = RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", settings.apiKey())
                .defaultHeader("anthropic-version", API_VERSION)
                .build();
        this.model = settings.model();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LlmResponse send(LlmRequest request) {
        Map<String, Object> body = toRequestBody(request);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long started = System.nanoTime();
            try {
                Map<String, Object> response = restClient.post()
                        .uri("/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JSON_OBJECT);
                LlmResponse parsed = parse(response);
                log.info("llm_call model={} attempt={} durationMs={} inputTokens={} outputTokens={} stopReason={}",
                        model, attempt, (System.nanoTime() - started) / 1_000_000,
                        parsed.inputTokens(), parsed.outputTokens(), parsed.stopReason());
                return parsed;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                log.warn("llm_call_failed attempt={} status={} retryable={}", attempt, status, retryable);
                if (!retryable) {
                    throw new AiUnavailableException("The AI provider rejected the request (HTTP " + status + ")");
                }
            } catch (ResourceAccessException e) {
                log.warn("llm_call_failed attempt={} reason=timeout_or_network", attempt);
            }
        }
        throw new AiUnavailableException("The assistant is temporarily unavailable. Please try again later.");
    }

    Map<String, Object> toRequestBody(LlmRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", request.maxTokens());
        if (request.system() != null) {
            body.put("system", request.system());
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        for (LlmMessage message : request.messages()) {
            messages.add(Map.of(
                    "role", message.role() == LlmMessage.Role.USER ? "user" : "assistant",
                    "content", message.content().stream().map(AnthropicLlmClient::toContent).toList()));
        }
        body.put("messages", messages);
        if (!request.tools().isEmpty()) {
            body.put("tools", request.tools().stream()
                    .map(t -> Map.of("name", t.name(), "description", t.description(), "input_schema",
                            t.inputSchema()))
                    .toList());
            if (request.forcedTool() != null) {
                body.put("tool_choice", Map.of("type", "tool", "name", request.forcedTool()));
            }
        }
        return body;
    }

    private static Map<String, Object> toContent(LlmContent block) {
        Map<String, Object> node = new LinkedHashMap<>();
        switch (block) {
            case LlmContent.Text text -> {
                node.put("type", "text");
                node.put("text", text.text());
            }
            case LlmContent.ToolUse use -> {
                node.put("type", "tool_use");
                node.put("id", use.id());
                node.put("name", use.name());
                node.put("input", use.input());
            }
            case LlmContent.ToolResult result -> {
                node.put("type", "tool_result");
                node.put("tool_use_id", result.toolUseId());
                node.put("content", result.content());
                if (result.isError()) {
                    node.put("is_error", true);
                }
            }
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    LlmResponse parse(Map<String, Object> response) {
        if (response == null) {
            throw new AiUnavailableException("Empty response from the AI provider");
        }
        List<LlmContent> content = new ArrayList<>();
        if (response.get("content") instanceof List<?> blocks) {
            for (Object item : blocks) {
                if (!(item instanceof Map<?, ?> block)) {
                    continue;
                }
                Object type = block.get("type");
                if ("text".equals(type)) {
                    content.add(new LlmContent.Text(String.valueOf(block.get("text"))));
                } else if ("tool_use".equals(type)) {
                    Map<String, Object> input = block.get("input") instanceof Map<?, ?> map
                            ? (Map<String, Object>) map
                            : Map.of();
                    content.add(new LlmContent.ToolUse(String.valueOf(block.get("id")),
                            String.valueOf(block.get("name")), input));
                }
            }
        }
        Map<?, ?> usage = response.get("usage") instanceof Map<?, ?> map ? map : Map.of();
        Object stopReason = response.get("stop_reason");
        return new LlmResponse(content, stopReason == null ? null : stopReason.toString(),
                number(usage.get("input_tokens")), number(usage.get("output_tokens")));
    }

    private static int number(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }
}
