package com.pocketlens.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards the accuracy of the API documentation: every operation implemented by the controllers is documented in
 * docs/reference/openapi.yaml and every documented operation exists.
 */
class OpenApiContractIT extends IntegrationTest {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Test
    void documentedOperationsMatchImplementedOperations() throws Exception {
        Map<String, Object> generated = body(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn())
                .read("$");
        Map<String, Object> documented;
        try (Reader reader = Files.newBufferedReader(Path.of("..", "docs", "reference", "openapi.yaml"))) {
            documented = new Yaml().load(reader);
        }

        assertThat(operations(documented)).isNotEmpty().isEqualTo(operations(generated));
    }

    @Test
    void swaggerUiServesTheContract() throws Exception {
        mvc.perform(get("/openapi.yaml")).andExpect(status().isOk());
    }

    @SuppressWarnings("unchecked")
    private static Set<String> operations(Map<String, Object> spec) {
        Set<String> result = new TreeSet<>();
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        paths.forEach((path, item) -> ((Map<String, Object>) item).keySet().forEach(method -> {
            if (HTTP_METHODS.contains(method)) {
                result.add(method.toUpperCase(Locale.ROOT) + " " + path);
            }
        }));
        return result;
    }
}
