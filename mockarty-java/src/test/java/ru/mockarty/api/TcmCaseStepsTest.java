// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TcmCaseStepsTest {
    @Test
    void readEditSavePreservesVersionAndDedupResponse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> bodies = new ArrayList<>();
        AtomicInteger saves = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "/api/v1/namespaces/qa/test-cases/case-1/versions";
        server.createContext(base, exchange -> {
            String path = exchange.getRequestURI().getPath();
            String response;
            int status = 200;
            if (exchange.getRequestMethod().equals("POST") && path.equals(base)) {
                bodies.add(mapper.readTree(exchange.getRequestBody()));
                int n = saves.incrementAndGet();
                status = n == 1 ? 201 : 200;
                response = "{\"id\":\"version-2\",\"version\":2,\"dedupSkipped\":" + (n > 1) + "}";
            } else if (path.equals(base)) {
                response = "{\"versions\":[{\"id\":\"version-1\"}]}";
            } else if (path.equals(base+"/version-1")) {
                response = "{\"id\":\"version-1\",\"steps\":[{\"stepUid\":\"login\",\"mode\":\"manual\",\"executorType\":\"manual\",\"action\":\"Login\",\"expectedResult\":\"Home\",\"expectedAttachmentId\":\"3b41b6d0-7f0b-49cb-9b69-f7007e00c2b4\",\"executorConfig\":{\"kind\":\"inline_copy\",\"sourceSharedCopyInstanceId\":\"captured-instance\"}}]}";
            } else { status = 404; response = "{}"; }
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try (MockartyClient client = MockartyClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).namespace("qa").build()) {
            TcmApi api = client.tcm();
            assertEquals(1, api.listCaseVersions(null, "case-1").size());
            JsonNode version = api.getCaseVersion(null, "case-1", "version-1");
            Map<String,Object> step = mapper.convertValue(version.path("steps").get(0), new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {});
            step.put("expectedResult", "Dashboard");
            JsonNode first = api.saveCaseSteps(null,"case-1",List.of(step),"updated expectation");
            JsonNode again = api.saveCaseSteps(null,"case-1",List.of(step),"updated expectation");
            assertEquals(first.path("id"),again.path("id"));
            assertTrue(again.path("dedupSkipped").asBoolean());
            assertEquals("updated expectation",bodies.get(0).path("notes").asText());
            assertEquals("Dashboard",bodies.get(0).path("steps").get(0).path("expectedResult").asText());
            assertEquals("3b41b6d0-7f0b-49cb-9b69-f7007e00c2b4",bodies.get(0).path("steps").get(0).path("expectedAttachmentId").asText());
            assertEquals("captured-instance",bodies.get(0).path("steps").get(0).path("executorConfig").path("sourceSharedCopyInstanceId").asText());
            assertThrows(IllegalArgumentException.class,()->api.saveCaseSteps(null,"case-1",null,""));
        } finally { server.stop(0); }
    }
}
