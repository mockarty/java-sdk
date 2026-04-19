// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.PlanRunCancelledException;
import ru.mockarty.exception.PlanRunFailedException;
import ru.mockarty.exception.PreconditionFailedException;
import ru.mockarty.exception.WebhookDeliveryException;
import ru.mockarty.model.AdHocItem;
import ru.mockarty.model.AdHocRunResponse;
import ru.mockarty.model.AllureReport;
import ru.mockarty.model.CreateAdHocRunRequest;
import ru.mockarty.model.PatchOptions;
import ru.mockarty.model.PatchPlanRequest;
import ru.mockarty.model.PlanRunStatus;
import ru.mockarty.model.PlanSchedule;
import ru.mockarty.model.PlanWebhook;
import ru.mockarty.model.RunEvent;
import ru.mockarty.model.TestPlan;
import ru.mockarty.model.TestPlanItem;
import ru.mockarty.model.TestPlanRun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPlanApiTest {

    private HttpServer server;
    private MockartyClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("test-key")
                .namespace("test-ns")
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

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    @DisplayName("create returns a populated plan")
    void create() {
        respond("/api/v1/test-plans",
                201,
                "{\"id\":\"plan-1\",\"numericId\":42,\"namespace\":\"test-ns\"," +
                        "\"name\":\"Smoke\",\"items\":[{\"order\":0,\"type\":\"functional\",\"refId\":\"c1\"}]}");

        TestPlan req = new TestPlan()
                .name("Smoke")
                .items(List.of(new TestPlanItem().order(0).type("functional").resourceId("c1")));
        TestPlan created = client.testPlans().create(req);
        assertEquals("plan-1", created.getId());
        assertEquals(42L, created.getNumericId());
        assertEquals("c1", created.getItems().get(0).getResourceId());
    }

    @Test
    @DisplayName("get normalises the hash prefix on numeric ids")
    void getStripsHashPrefix() {
        AtomicInteger hits = new AtomicInteger();
        server.createContext("/api/v1/test-plans/42", exchange -> {
            hits.incrementAndGet();
            String body = "{\"id\":\"plan-1\",\"numericId\":42,\"name\":\"x\",\"items\":[]}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes());
            }
        });
        TestPlan plan = client.testPlans().get("#42");
        assertEquals("plan-1", plan.getId());
        assertEquals(1, hits.get());
    }

    @Test
    @DisplayName("run returns the compact runId payload")
    void run() {
        respond("/api/v1/test-plans/plan-1/run",
                202,
                "{\"runId\":\"r1\",\"planId\":\"plan-1\",\"status\":\"pending\"}");
        TestPlanRun run = client.testPlans().run("plan-1", null, null);
        assertEquals("r1", run.getId());
        assertEquals("plan-1", run.getPlanId());
        assertEquals("pending", run.getStatus());
    }

    @Test
    @DisplayName("getRun parses itemsState list")
    void getRun() {
        respond("/api/v1/test-plans/runs/r1",
                200,
                "{\"id\":\"r1\",\"status\":\"completed\",\"totalItems\":2,\"completedItems\":2," +
                        "\"failedItems\":0,\"itemsState\":[{\"order\":0,\"type\":\"functional\",\"status\":\"passed\"}]}");
        TestPlanRun run = client.testPlans().getRun("r1");
        assertEquals("completed", run.getStatus());
        assertEquals(2, run.getTotalItems());
        assertNotNull(run.getItemsState());
        assertEquals("passed", run.getItemsState().get(0).getStatus());
    }

    @Test
    @DisplayName("getRunStatus reads compact envelope")
    void getRunStatus() {
        respond("/api/v1/test-plans/runs/r1/status",
                200,
                "{\"status\":\"running\",\"totalItems\":3,\"completedItems\":1,\"failedItems\":0}");
        PlanRunStatus status = client.testPlans().getRunStatus("r1");
        assertEquals("running", status.getStatus());
        assertEquals(3, status.getTotalItems());
    }

    @Test
    @DisplayName("waitForRun completes on terminal pass")
    void waitForRunSuccess() {
        respond("/api/v1/test-plans/runs/r1",
                200,
                "{\"id\":\"r1\",\"status\":\"completed\"}");
        TestPlanRun run = client.testPlans().waitForRun("r1", Duration.ofMillis(10), null);
        assertEquals("completed", run.getStatus());
    }

    @Test
    @DisplayName("waitForRun throws on failed")
    void waitForRunFailed() {
        respond("/api/v1/test-plans/runs/r1",
                200,
                "{\"id\":\"r1\",\"status\":\"failed\"}");
        assertThrows(PlanRunFailedException.class,
                () -> client.testPlans().waitForRun("r1", Duration.ofMillis(10), null));
    }

    @Test
    @DisplayName("waitForRun throws on cancelled")
    void waitForRunCancelled() {
        respond("/api/v1/test-plans/runs/r1",
                200,
                "{\"id\":\"r1\",\"status\":\"cancelled\"}");
        assertThrows(PlanRunCancelledException.class,
                () -> client.testPlans().waitForRun("r1", Duration.ofMillis(10), null));
    }

    @Test
    @DisplayName("cancelRun hits the cancel endpoint")
    void cancelRun() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/api/v1/test-plans/runs/r1/cancel", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        client.testPlans().cancelRun("r1");
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("downloadReportZip writes into the destination stream")
    void downloadReportZip() {
        server.createContext("/api/v1/test-plans/runs/r1/report.zip", exchange -> {
            byte[] bytes = new byte[]{'P', 'K', 0x03, 0x04, 'x'};
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        client.testPlans().downloadReportZip("r1", buf);
        assertEquals(5, buf.size());
        assertEquals('P', buf.toByteArray()[0]);
    }

    @Test
    @DisplayName("streamRun parses SSE frames into RunEvent")
    void streamRun() {
        server.createContext("/api/v1/test-plans/runs/r1/stream", exchange -> {
            String sse = ": heartbeat\n" +
                    "event: run.started\n" +
                    "data: {\"runId\":\"r1\"}\n" +
                    "\n" +
                    "event: item.finished\n" +
                    "data: {\"order\":0,\"status\":\"passed\"}\n" +
                    "\n";
            byte[] bytes = sse.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        List<RunEvent> events = new ArrayList<>();
        client.testPlans().streamRun("r1", events::add);
        assertEquals(2, events.size());
        assertEquals("run.started", events.get(0).getKind());
        assertEquals("r1", events.get(0).getData().get("runId"));
        assertEquals("item.finished", events.get(1).getKind());
        assertEquals("passed", events.get(1).getData().get("status"));
    }

    @Test
    @DisplayName("addSchedule round-trips a cron rule")
    void addSchedule() {
        respond("/api/v1/test-plans/plan-1/schedules",
                201,
                "{\"id\":\"s1\",\"kind\":\"cron\",\"cronExpr\":\"0 2 * * *\",\"enabled\":true}");
        PlanSchedule created = client.testPlans().addSchedule("plan-1",
                new PlanSchedule().kind("cron").cronExpr("0 2 * * *"));
        assertEquals("s1", created.getId());
        assertEquals("0 2 * * *", created.getCronExpr());
    }

    @Test
    @DisplayName("listSchedules unwraps the items envelope")
    void listSchedules() {
        respond("/api/v1/test-plans/plan-1/schedules",
                200,
                "{\"items\":[{\"id\":\"s1\",\"kind\":\"cron\"}],\"count\":1}");
        List<PlanSchedule> schedules = client.testPlans().listSchedules("plan-1");
        assertEquals(1, schedules.size());
        assertEquals("cron", schedules.get(0).getKind());
    }

    @Test
    @DisplayName("addWebhook accepts secret plaintext")
    void addWebhook() {
        respond("/api/v1/test-plans/plan-1/webhooks",
                201,
                "{\"id\":\"w1\",\"url\":\"https://ci/hook\",\"events\":[\"run.completed\"],\"enabled\":true}");
        PlanWebhook created = client.testPlans().addWebhook("plan-1",
                new PlanWebhook().url("https://ci/hook")
                        .events(List.of("run.completed"))
                        .secret("s3cr3t"));
        assertEquals("w1", created.getId());
    }

    @Test
    @DisplayName("testWebhook throws when the server reports failure")
    void testWebhookFailure() {
        respond("/api/v1/test-plans/plan-1/webhooks/w1/test",
                200,
                "{\"success\":false,\"status\":500,\"error\":\"timeout\"}");
        assertThrows(WebhookDeliveryException.class,
                () -> client.testPlans().testWebhook("plan-1", "w1"));
    }

    @Test
    @DisplayName("testWebhook succeeds silently on success=true")
    void testWebhookSuccess() {
        respond("/api/v1/test-plans/plan-1/webhooks/w1/test",
                200,
                "{\"success\":true,\"status\":200}");
        // should not throw
        client.testPlans().testWebhook("plan-1", "w1");
        assertTrue(true);
    }

    @Test
    @DisplayName("delete calls DELETE on the plan endpoint")
    void deletePlan() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/api/v1/test-plans/plan-1", exchange -> {
            if ("DELETE".equals(exchange.getRequestMethod())) {
                calls.incrementAndGet();
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        });
        client.testPlans().delete("plan-1");
        assertEquals(1, calls.get());
    }

    // ── TP-6b: namespace-scoped patch / ad-hoc run / reports ─────────

    @Test
    @DisplayName("patch uses supplied ifMatch etag and default namespace")
    void patchWithExplicitEtag() {
        AtomicReference<String> seenEtag = new AtomicReference<>();
        AtomicReference<String> seenBody = new AtomicReference<>();
        server.createContext("/api/v1/namespaces/test-ns/test-plans/plan-1",
                exchange -> {
                    seenEtag.set(exchange.getRequestHeaders().getFirst("If-Match"));
                    seenBody.set(new String(exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8));
                    String body = "{\"id\":\"plan-1\",\"name\":\"Renamed\"," +
                            "\"items\":[],\"updatedAt\":\"2026-04-19T12:00:01Z\"}";
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        TestPlan patched = client.testPlans().patch("plan-1",
                new PatchPlanRequest().name("Renamed").enabled(true),
                PatchOptions.of("\"1234\""));
        assertEquals("Renamed", patched.getName());
        assertEquals("\"1234\"", seenEtag.get());
        assertTrue(seenBody.get().contains("\"name\":\"Renamed\""));
        assertTrue(seenBody.get().contains("\"enabled\":true"));
    }

    @Test
    @DisplayName("patch pre-fetches etag when none is supplied")
    void patchAutoEtag() {
        AtomicReference<String> seenEtag = new AtomicReference<>();
        AtomicInteger getCalls = new AtomicInteger();

        // Legacy GET (for pre-fetch) returns updatedAt = 2026-04-19T12:00:00Z
        // → UnixMilli = 1745064000000.
        server.createContext("/api/v1/test-plans/plan-1", exchange -> {
            getCalls.incrementAndGet();
            String body = "{\"id\":\"plan-1\",\"name\":\"x\",\"items\":[]," +
                    "\"updatedAt\":\"2026-04-19T12:00:00Z\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes());
            }
        });

        // PATCH path is namespace-scoped — different context path.
        server.createContext("/api/v1/namespaces/test-ns/test-plans/plan-1",
                exchange -> {
                    seenEtag.set(exchange.getRequestHeaders().getFirst("If-Match"));
                    String body = "{\"id\":\"plan-1\",\"name\":\"Renamed\",\"items\":[]}";
                    byte[] bytes = body.getBytes();
                    exchange.getResponseHeaders().set("Content-Type",
                            "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        TestPlan patched = client.testPlans().patch("plan-1",
                new PatchPlanRequest().name("Renamed"), null);
        assertNotNull(patched);
        assertTrue(getCalls.get() >= 1, "expected pre-fetch GET call");
        assertNotNull(seenEtag.get());
        // Millis for 2026-04-19T12:00:00Z = 1776225600000 (sanity check
        // only; we just need the SDK to emit a quoted decimal string).
        assertTrue(seenEtag.get().startsWith("\""),
                "etag should be quoted: " + seenEtag.get());
        assertTrue(seenEtag.get().endsWith("\""),
                "etag should be quoted: " + seenEtag.get());
    }

    @Test
    @DisplayName("patch throws PreconditionFailedException on 412")
    void patch412() {
        server.createContext("/api/v1/namespaces/test-ns/test-plans/plan-1",
                exchange -> {
                    String body = "{\"error\":\"etag mismatch\",\"code\":\"conflict\"}";
                    exchange.getResponseHeaders().set("ETag", "\"999\"");
                    exchange.sendResponseHeaders(412, body.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body.getBytes());
                    }
                });

        PreconditionFailedException ex = assertThrows(
                PreconditionFailedException.class,
                () -> client.testPlans().patch("plan-1",
                        new PatchPlanRequest().name("Renamed"),
                        PatchOptions.of("\"0\"")));
        assertEquals("\"999\"", ex.getCurrentEtag());
    }

    @Test
    @DisplayName("patch rejects an empty request body")
    void patchEmptyBodyRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> client.testPlans().patch("plan-1",
                        new PatchPlanRequest(),
                        PatchOptions.of("\"1\"")));
    }

    @Test
    @DisplayName("patch honours per-call namespace override")
    void patchNamespaceOverride() {
        AtomicInteger otherNs = new AtomicInteger();
        server.createContext("/api/v1/namespaces/other-ns/test-plans/plan-1",
                exchange -> {
                    otherNs.incrementAndGet();
                    String body = "{\"id\":\"plan-1\",\"name\":\"x\",\"items\":[]}";
                    exchange.getResponseHeaders().set("Content-Type",
                            "application/json");
                    exchange.sendResponseHeaders(200, body.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body.getBytes());
                    }
                });
        client.testPlans().patch("plan-1",
                new PatchPlanRequest().name("x"),
                new PatchOptions().ifMatch("\"1\"").namespace("other-ns"));
        assertEquals(1, otherNs.get());
    }

    @Test
    @DisplayName("createAdHocRun dispatches to the namespace-scoped endpoint")
    void createAdHocRun() {
        AtomicReference<String> seenBody = new AtomicReference<>();
        server.createContext("/api/v1/namespaces/test-ns/test-runs/ad-hoc",
                exchange -> {
                    seenBody.set(new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8));
                    String body = "{\"run_id\":\"r-ad-1\",\"plan_id\":\"p-ad-1\"," +
                            "\"status\":\"pending\",\"adhoc\":true," +
                            "\"_links\":{\"self\":\"/runs/r-ad-1\"}}";
                    byte[] bytes = body.getBytes();
                    exchange.getResponseHeaders().set("Content-Type",
                            "application/json");
                    exchange.sendResponseHeaders(202, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        CreateAdHocRunRequest req = new CreateAdHocRunRequest()
                .name("smoke")
                .schedule("parallel")
                .items(List.of(
                        new AdHocItem().refId("c1").type("functional").order(0),
                        new AdHocItem().refId("f1").type("fuzz").order(1)));
        AdHocRunResponse resp = client.testPlans().createAdHocRun(req);
        assertEquals("r-ad-1", resp.getRunId());
        assertEquals("p-ad-1", resp.getPlanId());
        assertEquals("pending", resp.getStatus());
        assertTrue(resp.getAdhoc());
        assertNotNull(resp.getLinks());
        assertEquals("/runs/r-ad-1", resp.getLinks().get("self"));
        assertTrue(seenBody.get().contains("\"ref_id\":\"c1\""));
        assertTrue(seenBody.get().contains("\"schedule\":\"parallel\""));
        // namespace must NOT be serialised into the JSON body (it's the
        // URL segment).
        assertTrue(!seenBody.get().contains("\"namespace\""));
    }

    @Test
    @DisplayName("createAdHocRun rejects empty items client-side")
    void createAdHocRunValidates() {
        assertThrows(IllegalArgumentException.class,
                () -> client.testPlans().createAdHocRun(
                        new CreateAdHocRunRequest().items(List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> client.testPlans().createAdHocRun(
                        new CreateAdHocRunRequest().items(List.of(
                                new AdHocItem().type("functional")))));
        assertThrows(IllegalArgumentException.class,
                () -> client.testPlans().createAdHocRun(
                        new CreateAdHocRunRequest().items(List.of(
                                new AdHocItem().refId("c1")))));
    }

    @Test
    @DisplayName("getRunReport decodes the Allure JSON envelope")
    void getRunReport() {
        server.createContext(
                "/api/v1/namespaces/test-ns/test-plans/plan-1/runs/r1/report",
                exchange -> {
                    String body = "{\"runId\":\"r1\",\"planId\":\"plan-1\"," +
                            "\"status\":\"completed\"," +
                            "\"items\":[{\"status\":\"passed\",\"durationMs\":123," +
                            "\"steps\":[{\"name\":\"step1\",\"status\":\"passed\"," +
                            "\"durationMs\":10}]}]," +
                            "\"labels\":{\"tier\":\"smoke\"}}";
                    byte[] bytes = body.getBytes();
                    exchange.getResponseHeaders().set("Content-Type",
                            "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
        AllureReport report = client.testPlans().getRunReport(null, "plan-1", "r1");
        assertEquals("r1", report.getRunId());
        assertEquals("plan-1", report.getPlanId());
        assertEquals("completed", report.getStatus());
        assertNotNull(report.getItems());
        assertEquals(1, report.getItems().size());
        assertEquals("passed", report.getItems().get(0).getStatus());
        assertEquals(Long.valueOf(123L),
                report.getItems().get(0).getDurationMs());
        assertNotNull(report.getItems().get(0).getSteps());
        assertEquals("step1",
                report.getItems().get(0).getSteps().get(0).getName());
        assertNotNull(report.getLabels());
        assertEquals("smoke", report.getLabels().get("tier"));
        // Raw bytes are preserved for forward-compat reparses.
        assertNotNull(report.getRaw());
        assertTrue(report.getRaw().length > 0);
    }

    @Test
    @DisplayName("getRunReport preserves raw bytes on unknown top-level shape")
    void getRunReportUnknownShape() {
        server.createContext(
                "/api/v1/namespaces/other/test-plans/plan-1/runs/r1/report",
                exchange -> {
                    String body = "{\"some_future_field\":42}";
                    byte[] bytes = body.getBytes();
                    exchange.getResponseHeaders().set("Content-Type",
                            "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
        AllureReport report = client.testPlans()
                .getRunReport("other", "plan-1", "r1");
        assertNotNull(report);
        assertNull(report.getRunId());
        assertNotNull(report.getRaw());
    }

    @Test
    @DisplayName("getRunReport validates empty runId client-side")
    void getRunReportValidates() {
        assertThrows(IllegalArgumentException.class,
                () -> client.testPlans().getRunReport(null, "plan-1", ""));
    }

    @Test
    @DisplayName("getRunReportZip streams the body into destination")
    void getRunReportZip() {
        server.createContext(
                "/api/v1/namespaces/test-ns/test-plans/plan-1/runs/r1/report.zip",
                exchange -> {
                    byte[] bytes = new byte[]{'P', 'K', 0x03, 0x04, 'x', 'y', 'z'};
                    exchange.getResponseHeaders().set("Content-Type",
                            "application/zip");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        client.testPlans().getRunReportZip(null, "plan-1", "r1", buf);
        assertEquals(7, buf.size());
        assertEquals('P', buf.toByteArray()[0]);
        assertEquals('K', buf.toByteArray()[1]);
    }

    @Test
    @DisplayName("getRunReportZip surfaces non-2xx as MockartyException")
    void getRunReportZipError() {
        server.createContext(
                "/api/v1/namespaces/test-ns/test-plans/plan-1/runs/r2/report.zip",
                exchange -> {
                    String body = "{\"error\":\"not ready\"}";
                    exchange.sendResponseHeaders(404, body.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body.getBytes());
                    }
                });
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        assertThrows(ru.mockarty.exception.MockartyException.class,
                () -> client.testPlans().getRunReportZip(null, "plan-1", "r2", buf));
    }

    @Test
    @DisplayName("getRunReportZip rejects a null destination")
    void getRunReportZipValidatesDestination() {
        assertThrows(IllegalArgumentException.class,
                () -> client.testPlans().getRunReportZip(null, "plan-1", "r1", null));
    }
}
