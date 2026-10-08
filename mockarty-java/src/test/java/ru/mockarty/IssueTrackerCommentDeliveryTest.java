package ru.mockarty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.exception.MockartyApiException;
import ru.mockarty.exception.MockartyException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Existing AddComment contract; actual HTTP boundary, no native authority/outbox claim. */
class IssueTrackerCommentDeliveryTest {
    private static final String PATH = "/api/v1/namespaces/sandbox/issuetracker/issues/issue/comments";
    private static final String OPERATION = "33333333-3333-4333-8333-333333333333";
    private static final String CAPTURE = "44444444-4444-4444-8444-444444444444";
    private static final String UNKNOWN = "{\"error\":\"collaboration_capture_outcome_unknown\",\"state\":\"unknown\",\"operation_id\":\"" + OPERATION + "\",\"capture_id\":\"" + CAPTURE + "\"}";

    private HttpServer server(int status, String body, AtomicInteger attempts, boolean lostBody) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(PATH, exchange -> {
            attempts.incrementAndGet();
            byte[] request = exchange.getRequestBody().readAllBytes();
            if (!exchange.getRequestURI().getPath().equals(PATH) || !exchange.getRequestMethod().equals("POST") || !new String(request, StandardCharsets.UTF_8).equals("{\"body\":\"Owned body\"}")) {
                exchange.sendResponseHeaders(400, -1); exchange.close(); return;
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, lostBody ? bytes.length + 1024 : bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().flush();
            exchange.close(); // Truncated post-header body is an actual read-loss, not bare EOF retry calibration.
        });
        server.start();
        return server;
    }

    private MockartyClient client(HttpServer server, int retries) {
        return MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .namespace("sandbox").timeout(Duration.ofSeconds(5)).maxRetries(retries).build();
    }

    @Test void healthy201AndPending202RetainTheirActualResult() throws Exception {
        for (int status : new int[]{201, 202}) {
            AtomicInteger attempts = new AtomicInteger();
            String body = status == 201 ? "{\"id\":\"native-comment\",\"body\":\"Owned body\"}" : "{\"state\":\"pending\",\"operation_id\":\"" + OPERATION + "\",\"capture_id\":\"" + CAPTURE + "\"}";
            HttpServer server = server(status, body, attempts, false);
            try (MockartyClient client = client(server, 2)) {
                JsonNode answer = client.issueTracker().addComment("sandbox", "issue", "Owned body");
                assertEquals(new ObjectMapper().readTree(body), answer);
                assertEquals(1, attempts.get());
                if (status == 202) assertFalse(answer.has("id"), "accepted intent is not a saved native comment");
            } finally { server.stop(0); }
        }
    }

    @Test void unknown503IsNotReplayedAndRetainsExactErrorBody() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = server(503, UNKNOWN, attempts, false);
        try (MockartyClient client = client(server, 2)) {
            MockartyApiException error = assertThrows(MockartyApiException.class,
                    () -> client.issueTracker().addComment("sandbox", "issue", "Owned body"));
            assertEquals(1, attempts.get(), "unknown comment POST must not automatically recapture");
            assertEquals(UNKNOWN, error.getResponseBody(), "existing error API must retain unknown capture handles");
        } finally { server.stop(0); }
    }

    @Test void transientStatusesDoNotReplayCommentPOST() throws Exception {
        for (int status : new int[]{429, 502, 503, 504}) {
            AtomicInteger attempts = new AtomicInteger();
            HttpServer server = server(status, "{\"error\":\"unavailable\"}", attempts, false);
            try (MockartyClient client = client(server, 2)) {
                assertThrows(MockartyApiException.class, () -> client.issueTracker().addComment("sandbox", "issue", "Owned body"));
                assertEquals(1, attempts.get(), "non-idempotent comment POST replayed at status " + status);
            } finally { server.stop(0); }
        }
    }

    @Test void lostResponseBodyDoesNotReplayCommentPOST() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = server(201, "{\"id\":\"native-comment\"}", attempts, true);
        try (MockartyClient client = client(server, 2)) {
            assertThrows(MockartyException.class, () -> client.issueTracker().addComment("sandbox", "issue", "Owned body"));
            assertEquals(1, attempts.get(), "lost response after possible commit must not replay comment POST");
        } finally { server.stop(0); }
    }
}
