package com.pocketlens.ai.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocketlens.common.config.AppProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public AnthropicLlmClient(AppProperties.Ai settings, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) settings.timeout().toMillis());
        requestFactory.setReadTimeout((int) settings.timeout().toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", settings.apiKey())
                .defaultHeader("anthropic-version", API_VERSION)
                .build();
        this.objectMapper = objectMapper;
        this.model = settings.model();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LlmResponse send(LlmRequest request) {
        ObjectNode body = toRequestBody(request);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long started = System.nanoTime();
            try {
                JsonNode response = restClient.post()
                        .uri("/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
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

    ObjectNode toRequestBody(LlmRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", request.maxTokens());
        if (request.system() != null) {
            body.put("system", request.system());
        }
        ArrayNode messages = body.putArray("messages");
        for (LlmMessage message : request.messages()) {
            ObjectNode node = messages.addObject();
            node.put("role", message.role() == LlmMessage.Role.USER ? "user" : "assistant");
            ArrayNode content = node.putArray("content");
            message.content().forEach(block -> content.add(toContentNode(block)));
        }
        if (!request.tools().isEmpty()) {
            ArrayNode tools = body.putArray("tools");
            for (LlmTool tool : request.tools()) {
                ObjectNode node = tools.addObject();
                node.put("name", tool.name());
                node.put("description", tool.description());
                node.set("input_schema", objectMapper.valueToTree(tool.inputSchema()));
            }
            if (request.forcedTool() != null) {
                ObjectNode choice = body.putObject("tool_choice");
                choice.put("type", "tool");
                choice.put("name", request.forcedTool());
            }
        }
        return body;
    }

    private ObjectNode toContentNode(LlmContent block) {
        ObjectNode node = objectMapper.createObjectNode();
        switch (block) {
            case LlmContent.Text text -> {
                node.put("type", "text");
                node.put("text", text.text());
            }
            case LlmContent.ToolUse use -> {
                node.put("type", "tool_use");
                node.put("id", use.id());
                node.put("name", use.name());
                node.set("input", objectMapper.valueToTree(use.input()));
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

    LlmResponse parse(JsonNode response) {
        if (response == null) {
            throw new AiUnavailableException("Empty response from the AI provider");
        }
        List<LlmContent> content = new ArrayList<>();
        for (JsonNode block : response.path("content")) {
            String type = block.path("type").asText();
            if ("text".equals(type)) {
                content.add(new LlmContent.Text(block.path("text").asText()));
            } else if ("tool_use".equals(type)) {
                Map<String, Object> input = objectMapper.convertValue(block.path("input"), MAP_TYPE);
                content.add(new LlmContent.ToolUse(block.path("id").asText(), block.path("name").asText(), input));
            }
        }
        JsonNode usage = response.path("usage");
        return new LlmResponse(content, response.path("stop_reason").asText(null),
                usage.path("input_tokens").asInt(0), usage.path("output_tokens").asInt(0));
    }
}
