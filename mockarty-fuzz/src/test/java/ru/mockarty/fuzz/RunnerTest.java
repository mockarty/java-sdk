// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link Runner} against an in-process {@code HttpServer} stub.
 *
 * <p>We bind 127.0.0.1 explicitly (not localhost) per the IPv6 caveat in
 * the project's testing rules. The server runs on an ephemeral port so
 * parallel test runs don't fight over a fixed slot.</p>
 */
class RunnerTest {

    private HttpServer server;
    private String baseUrl;
    private final List<String> recordedPaths = new ArrayList<>();
    private final List<String> recordedBodies = new ArrayList<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private void register(String path, HttpHandler handler) {
        server.createContext(path, ex -> {
            recordedPaths.add(ex.getRequestMethod() + " " + ex.getRequestURI().getPath());
            recordedBodies.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            handler.handle(ex);
        });
    }

    private void boot() {
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respondJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static Target sampleTarget() {
        return Target.named("x")
                .httpEndpoint("POST", "/login")
                .seeds(Seed.of("s", "{}"))
                .mutator(Mutator.JSON)
                .build();
    }

    @Test
    void submitReturnsJobId() throws Exception {
        register("/api/v1/fuzzing/run", ex -> respondJson(ex, 200, "{\"id\":\"run-42\"}"));
        boot();
        try (Runner r = Runner.of(baseUrl, "default", "tok-abc")) {
            JobId job = r.submit(sampleTarget());
            assertEquals("run-42", job.value());
            assertEquals("POST /api/v1/fuzzing/run", recordedPaths.get(0));
            assertTrue(recordedBodies.get(0).contains("\"name\":\"x\""));
        }
    }

    @Test
    void submitFallsBackToRunIdField() throws Exception {
        register("/api/v1/fuzzing/run", ex -> respondJson(ex, 200, "{\"runId\":\"legacy-1\"}"));
        boot();
        try (Runner r = Runner.of(baseUrl, "default", "tok")) {
            assertEquals("legacy-1", r.submit(sampleTarget()).value());
        }
    }

    @Test
    void submitNon2xxThrows() throws Exception {
        register("/api/v1/fuzzing/run", ex -> respondJson(ex, 500, "{\"error\":\"boom\"}"));
        boot();
        try (Runner r = Runner.of(baseUrl, "default", "tok")) {
            IOException ex = assertThrows(IOException.class, () -> r.submit(sampleTarget()));
            assertTrue(ex.getMessage().contains("HTTP 500"));
        }
    }

    @Test
    void waitPollsUntilTerminal() throws Exception {
        AtomicInteger polls = new AtomicInteger();
        register("/api/v1/fuzzing/results/", ex -> {
            int n = polls.incrementAndGet();
            String body = (n < 3)
                    ? "{\"id\":\"r1\",\"status\":\"running\"}"
                    : "{\"id\":\"r1\",\"status\":\"completed\",\"totalRequests\":42,"
                        + "\"totalFindings\":1,\"criticalFindings\":0,\"highFindings\":1,"
                        + "\"findings\":[{\"id\":\"f1\",\"severity\":\"high\",\"category\":\"xss\","
                        + "\"title\":\"reflected\",\"responseStatus\":200}]}";
            respondJson(ex, 200, body);
        });
        boot();
        try (Runner r = Runner.builder()
                .adminUrl(baseUrl).namespace("default").apiToken("tok")
                .pollInterval(Duration.ofMillis(10))
                .pollTimeout(Duration.ofSeconds(5))
                .build()) {
            Result result = r.waitFor(new JobId("r1"));
            assertEquals("completed", result.status());
            assertEquals(42, result.totalRequests());
            assertEquals(1, result.highFindings());
            assertEquals(1, result.findings().size());
            assertEquals("xss", result.findings().get(0).category());
            assertTrue(polls.get() >= 3);
        }
    }

    @Test
    void waitTimesOut() throws Exception {
        register("/api/v1/fuzzing/results/",
                ex -> respondJson(ex, 200, "{\"id\":\"r1\",\"status\":\"running\"}"));
        boot();
        try (Runner r = Runner.builder()
                .adminUrl(baseUrl).namespace("default").apiToken("tok")
                .pollInterval(Duration.ofMillis(20))
                .pollTimeout(Duration.ofMillis(80))
                .build()) {
            IOException ex = assertThrows(IOException.class, () -> r.waitFor(new JobId("r1")));
            assertTrue(ex.getMessage().contains("timeout"));
        }
    }

    @Test
    void waitTreats404AsQueued() throws Exception {
        AtomicInteger polls = new AtomicInteger();
        register("/api/v1/fuzzing/results/", ex -> {
            int n = polls.incrementAndGet();
            if (n < 2) {
                respondJson(ex, 404, "{}");
            } else {
                respondJson(ex, 200, "{\"id\":\"r1\",\"status\":\"completed\"}");
            }
        });
        boot();
        try (Runner r = Runner.builder()
                .adminUrl(baseUrl).namespace("default").apiToken("tok")
                .pollInterval(Duration.ofMillis(10))
                .pollTimeout(Duration.ofSeconds(2))
                .build()) {
            Result result = r.waitFor(new JobId("r1"));
            assertEquals("completed", result.status());
        }
    }

    @Test
    void streamYieldsProgressFindingsAndCompleted() throws Exception {
        CountDownLatch handlerStarted = new CountDownLatch(1);
        register("/api/v1/fuzzing/run/r1/events", ex -> {
            handlerStarted.countDown();
            ex.getResponseHeaders().add("Content-Type", "text/event-stream");
            ex.sendResponseHeaders(200, 0);
            try (OutputStream os = ex.getResponseBody()) {
                os.write("data: {\"type\":\"progress\",\"completedRequests\":5,\"totalRequests\":100}\n\n"
                        .getBytes(StandardCharsets.UTF_8));
                os.write("data: {\"type\":\"finding\",\"finding\":{\"id\":\"f1\",\"severity\":\"high\",\"category\":\"xss\"}}\n\n"
                        .getBytes(StandardCharsets.UTF_8));
                os.write("data: {\"type\":\"completed\",\"result\":{\"id\":\"r1\",\"status\":\"completed\",\"totalFindings\":1}}\n\n"
                        .getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });
        boot();
        try (Runner r = Runner.of(baseUrl, "default", "tok")) {
            List<Event> events = r.stream(new JobId("r1"))
                    .collect(Collectors.toList());
            assertTrue(handlerStarted.await(2, TimeUnit.SECONDS));
            assertEquals(3, events.size(), () -> "got events: " + events);
            assertTrue(events.get(0) instanceof Event.Progress);
            assertTrue(events.get(1) instanceof Event.FindingFound);
            assertTrue(events.get(2) instanceof Event.Completed);

            Event.Progress p = (Event.Progress) events.get(0);
            assertEquals(5, p.completedRequests());
            Event.FindingFound f = (Event.FindingFound) events.get(1);
            assertEquals("xss", f.finding().category());
            Event.Completed c = (Event.Completed) events.get(2);
            assertEquals("completed", c.result().status());
        }
    }

    @Test
    void streamIgnoresCommentsAndBareJson() throws Exception {
        register("/api/v1/fuzzing/run/r1/events", ex -> {
            ex.getResponseHeaders().add("Content-Type", "text/event-stream");
            ex.sendResponseHeaders(200, 0);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(": keep-alive ping\n".getBytes(StandardCharsets.UTF_8));
                os.write("\n".getBytes(StandardCharsets.UTF_8));
                // Bare JSON, no `data:` prefix — some proxies strip it.
                os.write("{\"type\":\"progress\",\"completedRequests\":1}\n".getBytes(StandardCharsets.UTF_8));
                os.write("data: not-valid-json\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });
        boot();
        try (Runner r = Runner.of(baseUrl, "default", "tok")) {
            List<Event> events = r.stream(new JobId("r1")).collect(Collectors.toList());
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof Event.Progress);
        }
    }

    @Test
    void stopPostsToStopEndpoint() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        register("/api/v1/fuzzing/run/r1/stop", ex -> {
            hits.incrementAndGet();
            respondJson(ex, 200, "{}");
        });
        boot();
        try (Runner r = Runner.of(baseUrl, "default", "tok")) {
            r.stop(new JobId("r1"));
            assertEquals(1, hits.get());
        }
    }

    @Test
    void submitWithoutAdminUrlThrows() {
        try (Runner r = Runner.builder().build()) {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> r.submit(sampleTarget()));
            assertTrue(ex.getMessage().contains("adminUrl"));
        }
    }

    @Test
    void authHeadersAreSent() throws Exception {
        register("/api/v1/fuzzing/run", ex -> {
            assertEquals("tok-zzz", ex.getRequestHeaders().getFirst("X-API-Key"));
            assertEquals("ns-1", ex.getRequestHeaders().getFirst("X-Namespace"));
            respondJson(ex, 200, "{\"id\":\"run-1\"}");
        });
        boot();
        try (Runner r = Runner.of(baseUrl, "ns-1", "tok-zzz")) {
            assertNotNull(r.submit(sampleTarget()));
        }
    }

    @Test
    void waitOverloadAliasesWaitFor() throws Exception {
        register("/api/v1/fuzzing/results/",
                ex -> respondJson(ex, 200, "{\"id\":\"r1\",\"status\":\"completed\"}"));
        boot();
        try (Runner r = Runner.builder()
                .adminUrl(baseUrl).namespace("ns").apiToken("tok")
                .pollInterval(Duration.ofMillis(5))
                .pollTimeout(Duration.ofSeconds(2))
                .build()) {
            Result a = r.wait(new JobId("r1"));
            Result b = r.waitFor(new JobId("r1"));
            assertEquals(a.status(), b.status());
        }
    }
}
