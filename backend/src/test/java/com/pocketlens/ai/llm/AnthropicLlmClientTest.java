package com.pocketlens.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocketlens.support.TestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnthropicLlmClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AnthropicLlmClient client = new AnthropicLlmClient(TestData.properties("key").ai(), mapper);

    @Test
    void buildsMessagesApiRequestWithToolsAndToolResults() {
        LlmRequest request = new LlmRequest("system", List.of(
                LlmMessage.user("question"),
                new LlmMessage(LlmMessage.Role.ASSISTANT, List.of(new LlmContent.ToolUse("t1", "tool", Map.of("a", 1)))),
                new LlmMessage(LlmMessage.Role.USER, List.of(new LlmContent.ToolResult("t1", "{\"x\":1}", true)))),
                List.of(new LlmTool("tool", "desc", Map.of("type", "object"))), "tool", 256);

        ObjectNode body = client.toRequestBody(request);

        assertThat(body.path("model").asText()).isEqualTo("test-model");
        assertThat(body.path("system").asText()).isEqualTo("system");
        assertThat(body.path("messages")).hasSize(3);
        assertThat(body.path("messages").get(1).path("content").get(0).path("type").asText()).isEqualTo("tool_use");
        JsonNode toolResult = body.path("messages").get(2).path("content").get(0);
        assertThat(toolResult.path("tool_use_id").asText()).isEqualTo("t1");
        assertThat(toolResult.path("is_error").asBoolean()).isTrue();
        assertThat(body.path("tools").get(0).path("input_schema").path("type").asText()).isEqualTo("object");
        assertThat(body.path("tool_choice").path("name").asText()).isEqualTo("tool");
    }

    @Test
    void parsesTextToolUseAndUsage() throws Exception {
        JsonNode response = mapper.readTree("""
                {"content":[{"type":"text","text":"Hi"},{"type":"tool_use","id":"t1","name":"get","input":{"a":1}}],
                 "stop_reason":"tool_use","usage":{"input_tokens":10,"output_tokens":3}}""");

        LlmResponse parsed = client.parse(response);

        assertThat(parsed.text()).isEqualTo("Hi");
        assertThat(parsed.wantsTools()).isTrue();
        assertThat(parsed.toolUses()).singleElement().satisfies(u -> assertThat(u.input()).containsEntry("a", 1));
        assertThat(parsed.inputTokens()).isEqualTo(10);
        assertThatThrownBy(() -> client.parse(null)).isInstanceOf(AiUnavailableException.class);
        assertThat(client.isAvailable()).isTrue();
    }

    @Test
    void disabledClientReportsUnavailable() {
        DisabledLlmClient disabled = new DisabledLlmClient();
        assertThat(disabled.isAvailable()).isFalse();
        assertThatThrownBy(() -> disabled.send(null)).isInstanceOf(AiUnavailableException.class);
    }
}
