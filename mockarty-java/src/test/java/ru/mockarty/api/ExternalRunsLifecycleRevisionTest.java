package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalRunsLifecycleRevisionTest {
    private HttpServer server;
    private MockartyClient client;
    private volatile String stepsIfMatch;
    private volatile String attachmentIfMatch;
    private volatile String finishIfMatch;
    private volatile String attachmentContentType;
    private volatile byte[] attachmentBody;
    private final AtomicInteger requests = new AtomicInteger();
    private volatile boolean failWithServiceUnavailable;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/namespaces/sandbox/tcm/external-runs/lifecycle", this::handle);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("test-token").namespace("sandbox").timeout(Duration.ofSeconds(5)).maxRetries(2).build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void fencesEveryStreamingMutationAndUploadsMultipartEvidence() throws Exception {
        ExternalRunsApi api = client.externalRuns();
        JsonNode run = api.appendStepsAtRevision("sandbox", "run-1", 7,
                List.of(Map.of("step_key", "s1", "status", "passed")));
        assertEquals(8, run.path("revision").asInt());

        run = api.uploadAttachmentAtRevision("sandbox", "run-1", 8,
                "evidence.txt", "measured".getBytes(StandardCharsets.UTF_8));
        assertEquals(9, run.path("revision").asInt());

        run = api.finishRunAtRevision("sandbox", "run-1", 9, "passed", "ok");
        assertEquals(11, run.path("revision").asInt());
        assertEquals("\"7\"", stepsIfMatch);
        assertEquals("\"8\"", attachmentIfMatch);
        assertEquals("\"9\"", finishIfMatch);
        assertTrue(attachmentContentType.startsWith("multipart/form-data; boundary="));
        String multipart = new String(attachmentBody, StandardCharsets.UTF_8);
        assertTrue(multipart.contains("evidence.txt"));
        assertTrue(multipart.contains("measured"));
    }

    @Test
    void rejectsUnsafeAttachmentNamesBeforeNetwork() {
        ExternalRunsApi api = client.externalRuns();
        assertThrows(IllegalArgumentException.class,
                () -> api.uploadAttachment("sandbox", "run-1", "evidence\r\nX-Injected: true", new byte[0]));
    }

    @Test
    void lifecycleMutationsAreNeverAutomaticallyReplayed() {
        ExternalRunsApi api = client.externalRuns();
        failWithServiceUnavailable = true;

        assertThrows(RuntimeException.class,
                () -> api.startRun("sandbox", Map.of("name", "run")));
        assertThrows(RuntimeException.class,
                () -> api.appendStepsAtRevision("sandbox", "run-1", 7,
                        List.of(Map.of("step_key", "s1", "status", "passed"))));
        assertThrows(RuntimeException.class,
                () -> api.uploadAttachmentAtRevision("sandbox", "run-1", 7,
                        "evidence.txt", "measured".getBytes(StandardCharsets.UTF_8)));
        assertThrows(RuntimeException.class,
                () -> api.finishRunAtRevision("sandbox", "run-1", 7, "passed", "ok"));

        assertEquals(4, requests.get(),
                "each non-idempotent lifecycle mutation must make exactly one HTTP attempt");
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        if (failWithServiceUnavailable) {
            byte[] unavailable = "temporarily unavailable".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, unavailable.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(unavailable);
            }
            return;
        }
        String path = exchange.getRequestURI().getRawPath();
        int revision;
        String status = "running";
        if (path.endsWith("/steps")) {
            stepsIfMatch = exchange.getRequestHeaders().getFirst("If-Match");
            revision = 8;
        } else if (path.endsWith("/attachments")) {
            attachmentIfMatch = exchange.getRequestHeaders().getFirst("If-Match");
            attachmentContentType = exchange.getRequestHeaders().getFirst("Content-Type");
            attachmentBody = exchange.getRequestBody().readAllBytes();
            revision = 9;
        } else if (path.endsWith("/finish")) {
            finishIfMatch = exchange.getRequestHeaders().getFirst("If-Match");
            revision = 11;
            status = "passed";
        } else {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        byte[] bytes = ("{\"id\":\"run-1\",\"status\":\"" + status + "\",\"revision\":" + revision + "}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
