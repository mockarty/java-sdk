// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import ru.mockarty.model.Contract;
import ru.mockarty.model.ContractValidationResult;
import ru.mockarty.model.FuzzingConfig;
import ru.mockarty.model.FuzzingResult;
import ru.mockarty.model.FuzzingRun;
import ru.mockarty.model.GeneratorPreview;
import ru.mockarty.model.GeneratorRequest;
import ru.mockarty.model.GeneratorResponse;
import ru.mockarty.model.ImportResult;
import ru.mockarty.model.RecorderEntry;
import ru.mockarty.model.RecorderSession;
import ru.mockarty.model.TemplateFile;
import ru.mockarty.model.TestRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockartyAdvancedApiTest {

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
    @DisplayName("API accessor methods")
    class AccessorTests {

        @Test
        @DisplayName("should return all new API instances")
        void apiInstances() {
            assertNotNull(client.generator());
            assertNotNull(client.fuzzing());
            assertNotNull(client.contracts());
            assertNotNull(client.recorder());
            assertNotNull(client.templates());
            assertNotNull(client.imports());
            assertNotNull(client.testRuns());
            assertNotNull(client.tags());
            assertNotNull(client.folders());
            assertNotNull(client.undefined());
            assertNotNull(client.stats());
            assertNotNull(client.agentTasks());
            assertNotNull(client.namespaceSettings());
            assertNotNull(client.proxy());
            assertNotNull(client.environments());
        }
    }

    @Nested
    @DisplayName("Generator API")
    class GeneratorApiTests {

        @Test
        @DisplayName("should generate mocks from OpenAPI")
        void generateFromOpenAPI() throws Exception {
            server.createContext("/api/v1/generators/openapi", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());

                String json = "{\"created\":5,\"updated\":0,\"skipped\":0}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            GeneratorRequest request = new GeneratorRequest()
                    .spec("{\"openapi\":\"3.0.0\"}")
                    .namespace("test");
            GeneratorResponse result = client.generator().fromOpenAPI(request);
            assertEquals(5, result.getCreated());
        }

        @Test
        @DisplayName("should preview OpenAPI generation")
        void previewOpenAPI() throws Exception {
            server.createContext("/api/v1/generators/openapi/preview", exchange -> {
                String json = "{\"count\":3,\"warnings\":[]}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            GeneratorPreview result = client.generator().previewOpenAPI(
                    new GeneratorRequest().spec("spec-content"));
            assertEquals(3, result.getCount());
        }

        @Test
        @DisplayName("should generate mocks from WSDL")
        void generateFromWSDL() throws Exception {
            server.createContext("/api/v1/generators/soap", exchange -> {
                String json = "{\"created\":2}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            GeneratorResponse result = client.generator().fromWSDL(
                    new GeneratorRequest().wsdlContent("<wsdl/>"));
            assertEquals(2, result.getCreated());
        }

        @Test
        @DisplayName("should generate mocks from Proto")
        void generateFromProto() throws Exception {
            server.createContext("/api/v1/generators/grpc", exchange -> {
                String json = "{\"created\":4}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            GeneratorResponse result = client.generator().fromProto(
                    new GeneratorRequest().protoContent("syntax = \"proto3\";"));
            assertEquals(4, result.getCreated());
        }

        @Test
        @DisplayName("should generate mocks from GraphQL")
        void generateFromGraphQL() throws Exception {
            server.createContext("/api/v1/generators/graphql", exchange -> {
                String json = "{\"created\":3}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            GeneratorResponse result = client.generator().fromGraphQL(
                    new GeneratorRequest().spec("type Query { users: [User] }"));
            assertEquals(3, result.getCreated());
        }

        @Test
        @DisplayName("should generate mocks from HAR")
        void generateFromHAR() throws Exception {
            server.createContext("/api/v1/generators/har", exchange -> {
                String json = "{\"created\":10}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            GeneratorResponse result = client.generator().fromHAR(
                    new GeneratorRequest().harContent("{\"log\":{}}"));
            assertEquals(10, result.getCreated());
        }
    }

    @Nested
    @DisplayName("Fuzzing API")
    class FuzzingApiTests {

        @Test
        @DisplayName("should start fuzzing run")
        void startRun() throws Exception {
            server.createContext("/api/v1/fuzzing/run", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                String json = "{\"id\":\"run-1\",\"configId\":\"cfg-1\",\"status\":\"running\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            FuzzingRun result = client.fuzzing().start("cfg-1");
            assertEquals("run-1", result.getId());
            assertEquals("running", result.getStatus());
        }

        @Test
        @DisplayName("should get fuzzing result")
        void getResult() throws Exception {
            server.createContext("/api/v1/fuzzing/results/run-1", exchange -> {
                String json = "{\"id\":\"res-1\",\"configId\":\"cfg-1\",\"status\":\"completed\",\"totalRequests\":1000,\"totalFindings\":5}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            FuzzingResult result = client.fuzzing().getResult("run-1");
            assertEquals("completed", result.getStatus());
            assertEquals(1000, result.getTotalRequests());
        }

        @Test
        @DisplayName("should create fuzzing config")
        void createConfig() throws Exception {
            server.createContext("/api/v1/fuzzing/configs", exchange -> {
                String json = "{\"id\":\"cfg-new\",\"name\":\"Test Config\",\"targetUrl\":\"http://example.com\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            FuzzingConfig config = new FuzzingConfig()
                    .name("Test Config")
                    .targetBaseUrl("http://example.com");
            FuzzingConfig result = client.fuzzing().createConfig(config);
            assertEquals("cfg-new", result.getId());
            assertEquals("Test Config", result.getName());
        }

        @Test
        @DisplayName("should stop fuzzing run")
        void stopRun() throws Exception {
            server.createContext("/api/v1/fuzzing/run/run-1/stop", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                exchange.sendResponseHeaders(200, -1);
            });

            assertDoesNotThrow(() -> client.fuzzing().stop("run-1"));
        }

        @Test
        @DisplayName("should list fuzzing results")
        void listResults() throws Exception {
            server.createContext("/api/v1/fuzzing/results", exchange -> {
                String json = "[{\"id\":\"r1\",\"status\":\"completed\"},{\"id\":\"r2\",\"status\":\"running\"}]";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            List<FuzzingResult> results = client.fuzzing().listResults();
            assertEquals(2, results.size());
        }
    }

    @Nested
    @DisplayName("Contract API")
    class ContractApiTests {

        @Test
        @DisplayName("should validate mocks against contract")
        void validateMocks() throws Exception {
            server.createContext("/api/v1/contract/validate-mocks", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                String json = "{\"valid\":true,\"violations\":[]}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            Map<String, Object> result = client.contracts().validateMocks(
                    Map.of("mockIds", List.of("m1", "m2")));
            assertEquals(true, result.get("valid"));
        }

        @Test
        @DisplayName("should verify provider")
        void verifyProvider() throws Exception {
            server.createContext("/api/v1/contract/verify-provider", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                String json = "{\"valid\":true,\"results\":[]}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            Map<String, Object> result = client.contracts().verifyProvider(
                    Map.of("providerUrl", "http://example.com"));
            assertEquals(true, result.get("valid"));
        }

        @Test
        @DisplayName("should list contract configs")
        void listConfigs() throws Exception {
            server.createContext("/api/v1/contract/configs", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String json = "[{\"id\":\"ct-1\",\"name\":\"User API\"},{\"id\":\"ct-2\",\"name\":\"Order API\"}]";
                    byte[] body = json.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                }
            });

            List<Contract> configs = client.contracts().listConfigs();
            assertEquals(2, configs.size());
        }

        @Test
        @DisplayName("should create contract config")
        void createConfig() throws Exception {
            server.createContext("/api/v1/contract/configs", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    String json = "{\"id\":\"ct-1\",\"name\":\"User API\",\"protocol\":\"http\"}";
                    byte[] body = json.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                }
            });

            Contract config = new Contract()
                    .name("User API")
                    .protocol("http");
            Contract result = client.contracts().createConfig(config);
            assertEquals("ct-1", result.getId());
            assertEquals("User API", result.getName());
        }

        @Test
        @DisplayName("should delete contract config")
        void deleteConfig() throws Exception {
            server.createContext("/api/v1/contract/configs/ct-1", exchange -> {
                assertEquals("DELETE", exchange.getRequestMethod());
                exchange.sendResponseHeaders(200, -1);
            });

            assertDoesNotThrow(() -> client.contracts().deleteConfig("ct-1"));
        }

        @Test
        @DisplayName("should list contract results")
        void listResults() throws Exception {
            server.createContext("/api/v1/contract/results", exchange -> {
                String json = "[{\"contractId\":\"ct-1\",\"valid\":true}]";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            List<ContractValidationResult> results = client.contracts().listResults();
            assertEquals(1, results.size());
            assertTrue(results.get(0).isPassed());
        }

        @Test
        @DisplayName("should get contract result by ID")
        void getResult() throws Exception {
            server.createContext("/api/v1/contract/results/res-1", exchange -> {
                String json = "{\"contractId\":\"ct-1\",\"valid\":true,\"violations\":[]}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            ContractValidationResult result = client.contracts().getResult("res-1");
            assertTrue(result.isPassed());
            assertEquals("ct-1", result.getContractId());
        }
    }

    @Nested
    @DisplayName("Recorder API")
    class RecorderApiTests {

        @Test
        @DisplayName("should start recording session")
        void startSession() throws Exception {
            server.createContext("/api/v1/recorder/start", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                String json = "{\"id\":\"sess-1\",\"name\":\"My Session\",\"status\":\"created\",\"targetUrl\":\"http://example.com\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            RecorderSession session = client.recorder().start(Map.of(
                    "name", "My Session",
                    "targetUrl", "http://example.com",
                    "namespace", "test-namespace"
            ));
            assertEquals("sess-1", session.getId());
            assertEquals("created", session.getStatus());
        }

        @Test
        @DisplayName("should list sessions")
        void listSessions() throws Exception {
            server.createContext("/api/v1/recorder/sessions", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String json = "[{\"id\":\"s1\",\"name\":\"Session 1\"},{\"id\":\"s2\",\"name\":\"Session 2\"}]";
                    byte[] body = json.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                }
            });

            List<RecorderSession> sessions = client.recorder().listSessions();
            assertEquals(2, sessions.size());
        }

        @Test
        @DisplayName("should stop recording")
        void stopRecording() throws Exception {
            server.createContext("/api/v1/recorder/sess-1/stop", exchange -> {
                String json = "{\"id\":\"sess-1\",\"status\":\"stopped\"}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            RecorderSession result = client.recorder().stopRecording("sess-1");
            assertEquals("stopped", result.getStatus());
        }

        @Test
        @DisplayName("should get entries")
        void getEntries() throws Exception {
            server.createContext("/api/v1/recorder/sess-1/entries", exchange -> {
                String json = "[{\"id\":\"e1\",\"method\":\"GET\",\"path\":\"/api/users\",\"statusCode\":200}]";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            List<RecorderEntry> entries = client.recorder().getEntries("sess-1");
            assertEquals(1, entries.size());
            assertEquals("GET", entries.get(0).getMethod());
            assertEquals(200, entries.get(0).getStatusCode());
        }

        @Test
        @DisplayName("should replay session and forward options")
        void replaySession() throws Exception {
            server.createContext("/api/v1/recorder/sess-1/replay", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                String reqBody = new String(exchange.getRequestBody().readAllBytes());
                // Server-side smoke check that the options actually crossed
                // the wire as JSON.
                assertTrue(reqBody.contains("staging.example.com"),
                        "request body missing targetUrl, got: " + reqBody);
                assertTrue(reqBody.contains("\"concurrency\""),
                        "request body missing concurrency");
                String json = "{\"sessionId\":\"sess-1\",\"totalEntries\":3,\"matched\":2,"
                        + "\"mismatched\":0,\"failed\":1,\"skipped\":0,\"results\":[]}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            Map<String, Object> summary = client.recorder().replaySession("sess-1", Map.of(
                    "targetUrl", "http://staging.example.com",
                    "concurrency", 5,
                    "timeoutMs", 5000
            ));
            assertNotNull(summary);
            // Jackson decodes JSON numbers as Integer, but defensively also
            // accept Long here for forward-compatibility.
            Object matched = summary.get("matched");
            assertTrue(matched instanceof Number);
            assertEquals(2, ((Number) matched).intValue());
            assertEquals(3, ((Number) summary.get("totalEntries")).intValue());
        }

        @Test
        @DisplayName("should replay session with null options")
        void replaySessionNullOptions() throws Exception {
            server.createContext("/api/v1/recorder/sess-2/replay", exchange -> {
                String reqBody = new String(exchange.getRequestBody().readAllBytes());
                // Even with null options the SDK must send a JSON object,
                // never a literal "null" payload.
                assertTrue(reqBody.startsWith("{") && reqBody.endsWith("}"),
                        "expected empty JSON object, got: " + reqBody);
                String json = "{\"sessionId\":\"sess-2\",\"totalEntries\":0}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            Map<String, Object> summary = client.recorder().replaySession("sess-2", null);
            assertNotNull(summary);
            assertEquals(0, ((Number) summary.get("totalEntries")).intValue());
        }

        @Test
        @DisplayName("should correlate session and return report")
        void correlateSession() throws Exception {
            server.createContext("/api/v1/recorder/sess-1/correlate", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                String reqBody = new String(exchange.getRequestBody().readAllBytes());
                assertTrue(reqBody.contains("\"minValueLength\""),
                        "expected minValueLength in body, got: " + reqBody);
                String json = "{\"sessionId\":\"sess-1\",\"totalEntries\":4,"
                        + "\"correlations\":[{\"value\":\"tok-abc\",\"valueType\":\"token\","
                        + "\"confidence\":0.95,\"source\":{\"entryId\":\"e1\","
                        + "\"section\":\"response.body.json\"},\"targets\":[{\"entryId\":\"e2\","
                        + "\"section\":\"request.header\"}]}]}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            Map<String, Object> report = client.recorder().correlateSession("sess-1", Map.of(
                    "minValueLength", 8,
                    "excludeNumeric", true
            ));
            assertNotNull(report);
            assertEquals(4, ((Number) report.get("totalEntries")).intValue());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> corrs = (List<Map<String, Object>>) report.get("correlations");
            assertEquals(1, corrs.size());
            assertEquals("token", corrs.get(0).get("valueType"));
        }
    }

    @Nested
    @DisplayName("Template API")
    class TemplateApiTests {

        @Test
        @DisplayName("should list templates")
        void listTemplates() throws Exception {
            server.createContext("/api/v1/templates", exchange -> {
                String json = "[{\"name\":\"users.json\",\"size\":1024},{\"name\":\"orders.json\",\"size\":2048}]";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            List<TemplateFile> templates = client.templates().list();
            assertEquals(2, templates.size());
            assertEquals("users.json", templates.get(0).getName());
        }

        @Test
        @DisplayName("should upload template")
        void uploadTemplate() throws Exception {
            server.createContext("/api/v1/templates", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    String json = "{\"name\":\"response.json\",\"size\":512}";
                    byte[] body = json.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                }
            });

            TemplateFile result = client.templates().upload("response.json", "{\"status\":\"ok\"}");
            assertEquals("response.json", result.getName());
        }
    }

    @Nested
    @DisplayName("Import API")
    class ImportApiTests {

        @Test
        @DisplayName("should import from Postman")
        void importPostman() throws Exception {
            server.createContext("/api/v1/api-tester/import/postman", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                String json = "{\"created\":3,\"updated\":0,\"skipped\":1}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            ImportResult result = client.imports().postman("{\"info\":{\"name\":\"test\"}}", null);
            assertEquals(3, result.getCreated());
            assertEquals(1, result.getSkipped());
        }

        @Test
        @DisplayName("should import from OpenAPI")
        void importOpenAPI() throws Exception {
            server.createContext("/api/v1/api-tester/import/openapi", exchange -> {
                String json = "{\"created\":5,\"mockIds\":[\"m1\",\"m2\",\"m3\",\"m4\",\"m5\"]}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            ImportResult result = client.imports().openAPI("{\"openapi\":\"3.0.0\"}", "prod");
            assertEquals(5, result.getCreated());
            assertEquals(5, result.getMockIds().size());
        }

        @Test
        @DisplayName("should import from HAR")
        void importHAR() throws Exception {
            server.createContext("/api/v1/api-tester/import/har", exchange -> {
                String json = "{\"created\":2}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            ImportResult result = client.imports().har("{\"log\":{}}", null);
            assertEquals(2, result.getCreated());
        }

        @Test
        @DisplayName("should import from WSDL")
        void importWsdl() throws Exception {
            server.createContext("/api/v1/api-tester/import/wsdl", exchange -> {
                String json = "{\"created\":1}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            ImportResult result = client.imports().wsdl("<wsdl/>", null);
            assertEquals(1, result.getCreated());
        }

        @Test
        @DisplayName("should import from gRPC proto")
        void importGrpc() throws Exception {
            server.createContext("/api/v1/api-tester/import/grpc", exchange -> {
                String json = "{\"created\":4}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            ImportResult result = client.imports().grpcProto("syntax = \"proto3\";", null);
            assertEquals(4, result.getCreated());
        }

        @Test
        @DisplayName("should import from Mockarty export")
        void importMockarty() throws Exception {
            server.createContext("/api/v1/api-tester/import/mockarty", exchange -> {
                String json = "{\"created\":7,\"updated\":2}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            ImportResult result = client.imports().mockarty("{\"mocks\":[]}", "staging");
            assertEquals(7, result.getCreated());
            assertEquals(2, result.getUpdated());
        }
    }

    @Nested
    @DisplayName("TestRun API")
    class TestRunApiTests {

        @Test
        @DisplayName("should list test runs")
        void listRuns() throws Exception {
            server.createContext("/api/v1/api-tester/test-runs", exchange -> {
                String json = "[{\"id\":\"tr-1\",\"status\":\"completed\",\"totalTests\":10,\"passedTests\":9,\"failedTests\":1}]";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            List<TestRun> runs = client.testRuns().list();
            assertEquals(1, runs.size());
            assertEquals("completed", runs.get(0).getStatus());
            assertEquals(10, runs.get(0).getTotalTests());
        }

        @Test
        @DisplayName("should get test run by ID")
        void getRun() throws Exception {
            server.createContext("/api/v1/api-tester/test-runs/tr-1", exchange -> {
                String json = "{\"id\":\"tr-1\",\"status\":\"completed\",\"passedTests\":5,\"failedTests\":0}";
                byte[] body = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            TestRun run = client.testRuns().get("tr-1");
            assertEquals("tr-1", run.getId());
            assertEquals(0, run.getFailedTests());
        }

        @Test
        @DisplayName("should cancel test run")
        void cancelRun() throws Exception {
            server.createContext("/api/v1/api-tester/test-runs/tr-1/cancel", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                exchange.sendResponseHeaders(200, -1);
            });

            assertDoesNotThrow(() -> client.testRuns().cancel("tr-1"));
        }

        @Test
        @DisplayName("should delete test run")
        void deleteRun() throws Exception {
            server.createContext("/api/v1/api-tester/test-runs/tr-1", exchange -> {
                if ("DELETE".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                }
            });

            assertDoesNotThrow(() -> client.testRuns().delete("tr-1"));
        }

        @Test
        @DisplayName("should export test run")
        void exportRun() throws Exception {
            server.createContext("/api/v1/api-tester/test-runs/tr-1/export", exchange -> {
                byte[] body = "{\"results\":[]}".getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            byte[] bytes = client.testRuns().export("tr-1", "json");
            assertNotNull(bytes);
            assertTrue(bytes.length > 0);
        }
    }

    @Nested
    @DisplayName("Model builder patterns")
    class ModelBuilderTests {

        @Test
        @DisplayName("GeneratorRequest should support builder pattern")
        void generatorRequestBuilder() {
            GeneratorRequest req = new GeneratorRequest()
                    .spec("spec-content")
                    .namespace("prod")
                    .pathPrefix("/api/v2")
                    .serverName("my-server")
                    .graphqlUrl("http://graphql.example.com");

            assertEquals("spec-content", req.getSpec());
            assertEquals("prod", req.getNamespace());
            assertEquals("/api/v2", req.getPathPrefix());
            assertEquals("my-server", req.getServerName());
            assertEquals("http://graphql.example.com", req.getGraphqlUrl());
        }

        @Test
        @DisplayName("FuzzingConfig should support builder pattern")
        void fuzzingConfigBuilder() {
            FuzzingConfig config = new FuzzingConfig()
                    .name("SQL Injection Test")
                    .targetBaseUrl("http://target.example.com/api")
                    .method("POST")
                    .duration(60)
                    .concurrency(10)
                    .securityChecks(true);

            assertEquals("SQL Injection Test", config.getName());
            assertEquals("http://target.example.com/api", config.getTargetBaseUrl());
            assertEquals("POST", config.getMethod());
            assertEquals(60, config.getDuration());
            assertEquals(10, config.getConcurrency());
            assertTrue(config.getSecurityChecks());
        }

        @Test
        @DisplayName("Contract should support builder pattern")
        void contractBuilder() {
            Contract contract = new Contract()
                    .name("User API Contract")
                    .protocol("http")
                    .provider("user-service")
                    .consumer("frontend")
                    .enabled(true)
                    .tags(List.of("users", "v2"));

            assertEquals("User API Contract", contract.getName());
            assertEquals("http", contract.getProtocol());
            assertEquals("user-service", contract.getProvider());
            assertEquals("frontend", contract.getConsumer());
            assertTrue(contract.getEnabled());
            assertEquals(2, contract.getTags().size());
        }

        @Test
        @DisplayName("TestRun toString should be informative")
        void testRunToString() {
            TestRun run = new TestRun()
                    .id("tr-1")
                    .status("completed")
                    .totalTests(10)
                    .passedTests(9)
                    .failedTests(1);

            String str = run.toString();
            assertTrue(str.contains("tr-1"));
            assertTrue(str.contains("completed"));
            assertTrue(str.contains("10"));
        }

        @Test
        @DisplayName("ContractValidationResult isPassed should work correctly")
        void contractValidationResult() {
            ContractValidationResult passed = new ContractValidationResult().valid(true);
            assertTrue(passed.isPassed());

            ContractValidationResult failed = new ContractValidationResult().valid(false);
            assertFalse(failed.isPassed());

            ContractValidationResult nullResult = new ContractValidationResult();
            assertFalse(nullResult.isPassed());
        }

        @Test
        @DisplayName("ImportResult should have all fields")
        void importResultFields() {
            ImportResult result = new ImportResult()
                    .created(5)
                    .updated(2)
                    .skipped(1)
                    .mockIds(List.of("m1", "m2", "m3", "m4", "m5"))
                    .warnings(List.of("deprecated field"));

            assertEquals(5, result.getCreated());
            assertEquals(2, result.getUpdated());
            assertEquals(1, result.getSkipped());
            assertEquals(5, result.getMockIds().size());
            assertEquals(1, result.getWarnings().size());
        }

        @Test
        @DisplayName("RecorderEntry should support builder pattern")
        void recorderEntryBuilder() {
            RecorderEntry entry = new RecorderEntry()
                    .id("e1")
                    .method("POST")
                    .path("/api/orders")
                    .statusCode(201)
                    .duration(150L);

            assertEquals("e1", entry.getId());
            assertEquals("POST", entry.getMethod());
            assertEquals("/api/orders", entry.getPath());
            assertEquals(201, entry.getStatusCode());
            assertEquals(150L, entry.getDuration());
        }
    }
}
