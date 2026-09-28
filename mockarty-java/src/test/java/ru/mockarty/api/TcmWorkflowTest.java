package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TcmWorkflowTest {
    @Test
    void caseMetadataPngAndRunRoundTrip() throws Exception {
        byte[] png = new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 1, 2};
        String filename = "before \"login\".png";
        String base = "/api/v1/namespaces/qa";
        ObjectMapper mapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            byte[] response;
            int code = 200;
            if (method.equals("POST") && path.equals(base + "/test-cases")) {
                JsonNode request = mapper.readTree(exchange.getRequestBody());
                assertEquals("testit:100", request.path("externalId").asText());
                assertEquals("payments", request.path("customFields").get(0).path("value").asText());
                assertEquals("Pay", request.path("steps").get(0).path("action").asText());
                response = "{\"id\":\"c1\"}".getBytes(StandardCharsets.UTF_8);
                code = 201;
            } else if (method.equals("GET") && path.equals(base + "/test-cases/c1")) {
                response = "{\"id\":\"c1\",\"steps\":[{\"action\":\"Pay\"}]}".getBytes(StandardCharsets.UTF_8);
            } else if (method.equals("GET") && path.equals(base + "/test-cases")) {
                response = "{\"test_cases\":[{\"id\":\"c1\"}]}".getBytes(StandardCharsets.UTF_8);
            } else if (method.equals("POST") && path.equals(base + "/tcm/attachments/upload")) {
                assertEquals("parentKind=tcm_case&parentId=c1", exchange.getRequestURI().getQuery());
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
                assertTrue(body.contains("filename=\"before \\\"login\\\".png\""), body);
                assertTrue(body.contains("Content-Type: image/png"));
                assertTrue(body.contains(new String(png, StandardCharsets.ISO_8859_1)));
                response = "{\"id\":\"a1\"}".getBytes(StandardCharsets.UTF_8);
            } else if (method.equals("GET") && path.equals(base + "/tcm/attachments")) {
                response = "{\"items\":[{\"id\":\"a1\"}]}".getBytes(StandardCharsets.UTF_8);
            } else if (method.equals("GET") && path.equals(base + "/tcm/attachments/a1/raw")) {
                response = png;
            } else if (method.equals("POST") && path.equals(base + "/test-cases/c1/run")) {
                response = "{\"id\":\"r1\",\"status\":\"running\"}".getBytes(StandardCharsets.UTF_8);
                code = 202;
            } else if (method.equals("GET") && path.equals(base + "/tcm/case-runs/r1")) {
                response = "{\"id\":\"r1\",\"status\":\"running\"}".getBytes(StandardCharsets.UTF_8);
            } else {
                fail("unexpected " + method + " " + path);
                response = "{}".getBytes(StandardCharsets.UTF_8);
                code = 404;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, response.length);
            try (var out = exchange.getResponseBody()) { out.write(response); }
        });
        server.start();
        try (MockartyClient client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).namespace("qa").build()) {
            TcmApi api = client.tcm();
            JsonNode created = api.createCase(null, Map.of("name", "Payment", "externalId", "testit:100", "customFields", List.of(Map.of("name", "Component", "value", "payments")), "steps", List.of(Map.of("action", "Pay", "expectedResult", "Accepted"))));
            assertEquals("c1", created.path("id").asText());
            assertEquals("Pay", api.getCase(null, "c1").path("steps").get(0).path("action").asText());
            assertEquals(1, api.listCases(null, Map.of()).size());
            assertEquals("a1", api.uploadAttachment(null, "tcm_case", "c1", filename, png, "image/png").path("id").asText());
            assertEquals(1, api.listAttachments(null, "tcm_case", "c1").size());
            assertTrue(Arrays.equals(png, api.downloadAttachment(null, "a1")));
            assertEquals("r1", api.runCase(null, "c1", Map.of("mode", "manual")).path("id").asText());
            assertEquals("running", api.getCaseRun(null, "r1").path("status").asText());
            assertThrows(IllegalArgumentException.class, () -> api.uploadAttachment(null, "tcm_case", "c1", "bad\r\nname.png", png, "image/png"));
            assertThrows(IllegalArgumentException.class, () -> api.uploadAttachment(null, "tcm_case", "c1", "good.png", png, "image/png\r\nInjected: yes"));
        } finally { server.stop(0); }
    }
}
