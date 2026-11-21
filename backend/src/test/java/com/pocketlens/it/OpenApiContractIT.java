package com.pocketlens.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Guards the accuracy of the API documentation: every operation implemented by the controllers is documented in
 * docs/reference/openapi.yaml and every documented operation exists.
 */
class OpenApiContractIT extends IntegrationTest {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Test
    void documentedOperationsMatchImplementedOperations() throws Exception {
        JsonNode generated = body(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());
        JsonNode documented = new ObjectMapper(new YAMLFactory())
                .readTree(Path.of("..", "docs", "reference", "openapi.yaml").toFile());

        assertThat(operations(documented)).isEqualTo(operations(generated));
    }

    @Test
    void swaggerUiServesTheContract() throws Exception {
        mvc.perform(get("/openapi.yaml")).andExpect(status().isOk());
    }

    private static Set<String> operations(JsonNode spec) {
        Set<String> result = new TreeSet<>();
        Iterator<Map.Entry<String, JsonNode>> paths = spec.path("paths").fields();
        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> path = paths.next();
            path.getValue().fieldNames().forEachRemaining(method -> {
                if (HTTP_METHODS.contains(method)) {
                    result.add(method.toUpperCase(Locale.ROOT) + " " + path.getKey());
                }
            });
        }
        return result;
    }
}
