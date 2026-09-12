package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoderDeliveryApiTest {
    @Test
    void materialUploadRejectsHeaderInjectionAndEmptyProduct() {
        try (MockartyClient client = MockartyClient.create("http://127.0.0.1:1", "mk_test")) {
            assertThrows(IllegalArgumentException.class, () -> client.coderDelivery().uploadMissionMaterial("team", "p", "design.txt", new byte[64 * 1024 + 1], "text/plain"));
            assertThrows(IllegalArgumentException.class, () -> client.coderDelivery().uploadMissionMaterial("team", "", "design.txt", new byte[]{1}, "text/plain"));
            assertThrows(IllegalArgumentException.class, () -> client.coderDelivery().uploadMissionMaterial("team", "p", "design.txt", new byte[]{1}, "text/plain\r\nX: y"));
            assertThrows(IllegalArgumentException.class, () -> client.coderDelivery().uploadMissionMaterial("team", "p", "design\r\n.txt", new byte[]{1}, "text/plain"));
        }
    }

    @Test
    void materialUploadRejectsBackendInvalidNamesAndMimeBeforeNetwork() {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").apiKey("mk_test")
                .timeout(Duration.ofMillis(50)).maxRetries(0).build()) {
            CoderDeliveryApi api = client.coderDelivery();
            for (String filename : List.of(
                    "../design.txt",
                    " design.txt",
                    "\u00a0design.txt",
                    ".",
                    "a".repeat(129),
                    "é".repeat(65) + ".txt",
                    "bad\u0000.txt",
                    "bad\uD800.txt")) {
                assertThrows(IllegalArgumentException.class,
                        () -> api.uploadMissionMaterial("team", "p", filename,
                                new byte[]{1}, "text/plain"), filename);
            }
            assertThrows(IllegalArgumentException.class,
                    () -> api.uploadMissionMaterial("team", "p", "vector.svg",
                            new byte[]{1}, "image/svg+xml"));
        }
    }

    @Test
    void addToMissionDoesNotReplayTransientFailure() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = transientServer("/api/v1/coder/missions/m1/add", requests);
        try (MockartyClient client = retryingClient(server)) {
            assertThrows(MockartyException.class, () -> client.coderDelivery()
                    .addToMission("m1", Map.of("prompts", List.of("add tests"))));
        } finally {
            server.stop(0);
        }
        assertEquals(1, requests.get(), "non-idempotent mission addition must not be replayed");
    }

    @Test
    void materialUploadDoesNotReplayTransientFailure() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = transientServer("/api/v1/missions/materials", requests);
        try (MockartyClient client = retryingClient(server)) {
            assertThrows(MockartyException.class, () -> client.coderDelivery()
                    .uploadMissionMaterial("team", "p", "design.txt",
                            "content".getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8"));
        } finally {
            server.stop(0);
        }
        assertEquals(1, requests.get(), "non-idempotent material upload must not be replayed");
    }

    private static HttpServer transientServer(String path, AtomicInteger requests) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"error\":\"transient\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static MockartyClient retryingClient(HttpServer server) {
        return MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("mk_test").namespace("team")
                .timeout(Duration.ofSeconds(5)).maxRetries(2).build();
    }
    @Test
    void startRequiresGoalAndRepository() {
        try (MockartyClient client = MockartyClient.create("http://127.0.0.1:1", "mk_test")) {
            CoderDeliveryApi api = client.coderDelivery();
            assertThrows(IllegalArgumentException.class, () -> api.startMission(Map.of("goal", "ship")));
            assertThrows(IllegalArgumentException.class, () -> api.getMission(""));
            assertThrows(IllegalArgumentException.class, () -> api.reconcileDeploy("m1", ""));
            assertThrows(IllegalArgumentException.class, () -> api.addToMission("m1", Map.of()));
            assertThrows(IllegalArgumentException.class, () -> api.queryObservability(Map.of("source", "prometheus")));
        }
    }

    @Test
    void approvalSuffixPrecedesNamespaceQuery() throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").apiKey("mk_test").namespace("team a").build()) {
            CoderDeliveryApi api = client.coderDelivery();
            var method = CoderDeliveryApi.class.getDeclaredMethod("missionPath", String.class, String.class);
            method.setAccessible(true);
            assertEquals("/api/v1/coder/missions/m%2F1/approve?namespace=team%20a", method.invoke(api, "m/1", "/approve"));
            assertEquals("/api/v1/coder/missions/m%2F1/deploy-outcome?namespace=team%20a", method.invoke(api, "m/1", "/deploy-outcome"));
            assertEquals("/api/v1/coder/missions/m%2F1/add?namespace=team%20a", method.invoke(api, "m/1", "/add"));
            var observation = CoderDeliveryApi.class.getDeclaredMethod("observabilityPath", String.class);
            observation.setAccessible(true);
            assertEquals("/api/v1/observability/sources?namespace=team%20a", observation.invoke(api, "sources"));
        }
    }
}
