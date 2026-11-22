package com.pocketlens.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pocketlens.support.TestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnthropicLlmClientTest {

    private final AnthropicLlmClient client = new AnthropicLlmClient(TestData.properties("key").ai());

    @Test
    @SuppressWarnings("unchecked")
    void buildsMessagesApiRequestWithToolsAndToolResults() {
        LlmRequest request = new LlmRequest("system", List.of(
                LlmMessage.user("question"),
                new LlmMessage(LlmMessage.Role.ASSISTANT, List.of(new LlmContent.ToolUse("t1", "tool", Map.of("a", 1)))),
                new LlmMessage(LlmMessage.Role.USER, List.of(new LlmContent.ToolResult("t1", "{\"x\":1}", true)))),
                List.of(new LlmTool("tool", "desc", Map.of("type", "object"))), "tool", 256);

        Map<String, Object> body = client.toRequestBody(request);

        assertThat(body).containsEntry("model", "test-model").containsEntry("system", "system");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        assertThat(messages).hasSize(3);
        Map<String, Object> toolUse = ((List<Map<String, Object>>) messages.get(1).get("content")).get(0);
        assertThat(toolUse).containsEntry("type", "tool_use");
        Map<String, Object> toolResult = ((List<Map<String, Object>>) messages.get(2).get("content")).get(0);
        assertThat(toolResult).containsEntry("tool_use_id", "t1").containsEntry("is_error", true);
        Map<String, Object> tool = ((List<Map<String, Object>>) body.get("tools")).get(0);
        assertThat(tool.get("input_schema")).isEqualTo(Map.of("type", "object"));
        assertThat(body.get("tool_choice")).isEqualTo(Map.of("type", "tool", "name", "tool"));
    }

    @Test
    void parsesTextToolUseAndUsage() {
        Map<String, Object> response = Map.of(
                "content", List.of(Map.of("type", "text", "text", "Hi"),
                        Map.of("type", "tool_use", "id", "t1", "name", "get", "input", Map.of("a", 1)), "junk"),
                "stop_reason", "tool_use",
                "usage", Map.of("input_tokens", 10, "output_tokens", 3));

        LlmResponse parsed = client.parse(response);

        assertThat(parsed.text()).isEqualTo("Hi");
        assertThat(parsed.wantsTools()).isTrue();
        assertThat(parsed.toolUses()).singleElement().satisfies(u -> assertThat(u.input()).containsEntry("a", 1));
        assertThat(parsed.inputTokens()).isEqualTo(10);
        assertThat(client.parse(Map.of()).content()).isEmpty();
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
