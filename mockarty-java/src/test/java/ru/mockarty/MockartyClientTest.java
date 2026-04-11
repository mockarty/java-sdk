// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import ru.mockarty.exception.MockartyApiException;
import ru.mockarty.exception.MockartyConflictException;
import ru.mockarty.exception.MockartyConnectionException;
import ru.mockarty.exception.MockartyExternalException;
import ru.mockarty.exception.MockartyForbiddenException;
import ru.mockarty.exception.MockartyNotFoundException;
import ru.mockarty.exception.MockartyRateLimitException;
import ru.mockarty.exception.MockartyServerException;
import ru.mockarty.exception.MockartyUnauthorizedException;
import ru.mockarty.exception.MockartyUnavailableException;
import ru.mockarty.exception.MockartyValidationException;
import ru.mockarty.model.HealthResponse;
import ru.mockarty.model.Mock;
import ru.mockarty.model.SaveMockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockartyClientTest {

    private HttpServer server;
    private MockartyClient client;
    private int port;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + port)
                .apiKey("test-api-key")
                .namespace("test-namespace")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should create client with builder defaults")
        void builderDefaults() {
            try (MockartyClient c = MockartyClient.builder()
                    .baseUrl("http://localhost:5770")
                    .build()) {
                assertNotNull(c);
                assertEquals("http://localhost:5770", c.getConfig().getBaseUrl());
                assertEquals("sandbox", c.getConfig().getNamespace());
            }
        }

        @Test
        @DisplayName("should strip trailing slash from base URL")
        void stripTrailingSlash() {
            try (MockartyClient c = MockartyClient.builder()
                    .baseUrl("http://localhost:5770/")
                    .build()) {
                assertEquals("http://localhost:5770", c.getConfig().getBaseUrl());
            }
        }

        @Test
        @DisplayName("should configure all builder options")
        void allBuilderOptions() {
            try (MockartyClient c = MockartyClient.builder()
                    .baseUrl("http://custom:8080")
                    .apiKey("my-key")
                    .namespace("prod")
                    .timeout(Duration.ofSeconds(10))
                    .build()) {
                assertEquals("http://custom:8080", c.getConfig().getBaseUrl());
                assertEquals("prod", c.getConfig().getNamespace());
                assertNotNull(c.getConfig().getApiKey());
            }
        }
    }

    @Nested
    @DisplayName("Static factory methods")
    class FactoryTests {

        @Test
        @DisplayName("should create client with create()")
        void createDefault() {
            try (MockartyClient c = MockartyClient.create()) {
                assertNotNull(c);
            }
        }

        @Test
        @DisplayName("should create client with create(baseUrl)")
        void createWithUrl() {
            try (MockartyClient c = MockartyClient.create("http://localhost:9999")) {
                assertEquals("http://localhost:9999", c.getConfig().getBaseUrl());
            }
        }

        @Test
        @DisplayName("should create client with create(baseUrl, apiKey)")
        void createWithUrlAndKey() {
            try (MockartyClient c = MockartyClient.create("http://localhost:9999", "key123")) {
                assertEquals("http://localhost:9999", c.getConfig().getBaseUrl());
                assertEquals("key123", c.getConfig().getApiKey());
            }
        }
    }

    @Nested
    @DisplayName("API access")
    class ApiAccessTests {

        @Test
        @DisplayName("should return API instances")
        void apiInstances() {
            assertNotNull(client.mocks());
            assertNotNull(client.namespaces());
            assertNotNull(client.stores());
            assertNotNull(client.collections());
            assertNotNull(client.perf());
            assertNotNull(client.health());
        }
    }

    @Nested
    @DisplayName("HTTP operations")
    class HttpOperationTests {

        @Test
        @DisplayName("should send authorization header")
        void authorizationHeader() throws Exception {
            server.createContext("/api/v1/mocks/test-id", exchange -> {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                assertEquals("Bearer test-api-key", auth);

                Mock mock = new Mock().id("test-id").namespace("sandbox");
                byte[] body = mapper.writeValueAsBytes(mock);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            Mock result = client.mocks().get("test-id");
            assertEquals("test-id", result.getId());
        }

        @Test
        @DisplayName("should send user-agent header")
        void userAgentHeader() throws Exception {
            server.createContext("/health", exchange -> {
                String ua = exchange.getRequestHeaders().getFirst("User-Agent");
                assertTrue(ua.startsWith("mockarty-java-sdk/"));

                String json = "{\"status\":\"pass\",\"releaseId\":\"1.0.0\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            HealthResponse response = client.health().check();
            assertEquals("pass", response.getStatus());
        }

        @Test
        @DisplayName("should deserialize POST response")
        void postRequest() throws Exception {
            server.createContext("/api/v1/mocks", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                assertEquals("application/json", contentType);

                String json = "{\"overwritten\":false,\"mock\":{\"id\":\"new-mock\"}}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            SaveMockResponse result = client.mocks().create(new Mock().id("new-mock"));
            assertFalse(result.isOverwritten());
            assertEquals("new-mock", result.getMock().getId());
        }

        @Test
        @DisplayName("should handle DELETE request")
        void deleteRequest() throws Exception {
            server.createContext("/api/v1/mocks/to-delete", exchange -> {
                assertEquals("DELETE", exchange.getRequestMethod());
                exchange.sendResponseHeaders(200, -1);
            });

            assertDoesNotThrow(() -> client.mocks().delete("to-delete"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should throw MockartyNotFoundException for 404")
        void notFound() {
            server.createContext("/api/v1/mocks/missing", exchange -> {
                String json = "{\"error\":\"mock not found\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyNotFoundException e = assertThrows(MockartyNotFoundException.class,
                    () -> client.mocks().get("missing"));
            assertEquals(404, e.getStatusCode());
            assertTrue(e.getErrorMessage().contains("mock not found"));
        }

        @Test
        @DisplayName("should throw MockartyUnauthorizedException for 401")
        void unauthorized() {
            server.createContext("/health", exchange -> {
                String json = "{\"error\":\"unauthorized\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(401, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            assertThrows(MockartyUnauthorizedException.class,
                    () -> client.health().check());
        }

        @Test
        @DisplayName("should throw MockartyForbiddenException for 403")
        void forbidden() {
            server.createContext("/api/v1/mocks", exchange -> {
                String json = "{\"error\":\"forbidden\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(403, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            assertThrows(MockartyForbiddenException.class,
                    () -> client.mocks().create(new Mock()));
        }

        @Test
        @DisplayName("should throw MockartyApiException for 500")
        void serverError() {
            server.createContext("/health", exchange -> {
                String json = "{\"error\":\"internal error\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(500, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyApiException e = assertThrows(MockartyApiException.class,
                    () -> client.health().check());
            assertEquals(500, e.getStatusCode());
        }

        @Test
        @DisplayName("should throw MockartyConnectionException for unreachable server")
        void connectionError() {
            try (MockartyClient unreachable = MockartyClient.builder()
                    .baseUrl("http://localhost:1")
                    .timeout(Duration.ofSeconds(1))
                    .build()) {
                assertThrows(MockartyConnectionException.class,
                        () -> unreachable.health().check());
            }
        }

        @Test
        @DisplayName("should handle non-JSON error response")
        void nonJsonError() {
            server.createContext("/health", exchange -> {
                byte[] body = "Bad Gateway".getBytes();
                exchange.sendResponseHeaders(502, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyApiException e = assertThrows(MockartyApiException.class,
                    () -> client.health().check());
            assertEquals(502, e.getStatusCode());
            assertTrue(e.getErrorMessage().contains("Bad Gateway"));
        }

        @Test
        @DisplayName("should parse code and request_id from error envelope")
        void parsesCodeAndRequestId() {
            server.createContext("/api/v1/mocks/missing", exchange -> {
                String json =
                        "{\"error\":\"mock not found\",\"code\":\"not_found\",\"request_id\":\"req-abc-123\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyNotFoundException e = assertThrows(MockartyNotFoundException.class,
                    () -> client.mocks().get("missing"));
            assertEquals(404, e.getStatusCode());
            assertEquals("not_found", e.getCode());
            assertEquals("req-abc-123", e.getRequestId());
            assertTrue(e.getMessage().contains("not_found"));
            assertTrue(e.getMessage().contains("req-abc-123"));
        }

        @Test
        @DisplayName("should dispatch by code field — validation")
        void dispatchByCodeValidation() {
            // Status is 200 (unusual) but code is 'validation' → should still raise validation.
            // Use a realistic 400 case instead.
            server.createContext("/api/v1/mocks", exchange -> {
                String json = "{\"error\":\"bad input\",\"code\":\"validation\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(400, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyValidationException e = assertThrows(MockartyValidationException.class,
                    () -> client.mocks().create(new Mock()));
            assertEquals(400, e.getStatusCode());
            assertEquals("validation", e.getCode());
        }

        @Test
        @DisplayName("should dispatch by code field — conflict")
        void dispatchByCodeConflict() {
            server.createContext("/api/v1/mocks", exchange -> {
                String json = "{\"error\":\"duplicate\",\"code\":\"conflict\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(409, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyConflictException e = assertThrows(MockartyConflictException.class,
                    () -> client.mocks().create(new Mock()));
            assertEquals(409, e.getStatusCode());
            assertEquals("conflict", e.getCode());
        }

        @Test
        @DisplayName("should dispatch by code field — rate_limit")
        void dispatchByCodeRateLimit() {
            server.createContext("/health", exchange -> {
                String json = "{\"error\":\"too many requests\",\"code\":\"rate_limit\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(429, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyRateLimitException e = assertThrows(MockartyRateLimitException.class,
                    () -> client.health().check());
            assertEquals(429, e.getStatusCode());
            assertEquals("rate_limit", e.getCode());
        }

        @Test
        @DisplayName("should dispatch by code field — unavailable")
        void dispatchByCodeUnavailable() {
            server.createContext("/health", exchange -> {
                String json = "{\"error\":\"db down\",\"code\":\"unavailable\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(503, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyUnavailableException e = assertThrows(MockartyUnavailableException.class,
                    () -> client.health().check());
            assertEquals(503, e.getStatusCode());
            assertEquals("unavailable", e.getCode());
        }

        @Test
        @DisplayName("should dispatch by code field — external")
        void dispatchByCodeExternal() {
            server.createContext("/health", exchange -> {
                String json = "{\"error\":\"upstream failed\",\"code\":\"external\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(502, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyExternalException e = assertThrows(MockartyExternalException.class,
                    () -> client.health().check());
            assertEquals(502, e.getStatusCode());
            assertEquals("external", e.getCode());
        }

        @Test
        @DisplayName("code field wins over HTTP status for dispatch")
        void codeWinsOverStatus() {
            // Server returns 500 but with code=unavailable — the SDK should dispatch
            // by code (primary path), not by status.
            server.createContext("/health", exchange -> {
                String json = "{\"error\":\"db down\",\"code\":\"unavailable\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(500, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyUnavailableException e = assertThrows(MockartyUnavailableException.class,
                    () -> client.health().check());
            // StatusCode on the exception is pinned to the canonical 503 for Unavailable.
            assertEquals(503, e.getStatusCode());
            assertEquals("unavailable", e.getCode());
        }

        @Test
        @DisplayName("should fall back to status when code is missing (legacy server)")
        void legacyServerNoCode() {
            server.createContext("/api/v1/mocks/missing", exchange -> {
                String json = "{\"error\":\"not found\"}";
                byte[] body = json.getBytes();
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            MockartyNotFoundException e = assertThrows(MockartyNotFoundException.class,
                    () -> client.mocks().get("missing"));
            assertEquals(404, e.getStatusCode());
            assertNull(e.getCode());
        }
    }

    @Nested
    @DisplayName("Health API")
    class HealthApiTests {

        @Test
        @DisplayName("live() should return true for healthy server")
        void liveTrue() {
            server.createContext("/health", exchange -> {
                String json = "{\"status\":\"pass\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            assertTrue(client.health().live());
        }

        @Test
        @DisplayName("live() should return false for unreachable server")
        void liveFalse() {
            try (MockartyClient unreachable = MockartyClient.builder()
                    .baseUrl("http://localhost:1")
                    .timeout(Duration.ofSeconds(1))
                    .build()) {
                assertFalse(unreachable.health().live());
            }
        }

        @Test
        @DisplayName("ready() should return true for healthy server")
        void readyTrue() {
            server.createContext("/health", exchange -> {
                String json = "{\"status\":\"pass\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            assertTrue(client.health().ready());
        }

        @Test
        @DisplayName("ready() should return false for failing server")
        void readyFalse() {
            server.createContext("/health", exchange -> {
                String json = "{\"status\":\"fail\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            assertFalse(client.health().ready());
        }

        @Test
        @DisplayName("version() should return release ID")
        void version() {
            server.createContext("/health", exchange -> {
                String json = "{\"status\":\"pass\",\"releaseId\":\"2.5.0\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            assertEquals("2.5.0", client.health().version());
        }
    }

    @Nested
    @DisplayName("Store API")
    class StoreApiTests {

        @Test
        @DisplayName("should get global store")
        void globalGet() throws Exception {
            server.createContext("/api/v1/stores/global", exchange -> {
                assertTrue(exchange.getRequestURI().getQuery().contains("namespace=test-namespace"));
                String json = "{\"counter\":42,\"flag\":true}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            Map<String, Object> store = client.stores().globalGet();
            assertEquals(42, store.get("counter"));
            assertEquals(true, store.get("flag"));
        }
    }

    @Nested
    @DisplayName("Namespace API")
    class NamespaceApiTests {

        @Test
        @DisplayName("should list namespaces")
        void list() throws Exception {
            server.createContext("/api/v1/namespaces", exchange -> {
                String json = "[\"sandbox\",\"production\",\"staging\"]";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            var namespaces = client.namespaces().list();
            assertEquals(3, namespaces.size());
            assertTrue(namespaces.contains("sandbox"));
            assertTrue(namespaces.contains("production"));
        }
    }
}
