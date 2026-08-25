package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.AgentTask;
import ru.mockarty.model.ToolReceipt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTaskReceiptApiTest {
    private static final String RECEIPT_KEY = "sha256:" + "a".repeat(64);
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/agent/tasks/t1", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/reconcile")) {
                String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(request.contains("\"expectedVersion\":4"));
                assertTrue(request.contains("\"idempotencyKey\":\"review-1\""));
                reply(exchange, "{\"receipt\":{\"receiptKey\":\"" + RECEIPT_KEY +
                        "\",\"status\":\"done\",\"version\":5}}");
                return;
            }
            reply(exchange, "{\"task\":{\"id\":\"t1\",\"status\":\"running\"}," +
                    "\"toolReceipts\":[{\"receiptKey\":\"" + RECEIPT_KEY +
                    "\",\"status\":\"awaiting_reconcile\",\"version\":4}]," +
                    "\"canReconcileToolReceipts\":false," +
                    "\"toolReceiptRetryAllowed\":false," +
                    "\"toolReceiptReconcileBlockedReason\":\"task_active\"}");
        });
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("key").timeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test void getExternalActionReceiptCapabilities() throws Exception {
        AgentTask task = client.agentTasks().get("t1");
        assertEquals(1, task.getToolReceipts().size());
        assertEquals(4, task.getToolReceipts().get(0).getVersion());
        assertFalse(task.isCanReconcileToolReceipts());
        assertFalse(task.isToolReceiptRetryAllowed());
        assertEquals("task_active", task.getToolReceiptReconcileBlockedReason());
    }

    @Test void reconcileToolReceiptWireContract() throws Exception {
        ToolReceipt receipt = client.agentTasks().reconcileToolReceipt(
                "t1", RECEIPT_KEY, 4, "review-1", "already_applied",
                "verified downstream", "invoice 42");
        assertEquals("done", receipt.getStatus());
        assertEquals(5, receipt.getVersion());
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }
}
