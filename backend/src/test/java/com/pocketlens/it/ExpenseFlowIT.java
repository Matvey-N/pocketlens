package com.pocketlens.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Main user journey: import → de-duplication → categorisation by rules → correction with a rule → budget →
 * dashboard → subscriptions → export → account deletion.
 */
class ExpenseFlowIT extends IntegrationTest {

    @Test
    void fullJourney() throws Exception {
        Session session = register();
        String auth = session.bearer();

        JsonNode summary = importCsv(session, SAMPLE_CSV);
        assertThat(summary.path("rowsRead").asInt()).isEqualTo(8);
        assertThat(summary.path("imported").asInt()).isEqualTo(8);
        assertThat(summary.path("categorizedByRules").asInt()).isGreaterThanOrEqualTo(6);
        assertThat(summary.path("aiAvailable").asBoolean()).isFalse();

        JsonNode again = importCsv(session, SAMPLE_CSV);
        assertThat(again.path("imported").asInt()).isZero();
        assertThat(again.path("duplicates").asInt()).isEqualTo(8);

        mvc.perform(get("/api/v1/transactions").header(HttpHeaders.AUTHORIZATION, auth)
                        .param("from", "2026-11-01").param("to", "2026-11-30").param("sort", "amount,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.items[0].merchant").value("LIDL VILNIUS"));
        mvc.perform(get("/api/v1/transactions").header(HttpHeaders.AUTHORIZATION, auth).param("sort", "bogus,asc"))
                .andExpect(status().isBadRequest());

        // CAFE BLOOM is unknown without AI: correct it once and remember the rule
        JsonNode cafe = body(mvc.perform(get("/api/v1/transactions").header(HttpHeaders.AUTHORIZATION, auth)
                .param("q", "cafe")).andReturn()).path("items").get(0);
        assertThat(cafe.path("needsReview").asBoolean()).isTrue();
        String eatingOut = categoryId(auth, "Eating out");
        mvc.perform(patch("/api/v1/transactions/" + cafe.path("id").asText()).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + eatingOut + "\",\"applyToMerchant\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleCreated").value(true))
                .andExpect(jsonPath("$.transaction.categorizationSource").value("MANUAL"));
        mvc.perform(get("/api/v1/rules").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$[0].pattern").value("CAFE BLOOM"));

        // budget and dashboard
        String groceries = categoryId(auth, "Groceries");
        mvc.perform(post("/api/v1/budgets").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("categoryId", groceries, "monthlyLimit", 50,
                                "currency", "EUR"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION));
        mvc.perform(post("/api/v1/budgets").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("categoryId", groceries, "monthlyLimit", 50,
                                "currency", "EUR"))))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, auth).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencies.length()").value(2))
                .andExpect(jsonPath("$.currencies[0].currency").value("EUR"))
                .andExpect(jsonPath("$.currencies[0].expenses").value(76.49))
                .andExpect(jsonPath("$.currencies[0].income").value(2500.0))
                .andExpect(jsonPath("$.budgets[0].health").value("EXCEEDED"));

        mvc.perform(get("/api/v1/subscriptions").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$[0].merchant").value("SPOTIFY"))
                .andExpect(jsonPath("$[0].priceIncreased").value(true));

        mvc.perform(get("/api/v1/imports").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/imports/formats").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.length()").value(4));

        mvc.perform(get("/api/v1/users/me/export").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("pocketlens-export-")))
                .andExpect(jsonPath("$.transactions.length()").value(8));

        mvc.perform(delete("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"Secret12345\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", session.email(), "password", "Secret12345"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidFilesAreRejectedWithClearErrors() throws Exception {
        Session session = register();
        MockMultipartFile wrongColumns = new MockMultipartFile("file", "s.csv", "text/csv",
                "Datum,Text\n2026-11-01,x\n".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/imports").file(wrongColumns).param("format", "generic")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_COLUMNS_MISSING"))
                .andExpect(jsonPath("$.missingColumns.length()").value(4));

        mvc.perform(multipart("/api/v1/imports/preview").file(wrongColumns)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headers[0]").value("Datum"));

        mvc.perform(post("/api/v1/import-profiles").header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "My bank", "delimiter", ",",
                                "dateColumn", "Datum", "dateFormat", "yyyy-MM-dd", "descriptionColumn", "Text",
                                "amountColumn", "Text", "defaultCurrency", "EUR", "decimalSeparator", "."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My bank"));
    }

    private String categoryId(String auth, String name) throws Exception {
        JsonNode categories = body(mvc.perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, auth))
                .andReturn());
        for (JsonNode c : categories) {
            if (name.equals(c.path("name").asText())) {
                return c.path("id").asText();
            }
        }
        throw new IllegalStateException("No category " + name);
    }
}
