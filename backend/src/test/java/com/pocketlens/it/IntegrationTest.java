package com.pocketlens.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class of integration tests: the full application against a real PostgreSQL in a container.
 * One container is shared by all test classes (singleton pattern).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    protected static final String SAMPLE_CSV = """
            Date,Description,Amount,Currency
            2026-09-03,SPOTIFY P1234567,-10.99,EUR
            2026-10-03,SPOTIFY P1234567,-10.99,EUR
            2026-11-03,SPOTIFY P1234567,-11.99,EUR
            2026-11-01,LIDL 1234 VILNIUS,-42.10,EUR
            2026-11-02,LIDL 1234 VILNIUS,-17.90,EUR
            2026-11-04,CAFE BLOOM 0425,-4.50,EUR
            2026-11-05,AMAZON MARKETPLACE,-25.00,USD
            2026-11-06,SALARY ACME,2500.00,EUR
            """;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JsonMapper json;

    /** Registers a new user and returns its access token. */
    protected Session register() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "Secret12345",
                                "displayName", "Tester", "acceptPrivacyNotice", true,
                                "privacyNoticeVersion", "2026-11"))))
                .andExpect(status().isCreated())
                .andReturn();
        DocumentContext body = body(result);
        return new Session(email, body.read("$.accessToken"), UUID.fromString(body.read("$.user.id")),
                result.getResponse().getCookie("pl_refresh").getValue());
    }

    protected DocumentContext importCsv(Session session, String csv) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        MvcResult result = mvc.perform(multipart("/api/v1/imports").file(file).param("format", "generic")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result);
    }

    protected DocumentContext body(MvcResult result) throws Exception {
        return JsonPath.parse(result.getResponse().getContentAsString());
    }

    /** Authenticated user of a test. */
    protected record Session(String email, String accessToken, UUID userId, String refreshToken) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }
}
