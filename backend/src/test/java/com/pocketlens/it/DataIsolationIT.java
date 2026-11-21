package com.pocketlens.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * NFR-S2: every query is scoped by user; no user can read or change another user's data.
 */
class DataIsolationIT extends IntegrationTest {

    @Test
    void usersCannotSeeOrChangeEachOthersData() throws Exception {
        Session alice = register();
        Session bob = register();
        importCsv(alice, SAMPLE_CSV);

        JsonNode aliceTransactions = body(mvc.perform(get("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, alice.bearer())).andExpect(status().isOk()).andReturn());
        String transactionId = aliceTransactions.path("items").get(0).path("id").asText();
        JsonNode categories = body(mvc.perform(get("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bob.bearer())).andReturn());
        String categoryId = categories.get(0).path("id").asText();

        mvc.perform(get("/api/v1/transactions").header(HttpHeaders.AUTHORIZATION, bob.bearer()))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/v1/transactions/" + transactionId).header(HttpHeaders.AUTHORIZATION, bob.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(patch("/api/v1/transactions/" + transactionId).header(HttpHeaders.AUTHORIZATION, bob.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + categoryId + "\",\"applyToMerchant\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/dashboard").param("month", "2026-11").header(HttpHeaders.AUTHORIZATION, bob.bearer()))
                .andExpect(jsonPath("$.currencies.length()").value(0));

        JsonNode conversation = body(mvc.perform(post("/api/v1/conversations")
                .header(HttpHeaders.AUTHORIZATION, alice.bearer())).andExpect(status().isCreated()).andReturn());
        mvc.perform(get("/api/v1/conversations/" + conversation.path("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, bob.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void regularUsersCannotUseAdminEndpoints() throws Exception {
        Session user = register();
        mvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
