package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudRefundsApiTest {
    private HttpServer server;
    private MockartyClient client;
    private volatile String idempotencyKey;
    private volatile String requestBody;
    private volatile String requestPath;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/cloud/operator/refunds/", this::handle);
        server.start();
        client = MockartyClient.create("http://127.0.0.1:" + server.getAddress().getPort(), "operator-token");
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void resolveRefundPreservesExactOperatorContract() throws Exception {
        JsonNode response = client.cloudRefunds().resolveRefund("refund/1", "retry",
                "provider_recovery_retry", 4, "refund-resolution:exact-1");
        assertEquals(5, response.path("refund").path("generation").asLong());
        assertTrue(response.path("replayed").asBoolean());
        assertEquals("refund-resolution:exact-1", idempotencyKey);
        assertEquals("/api/v1/cloud/operator/refunds/refund%2F1/resolve", requestPath);
        assertEquals("{\"action\":\"retry\",\"reason_code\":\"provider_recovery_retry\",\"generation\":4}", requestBody);
    }

    @Test
    void resolveRefundRejectsUnsafeInputs() {
        CloudRefundsApi api = client.cloudRefunds();
        assertThrows(IllegalArgumentException.class,
                () -> api.resolveRefund("", "reject", "provider_reject", 0, "refund-1"));
        assertThrows(IllegalArgumentException.class,
                () -> api.resolveRefund(" ", "reject", "provider_reject", 0, "refund-1"));
        assertThrows(IllegalArgumentException.class,
                () -> api.resolveRefund("op-1", "succeeded", "operator_says_paid", 0, "refund-1"));
        assertThrows(IllegalArgumentException.class,
                () -> api.resolveRefund("op-1", "reject", "Customer said no", 0, "refund-1"));
        assertThrows(IllegalArgumentException.class,
                () -> api.resolveRefund("op-1", "retry", "provider_retry", -1, "refund-1"));
        assertThrows(IllegalArgumentException.class,
                () -> api.resolveRefund("op-1", "retry", "provider_retry", 0, " refund-1 "));
    }

    @Test
    void doesNotExposeInteractiveSelfServiceCreation() {
        assertTrue(Arrays.stream(CloudRefundsApi.class.getMethods()).noneMatch(method ->
                method.getName().equals("requestRefund") || method.getName().equals("createRefund")));
    }

    private void handle(HttpExchange exchange) throws IOException {
        idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        requestPath = exchange.getRequestURI().getRawPath();
        requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        byte[] response = "{\"refund\":{\"operation_id\":\"refund/1\",\"status\":\"accepted\",\"generation\":5},\"replayed\":true,\"request_id\":\"req-1\"}"
                .getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
