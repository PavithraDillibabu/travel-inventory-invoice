package com.navan.travel_inventory_invoice.integration;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.AfterAll;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceIntegrationTest {
    private static final String DB_USERNAME = System.getenv("DB_USERNAME");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    private static final String BASE_URL = System.getenv().getOrDefault(
            "TEST_DB_URL", "jdbc:postgresql://localhost:5432/my_local_db");
    private static final String TEST_SCHEMA = "invoice_it_" + UUID.randomUUID().toString().replace("-", "");
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
    void invoiceTripIsACompleteSnapshotAndTransactionsSurviveInvoiceDeletion() throws Exception {
        List<JsonNode> transactions = createTrip();
        String invoiceId = createInvoice(ids(transactions));
        JsonNode invoice = getInvoice(invoiceId);
        assertThat(invoice.path("currency").asText()).isEqualTo("EUR");
        assertMoney(invoice.path("total"), "685.70");
        assertThat(invoice.path("transaction_lines").size()).isEqualTo(5);
        assertMoney(lineByType(invoice, "flight").path("total"), "220.00");
        assertMoney(lineByType(invoice, "hotel").path("total"), "331.70");
        assertMoney(lineByType(invoice, "rail").path("total"), "94.00");
        assertMoney(lineByFeeKind(invoice, "trip_fee").path("total"), "25.00");
        assertMoney(lineByFeeKind(invoice, "agent_call_fee").path("total"), "15.00");

        assertThat(patchStatus(
                "/transactions/" + idOfType(transactions, "flight"),
                "{\"total\":999.00}"
        )).isEqualTo(409);
        assertThat(deleteStatus("/transactions/" + idOfType(transactions, "flight"))).isEqualTo(409);
        JsonNode afterRejectedEdits = getInvoice(invoiceId);
        assertMoney(afterRejectedEdits.path("total"), "685.70");
        assertMoney(lineByType(afterRejectedEdits, "flight").path("total"), "220.00");
        assertThat(getTransaction(idOfType(transactions, "flight")).path("id").asText())
                .isEqualTo(idOfType(transactions, "flight"));

        mvc.perform(delete("/invoices/{id}", invoiceId)).andExpect(status().isNoContent());
        mvc.perform(get("/invoices/{id}", invoiceId)).andExpect(status().isNotFound());
        for (JsonNode transaction : transactions) {
            JsonNode stillStored = getTransaction(transaction.path("id").asText());
            assertThat(stillStored.path("id").asText()).isEqualTo(transaction.path("id").asText());
        }
    }

    @Test
    void mixedCurrencyInvoiceReturnsConflictAndLeavesNoInvoice() throws Exception {
        JsonNode eurPayload = fixture("flight");
        ((ObjectNode) eurPayload).put("external_id", unique("mixed-eur"));
        ((ObjectNode) eurPayload).put("occurred_at", "2026-03-11T06:40:00Z");
        JsonNode eur = createTransaction(eurPayload);
        JsonNode usdPayload = fixture("hotel");
        ((ObjectNode) usdPayload).put("external_id", unique("mixed-usd"));
        ((ObjectNode) usdPayload).put("currency", "USD");
        JsonNode usd = createTransaction(usdPayload);
        ArrayNode request = mapper.createArrayNode().add(eur.path("id").asText()).add(usd.path("id").asText());
        mvc.perform(post("/invoices").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(mapper.createObjectNode().set("transaction_ids", request))))
                .andExpect(status().isConflict());
    }

    @Test
    void invoiceSnapshotRemainsUnchangedAfterTransactionDbUpdate()
            throws Exception {

        // 1. Create all five transactions
        List<JsonNode> transactions = createTrip();

        String flightId = idOfType(transactions, "flight");

        // 2. Create invoice with all five transactions
        String invoiceId = createInvoice(ids(transactions));

        // 3. Verify original invoice values
        JsonNode before = getInvoice(invoiceId);

        assertMoney(before.path("total"), "685.70");

        assertMoney(
                lineByType(before, "flight").path("total"),
                "220.00"
        );

        // 4. Directly update the original transaction in PostgreSQL
        int updatedRows = jdbc.update(
                "UPDATE transactions SET total_minor = ? WHERE id = ?",
                99900L,
                flightId
        );

        assertThat(updatedRows).isEqualTo(1);

        // 5. Verify the original transaction amount has changed
        JsonNode updatedTransaction = getTransaction(flightId);

        assertMoney(
                updatedTransaction.path("total"),
                "999.00"
        );

        // 6. Retrieve invoice again
        JsonNode after = getInvoice(invoiceId);

        // 7. Invoice snapshot must remain unchanged
        assertMoney(after.path("total"), "685.70");

        assertMoney(
                lineByType(after, "flight").path("total"),
                "220.00"
        );
    }

    private List<JsonNode> createTrip() throws Exception {
        JsonNode seeds = mapper.readTree(resource("seed-payloads.json"));
        List<JsonNode> created = new ArrayList<>();
        for (String key : List.of("flight", "hotel", "rail", "trip_fee", "agent_call_fee")) {
            JsonNode payload = seeds.get(key).deepCopy();
            ((ObjectNode) payload).put("external_id", unique(key));
            if (payload.path("occurred_at").asText().contains("0s3")) {
                ((ObjectNode) payload).put("occurred_at", "2026-03-11T06:40:00Z");
            }
            created.add(createTransaction(payload));
        }
        return created;
    }

    private JsonNode createTransaction(JsonNode payload) throws Exception {
        String body = mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    private String createInvoice(List<String> ids) throws Exception {
        String body = mvc.perform(post("/invoices").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(mapper.createObjectNode().set("transaction_ids", mapper.valueToTree(ids)))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("id").asText();
    }

    private JsonNode getInvoice(String id) throws Exception {
        return mapper.readTree(mvc.perform(get("/invoices/{id}", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode getTransaction(String id) throws Exception {
        return mapper.readTree(mvc.perform(get("/transactions/{id}", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private int patchStatus(String uri, String body) throws Exception {
        return mvc.perform(patch(uri).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();
    }

    private int deleteStatus(String uri) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(uri))
                .andReturn().getResponse().getStatus();
    }

    private JsonNode lineByType(JsonNode invoice, String type) {
        for (JsonNode line : invoice.path("transaction_lines")) if (type.equals(line.path("type").asText())) return line;
        throw new AssertionError("Missing invoice line of type " + type);
    }

    private JsonNode lineByFeeKind(JsonNode invoice, String kind) {
        for (JsonNode line : invoice.path("transaction_lines")) {
            if (kind.equals(line.at("/metadata/fee_kind").asText())) return line;
        }
        throw new AssertionError("Missing invoice line with fee_kind " + kind);
    }

    private String idOfType(List<JsonNode> transactions, String type) {
        return transactions.stream().filter(t -> type.equals(t.path("type").asText()))
                .findFirst().orElseThrow().path("id").asText();
    }

    private static List<String> ids(List<JsonNode> transactions) {
        return transactions.stream().map(t -> t.path("id").asText()).toList();
    }

    private JsonNode fixture(String name) throws Exception {
        return mapper.readTree(resource("seed-payloads.json")).get(name).deepCopy();
    }

    private static InputStream resource(String name) {
        return InvoiceIntegrationTest.class.getClassLoader().getResourceAsStream(name);
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

    private static void assertMoney(JsonNode actual, String expected) {
        assertThat(new BigDecimal(actual.asText())).isEqualByComparingTo(expected);
    }
}

