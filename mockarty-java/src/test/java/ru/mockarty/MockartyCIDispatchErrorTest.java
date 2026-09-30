// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.exception.MockartyApiException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MockartyCIDispatchErrorTest {
    @Test void uncertainLaunch() throws Exception { assertSavedLaunch(409); }
    @Test void refusedLaunch() throws Exception { assertSavedLaunch(503); }

    private void assertSavedLaunch(int status) throws Exception {
        String reason = status == 409 ? "ci_dispatch_uncertain" : "ci_dispatch_failed";
        String code = status == 409 ? "conflict" : "unavailable";
        String envelope = "{\"error\":\"inspect saved launch\",\"code\":\"" + code
                + "\",\"reason\":\"" + reason + "\",\"runId\":\"saved-run\",\"request_id\":\"request-1\"}";
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = serve(status, envelope, hits);
        try (MockartyClient client = client(server, 2)) {
            MockartyApiException error = assertThrows(MockartyApiException.class,
                    () -> client.testPlans().run("plan", null, null));
            assertEquals(1, hits.get());
            assertEquals(reason, error.getReason());
            assertEquals("saved-run", error.getRunId());
            assertEquals("request-1", error.getRequestId());
            assertEquals(code, error.getCode());
        } finally { server.stop(0); }
    }

    @Test void ordinaryUnavailablePOSTStillRetries() throws Exception {
        for (String envelope : new String[]{"null", "[]", "{\"error\":\"transient\"}",
                "{\"reason\":\"ci_dispatch_failed\"}", "{\"reason\":23,\"runId\":{}}"}) {
            AtomicInteger hits = new AtomicInteger();
            HttpServer server = serve(503, envelope, hits);
            try (MockartyClient client = client(server, 1)) {
                assertThrows(MockartyApiException.class,
                        () -> client.testPlans().run("plan", null, null));
                assertEquals(2, hits.get());
            } finally { server.stop(0); }
        }
    }

    @Test void legacyConstructorsKeepDefaults() {
        MockartyApiException error = new MockartyApiException(503, "old error", "{}", "unavailable", "request-1");
        assertNull(error.getReason());
        assertNull(error.getRunId());
        assertEquals("request-1", error.getRequestId());
    }

    private HttpServer serve(int status, String envelope, AtomicInteger hits) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/test-plans/plan/run", exchange -> {
            hits.incrementAndGet();
            byte[] body = envelope.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
        return server;
    }

    private MockartyClient client(HttpServer server, int retries) {
        return MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .maxRetries(retries).build();
    }
}
