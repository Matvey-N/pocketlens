package com.pocketlens.assistant.service.tool;

import com.pocketlens.ai.llm.LlmTool;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * All tools available to the assistant.
 */
@Component
public class ToolRegistry {

    private final Map<String, AssistantTool> tools;

    public ToolRegistry(List<AssistantTool> tools) {
        this.tools = tools.stream().collect(Collectors.toMap(AssistantTool::name, Function.identity()));
    }

    public Optional<AssistantTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<LlmTool> definitions() {
        return tools.values().stream()
                .sorted(java.util.Comparator.comparing(AssistantTool::name))
                .map(t -> new LlmTool(t.name(), t.description(), t.inputSchema()))
                .toList();
    }
}
