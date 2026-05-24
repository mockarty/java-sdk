// Copyright (c) 2026 Mockarty. All rights reserved.

package ru.mockarty.tester;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalStep;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExternalRunBridgeTest {

    private HttpServer server;
    private String base;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    @Test
    void happyPathMapsAllFields() {
        server.createContext("/", (HttpExchange ex) -> {
            ex.getResponseHeaders().add("Content-Type", "application/json");
            byte[] out = "{\"id\":42,\"name\":\"Alice\"}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, out.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(out); }
        });

        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/users/42")
                .expectStatus(200)
                .expectJsonPath("$.name", "Alice");
        t.finish();

        ExternalRunRequest req = ExternalRunBridge.toExternalRunRequest(t,
                new ExternalRunBridge.Options()
                        .caseName("users/get")
                        .testDisplayName("GET /users/42")
                        .framework("custom-runner")
                        .labels(Map.of("suite", "smoke"))
                        .autoCreate(true));

        assertEquals(ExternalRunRequest.STATUS_PASSED, req.getStatus());
        assertEquals("users/get", req.getCaseName());
        assertEquals("custom-runner", req.getFramework());
        assertTrue(req.isAutoCreate());
        assertNotNull(req.getStartedAt());
        assertNotNull(req.getFinishedAt());
        assertEquals(1, req.getSteps().size());
        ExternalStep step = req.getSteps().get(0);
        assertEquals(ExternalRunRequest.STATUS_PASSED, step.getStatus());
        Map<String, Object> meta = step.getMetadata();
        assertEquals("http", meta.get("protocol"));
        assertEquals("GET", meta.get("method"));
        assertEquals(200, meta.get("statusOrCode"));
    }

    @Test
    void failureCarriesErrorMessage() {
        server.createContext("/", ex -> {
            ex.sendResponseHeaders(500, -1);
            ex.close();
        });
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/").expectStatus(200);
        t.finish();

        ExternalRunRequest req = ExternalRunBridge.toExternalRunRequest(t,
                new ExternalRunBridge.Options().caseName("x"));
        assertEquals(ExternalRunRequest.STATUS_FAILED, req.getStatus());
        assertNotNull(req.getError());
        assertEquals(1, req.getSteps().size());
        assertEquals(ExternalRunRequest.STATUS_FAILED,
                req.getSteps().get(0).getStatus());
        assertNotNull(req.getSteps().get(0).getError());
    }

    @Test
    void emptyTesterEmitsRunOnly() {
        Tester t = new Tester.Builder().build();
        t.finish();
        ExternalRunRequest req = ExternalRunBridge.toExternalRunRequest(t,
                new ExternalRunBridge.Options().caseName("empty"));
        assertEquals(ExternalRunRequest.STATUS_PASSED, req.getStatus());
        // No steps means no timestamps either (NON_DEFAULT JsonInclude
        // omits them from the wire payload).
        assertNull(req.getSteps());
        assertNull(req.getStartedAt());
        assertNull(req.getFinishedAt());
        assertEquals("mockarty-tester-java", req.getFramework());
    }

    @Test
    void multipleFailuresJoinedWithSeparator() {
        server.createContext("/", ex -> {
            byte[] body = "{\"id\":1}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/")
                .expectStatus(204)
                .expectJsonPath("$.id", 99)
                .expectJsonPath("$.missing", "x");
        t.finish();

        ExternalRunRequest req = ExternalRunBridge.toExternalRunRequest(t,
                new ExternalRunBridge.Options().caseName("multi"));
        assertEquals(ExternalRunRequest.STATUS_FAILED, req.getStatus());
        assertEquals(1, req.getSteps().size());
        ExternalStep step = req.getSteps().get(0);
        assertEquals(ExternalRunRequest.STATUS_FAILED, step.getStatus());
        assertTrue(step.getError().contains("; "),
                "failures joined with separator");
    }

    @Test
    void optionsLabelsMetadataAndCaseIdPassThrough() {
        Tester t = new Tester.Builder().build();
        t.finish();
        Map<String, Object> md = new HashMap<>();
        md.put("git_sha", "abc123");
        md.put("ci_url", "https://ci/build/42");
        ExternalRunRequest req = ExternalRunBridge.toExternalRunRequest(t,
                new ExternalRunBridge.Options()
                        .caseId("case-uuid")
                        .labels(Map.of("feature", "auth", "severity", "critical"))
                        .metadata(md)
                        .claimCaseOwnership(true));
        assertEquals("case-uuid", req.getCaseId());
        assertEquals("auth", req.getLabels().get("feature"));
        assertEquals("abc123", req.getMetadata().get("git_sha"));
        // claim_case_ownership maps to a field not yet on the Java model
        // — the Options carries it but the model omission means it
        // round-trips through the wire only on Py/Go today. Document
        // by asserting current behaviour: req has no ClaimCaseOwnership
        // getter so the call-site at least compiles.
    }

    @Test
    void defaultFrameworkNameWhenNotOverridden() {
        Tester t = new Tester.Builder().build();
        t.finish();
        ExternalRunRequest req = ExternalRunBridge.toExternalRunRequest(t,
                new ExternalRunBridge.Options().caseName("x"));
        assertEquals("mockarty-tester-java", req.getFramework());
    }

    @Test
    void chainOrderPreservedInSteps() {
        server.createContext("/a", ex -> { ex.sendResponseHeaders(200, -1); ex.close(); });
        server.createContext("/b", ex -> { ex.sendResponseHeaders(200, -1); ex.close(); });
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/a").expectStatus(200);
        t.http().get("/b").expectStatus(200);
        t.finish();

        ExternalRunRequest req = ExternalRunBridge.toExternalRunRequest(t,
                new ExternalRunBridge.Options().caseName("multi-step"));
        List<ExternalStep> steps = req.getSteps();
        assertEquals(2, steps.size());
        assertTrue(steps.get(0).getName().endsWith("/a"));
        assertTrue(steps.get(1).getName().endsWith("/b"));
    }
}
