package com.navan.travel_inventory_invoice.integration;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.AfterAll;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {
    private static final String DB_USERNAME = System.getenv("DB_USERNAME");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    private static final String BASE_URL = System.getenv().getOrDefault(
            "TEST_DB_URL", "jdbc:postgresql://localhost:5432/my_local_db");
    private static final String TEST_SCHEMA = "transaction_it_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SCHEMA_URL = createIsolatedSchema();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> SCHEMA_URL);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> DB_USERNAME);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @AfterAll
    static void dropIsolatedSchema() throws SQLException {
        try (var connection = DriverManager.getConnection(BASE_URL, DB_USERNAME, DB_PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + TEST_SCHEMA + " CASCADE");
        }
    }

    @Test
    void createsEverySeedTypeAndGetsStructuredMetadata() throws Exception {
        JsonNode seeds = mapper.readTree(resource("seed-payloads.json"));
        for (String key : List.of("flight", "hotel", "rail", "trip_fee", "agent_call_fee")) {
            JsonNode payload = seeds.get(key).deepCopy();
            ((ObjectNode) payload).put("external_id", unique(key));
            fixFixtureTimestamp(payload);
            JsonNode created = create(payload, 201);
            JsonNode fetched = mapper.readTree(mvc.perform(get("/transactions/{id}", created.get("id").asText()))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(fetched.get("metadata").isObject()).isTrue();
            switch (key) {
                case "flight" -> assertThat(fetched.at("/metadata/coupons/0/flight_number").asText()).isEqualTo("BA982");
                case "hotel" -> assertThat(fetched.at("/metadata/address/city").asText()).isEqualTo("London");
                case "rail" -> assertThat(fetched.at("/metadata/legs").size()).isEqualTo(2);
                case "trip_fee" -> assertThat(fetched.at("/metadata/fee_kind").asText()).isEqualTo("trip_fee");
                case "agent_call_fee" -> assertThat(fetched.at("/metadata/fee_kind").asText()).isEqualTo("agent_call_fee");
            }
        }
    }

    @Test
    void duplicateExternalIdDoesNotCreateAnotherTransaction() throws Exception {
        JsonNode payload = fixture("flight");
        String externalId = unique("duplicate");
        ((ObjectNode) payload).put("external_id", externalId);
        JsonNode first = create(payload, 201);
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(payload)))
                .andExpect(status().isConflict());
        assertThat(transactionCount(externalId)).isEqualTo(1);
        assertThat(first.path("id").asText()).isNotBlank();
    }

    @Test
    void rejectsEmptyOrMismatchedMetadataAndUnknownTypesWithoutPersisting() throws Exception {
        JsonNode flightWithRailMetadata = fixture("flight");
        String railMismatchId = unique("flight-with-rail-metadata");
        ((ObjectNode) flightWithRailMetadata).put("external_id", railMismatchId);
        ((ObjectNode) flightWithRailMetadata).set("metadata", mapper.readTree("""
                {"legs":[{"origin":"London","destination":"Paris","departure":"2026-03-11T09:00:00Z","arrival":"2026-03-11T12:00:00Z","train_number":"T1","class":"standard"}]}
                """));
        assertInvalidAndNotStored(flightWithRailMetadata, railMismatchId);

        JsonNode emptyFlightMetadata = fixture("flight");
        String emptyFlightId = unique("empty-flight-metadata");
        ((ObjectNode) emptyFlightMetadata).put("external_id", emptyFlightId);
        ((ObjectNode) emptyFlightMetadata).set("metadata", mapper.createObjectNode());
        assertInvalidAndNotStored(emptyFlightMetadata, emptyFlightId);

        JsonNode unknownType = fixture("flight");
        String unknownTypeId = unique("unknown-type");
        ((ObjectNode) unknownType).put("external_id", unknownTypeId);
        ((ObjectNode) unknownType).put("type", "spaceship");
        assertInvalidAndNotStored(unknownType, unknownTypeId);

        JsonNode unknownFeeKind = fixture("trip_fee");
        String unknownFeeId = unique("unknown-fee-kind");
        ((ObjectNode) unknownFeeKind).put("external_id", unknownFeeId);
        ((ObjectNode) unknownFeeKind.get("metadata")).put("fee_kind", "support_fee");
        assertInvalidAndNotStored(unknownFeeKind, unknownFeeId);
    }

    private void assertInvalidAndNotStored(JsonNode payload, String externalId) throws Exception {
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest());
        assertThat(transactionCount(externalId)).isZero();
    }

    @Test
    void overlappingCreatesWithSameExternalIdLeaveOneRow() throws Exception {
        JsonNode payload = fixture("flight");
        String externalId = unique("race");
        ((ObjectNode) payload).put("external_id", externalId);
        byte[] body = mapper.writeValueAsBytes(payload);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> a = pool.submit(() -> concurrentPost(start, body));
            Future<Integer> b = pool.submit(() -> concurrentPost(start, body));
            start.countDown();
            int statusA = a.get();
            int statusB = b.get();
            assertThat(List.of(statusA, statusB)).contains(201).containsAnyOf(200, 409);
            assertThat(transactionCount(externalId)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void transactionListDefaultsToFiftyAndRejectsUnboundedLimit() throws Exception {
        JsonNode template = fixture("flight");
        for (int i = 0; i < 51; i++) {
            JsonNode payload = template.deepCopy();
            ((ObjectNode) payload).put("external_id", unique("page"));
            create(payload, 201);
        }
        JsonNode response = mapper.readTree(mvc.perform(get("/transactions"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(response.isArray()).isTrue();
        assertThat(response.size()).isEqualTo(50);
        mvc.perform(get("/transactions").param("limit", "1000")).andExpect(status().isBadRequest());
    }

    private int concurrentPost(CountDownLatch start, byte[] body) throws Exception {
        start.await();
        return mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();
    }

    private JsonNode create(JsonNode payload, int expectedStatus) throws Exception {
        String response = mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(payload)))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response);
    }

    private int transactionCount(String externalId) {
        return jdbc.queryForObject("select count(*) from transactions where external_id = ?", Integer.class, externalId);
    }

    private JsonNode fixture(String name) throws Exception {
        JsonNode payload = mapper.readTree(resource("seed-payloads.json")).get(name).deepCopy();
        fixFixtureTimestamp(payload);
        return payload;
    }

    private static void fixFixtureTimestamp(JsonNode payload) {
        if (payload.path("occurred_at").asText().contains("0s3")) {
            ((ObjectNode) payload).put("occurred_at", "2026-03-11T06:40:00Z");
        }
    }

    private static String unique(String prefix) { return prefix + "-" + UUID.randomUUID(); }

    private static String createIsolatedSchema() {
        if (DB_USERNAME == null || DB_PASSWORD == null) {
            throw new IllegalStateException("Set DB_USERNAME and DB_PASSWORD for PostgreSQL integration tests");
        }
        try (var connection = DriverManager.getConnection(BASE_URL, DB_USERNAME, DB_PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + TEST_SCHEMA);
        } catch (SQLException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        return BASE_URL + (BASE_URL.contains("?") ? "&" : "?") + "currentSchema=" + TEST_SCHEMA;
    }

    private static InputStream resource(String name) {
        return TransactionIntegrationTest.class.getClassLoader().getResourceAsStream(name);
    }
}

