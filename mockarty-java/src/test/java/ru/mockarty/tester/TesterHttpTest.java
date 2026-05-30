// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Smoke tests for the Java port of the fluent Tester DSL. */
public class TesterHttpTest {

    private HttpServer server;
    private String base;
    private final Map<String, RouteHandler> routes = new HashMap<>();

    @FunctionalInterface
    interface RouteHandler { void handle(HttpExchange ex) throws IOException; }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            RouteHandler h = routes.get(ex.getRequestMethod() + " " + ex.getRequestURI().getPath());
            if (h == null) {
                ex.sendResponseHeaders(404, -1);
                ex.close();
                return;
            }
            h.handle(ex);
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    private void route(String method, String path, RouteHandler h) {
        routes.put(method + " " + path, h);
    }

    private static void writeJson(HttpExchange ex, int status, String body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    @Test
    void httpGetExpectStatusJson() {
        route("GET", "/users/42", ex -> writeJson(ex, 200, "{\"id\":42,\"name\":\"Alice\"}"));

        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/users/42")
                .expectStatus(200)
                .expectJsonPath("$.name", "Alice")
                .expectJsonPath("$.id", 42)
                .extract("$.name", "user");
        t.finish();
        assertTrue(t.ok(), () -> "errors: " + t.errors());
        assertEquals("Alice", t.vars().get("user"));
    }

    @Test
    void httpChainExtractAndInterpolate() {
        route("GET", "/login", ex -> writeJson(ex, 200, "{\"token\":\"tok-123\"}"));
        AtomicReference<String> authHeader = new AtomicReference<>();
        route("POST", "/orders", ex -> {
            authHeader.set(ex.getRequestHeaders().getFirst("X-Auth"));
            writeJson(ex, 201, "{\"id\":99}");
        });

        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/login").expectStatus(200).extract("$.token", "token");
        t.http().post("/orders")
                .header("X-Auth", "Bearer {{token}}")
                .json(Map.of("userId", 42))
                .expectStatus(201);
        t.finish();
        assertTrue(t.ok(), () -> "errors: " + t.errors());
        assertEquals("Bearer tok-123", authHeader.get());
    }

    @Test
    void failingAssertionsAccumulate() {
        route("GET", "/x", ex -> writeJson(ex, 200, "{\"id\":1}"));

        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/x")
                .expectStatus(204)
                .expectJsonPath("$.id", 99);
        t.finish();
        assertFalse(t.ok());
        assertEquals(2, t.errors().size());
    }

    @Test
    void allMethodsRouted() {
        for (String verb : List.of("PUT", "PATCH", "DELETE", "HEAD")) {
            route(verb, "/x", ex -> {
                ex.getResponseHeaders().add("X-Method", ex.getRequestMethod());
                ex.sendResponseHeaders(200, -1);
                ex.close();
            });
        }
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().put("/x").expectHeader("X-Method", "PUT");
        t.http().patch("/x").expectHeader("X-Method", "PATCH");
        t.http().delete("/x").expectHeader("X-Method", "DELETE");
        t.http().head("/x").expectHeader("X-Method", "HEAD");
        t.finish();
        assertTrue(t.ok(), () -> "errors: " + t.errors());
    }

    @Test
    void autoFlushOnOk() {
        route("GET", "/", ex -> writeJson(ex, 204, ""));
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/").expectStatus(200); // mismatch, no finish()
        assertFalse(t.ok(), "OK() should auto-flush");
        assertEquals(1, t.errors().size());
    }

    @Test
    void graphqlHappyPath() {
        route("POST", "/graphql", ex -> writeJson(ex, 200,
                "{\"data\":{\"user\":{\"id\":42,\"name\":\"Alice\"}}}"));

        Tester t = new Tester.Builder().baseUrl(base).build();
        t.graphql("/graphql")
                .query("{ user(id: 42) { name } }", Map.of("id", 42))
                .expectStatus(200)
                .expectNoErrors()
                .expectField("$.data.user.name", "Alice")
                .extract("$.data.user.name", "user");
        t.finish();
        assertTrue(t.ok(), () -> "errors: " + t.errors());
        assertEquals("Alice", t.vars().get("user"));
    }

    @Test
    void interpolateUnknownStaysLiteral() {
        Map<String, String> v = new HashMap<>();
        v.put("a", "X");
        assertEquals("hi X", Interpolate.apply("hi {{a}}", v));
        assertEquals("hi {{missing}}", Interpolate.apply("hi {{missing}}", v));
        assertEquals("plain", Interpolate.apply("plain", v));
    }

    @Test
    void autoCloseFlushesPending() throws Exception {
        route("GET", "/", ex -> writeJson(ex, 200, "{}"));
        try (Tester t = new Tester.Builder().baseUrl(base).build()) {
            t.http().get("/").expectStatus(200);
            // try-with-resources triggers close() → finish() → flush.
        }
        // No assertion fail here — if try-with-resources broke the
        // step record would not commit and the previous chain would
        // leak as an in-flight unrecorded request.
    }

    @Test
    void expectBodyContains() {
        route("GET", "/x", ex -> writeJson(ex, 200, "{\"msg\":\"hello world\"}"));
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/x").expectBodyContains("hello");
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
    }

    @Test
    void expectJsonArrayLen() {
        route("GET", "/x", ex -> writeJson(ex, 200, "{\"items\":[1,2,3,4]}"));
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/x")
                .expectJsonPath("$.items[0]", 1)
                .expectJsonPath("$.items[-1]", 4)
                .expectJsonArrayLen("$.items", 4);
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
    }

    @Test
    void builderHeaderAppliesToEveryStep() {
        AtomicReference<String> hdr = new AtomicReference<>();
        route("GET", "/", ex -> {
            hdr.set(ex.getRequestHeaders().getFirst("X-Default"));
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        Tester t = new Tester.Builder()
                .baseUrl(base)
                .header("X-Default", "global")
                .build();
        t.http().get("/").expectStatus(200);
        t.finish();
        assertEquals("global", hdr.get());
    }

    @Test
    void failFastSkipsSubsequentSteps() {
        route("GET", "/a", ex -> { ex.sendResponseHeaders(500, -1); ex.close(); });
        AtomicReference<Boolean> bCalled = new AtomicReference<>(false);
        route("GET", "/b", ex -> {
            bCalled.set(true);
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        Tester t = new Tester.Builder().baseUrl(base).failFast().build();
        t.http().get("/a").expectStatus(200);
        t.http().get("/b").expectStatus(200);
        t.finish();
        assertFalse(t.ok());
        assertFalse(bCalled.get(), "fail-fast should skip second call");
    }

    @Test
    void graphqlErrorsArray() {
        route("POST", "/g", ex -> writeJson(ex, 200,
                "{\"data\":null,\"errors\":[{\"message\":\"boom\"},{\"message\":\"again\"}]}"));
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.graphql("/g")
                .query("X", null)
                .expectErrors(2)
                .expectField("$.errors[0].message", "boom");
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
    }

    @Test
    void reportContainsStepsInChainOrder() {
        route("GET", "/a", ex -> writeJson(ex, 200, "{}"));
        route("GET", "/b", ex -> writeJson(ex, 200, "{}"));
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.http().get("/a").expectStatus(200);
        t.http().get("/b").expectStatus(200);
        t.finish();
        assertEquals(2, t.report().size());
        assertTrue(t.report().get(0).name.endsWith("/a"));
        assertTrue(t.report().get(1).name.endsWith("/b"));
    }

    @Test
    void jsonPathBasic() {
        Object doc = new com.fasterxml.jackson.databind.ObjectMapper()
                .convertValue(Map.of("a", Map.of("b", List.of("x", "y", 3))), Object.class);
        assertEquals("x", JsonPath.resolve(doc, "$.a.b[0]"));
        assertEquals(3, ((Number) JsonPath.resolve(doc, "$.a.b[-1]")).intValue());
        try {
            JsonPath.resolve(doc, "$.a.z");
            fail("expected JsonPathException");
        } catch (JsonPathException expected) { /* ok */ }
    }
}
