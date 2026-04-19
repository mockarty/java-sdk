// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyForbiddenException;
import ru.mockarty.exception.PurgeConfirmationException;
import ru.mockarty.model.BulkPurgeResult;
import ru.mockarty.model.BulkRestoreResult;
import ru.mockarty.model.PurgeNowResult;
import ru.mockarty.model.RestoreResult;
import ru.mockarty.model.TrashListOptions;
import ru.mockarty.model.TrashListResult;
import ru.mockarty.model.TrashSettings;
import ru.mockarty.model.TrashSummary;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrashApiTest {

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
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    // ── List ────────────────────────────────────────────────────────

    @Test
    @DisplayName("listTrash parses the envelope and applies query filters")
    void listTrash() throws Exception {
        AtomicReference<String> seenQuery = new AtomicReference<>();
        server.createContext("/api/v1/namespaces/prod/trash", exchange -> {
            seenQuery.set(exchange.getRequestURI().getRawQuery());
            String body = "{\"items\":[{\"id\":\"i1\",\"name\":\"users\",\"namespace\":\"prod\"," +
                    "\"entity_type\":\"mock\",\"closed_at\":\"2026-04-19T12:00:00Z\"," +
                    "\"restore_available\":true}],\"total\":1,\"limit\":25,\"offset\":10}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        TrashListResult result = client.trash().listTrash("prod", new TrashListOptions()
                .entityType("mock").entityType("store")
                .search("users")
                .limit(25)
                .offset(10));
        assertEquals(1, result.getTotal());
        assertTrue(result.getItems().get(0).isRestoreAvailable());
        String q = seenQuery.get();
        assertNotNull(q);
        assertTrue(q.contains("type=mock%2Cstore"), q);
        assertTrue(q.contains("q=users"), q);
        assertTrue(q.contains("limit=25"), q);
        assertTrue(q.contains("offset=10"), q);
    }

    @Test
    @DisplayName("adminListTrash hits the admin-scoped path")
    void adminListTrash() throws Exception {
        respond("/api/v1/admin/trash",
                200,
                "{\"items\":[],\"total\":3,\"limit\":50,\"offset\":0}");
        TrashListResult r = client.trash().adminListTrash(new TrashListOptions());
        assertEquals(3, r.getTotal());
    }

    @Test
    @DisplayName("listTrash rejects blank namespace")
    void listTrashRejectsBlankNs() {
        assertThrows(IllegalArgumentException.class,
                () -> client.trash().listTrash("  ", new TrashListOptions()));
    }

    // ── Summary ─────────────────────────────────────────────────────

    @Test
    @DisplayName("summary decodes counts")
    void summary() throws Exception {
        respond("/api/v1/namespaces/ns/trash/summary",
                200,
                "{\"counts\":[{\"entity_type\":\"mock\",\"count\":4}],\"total\":4}");
        TrashSummary s = client.trash().summary("ns");
        assertEquals(4, s.getTotal());
        assertEquals("mock", s.getCounts().get(0).getEntityType());
        assertEquals(4, s.getCounts().get(0).getCount());
    }

    @Test
    @DisplayName("adminSummary decodes aggregate total")
    void adminSummary() throws Exception {
        respond("/api/v1/admin/trash/summary",
                200,
                "{\"counts\":[],\"total\":42}");
        TrashSummary s = client.trash().adminSummary();
        assertEquals(42, s.getTotal());
    }

    // ── Settings ────────────────────────────────────────────────────

    @Test
    @DisplayName("getSettings flags inherited scope")
    void getSettings() throws Exception {
        respond("/api/v1/namespaces/finance/trash/settings",
                200,
                "{\"scope\":\"namespace\",\"retention_days\":7,\"enabled\":true,\"inherited\":true}");
        TrashSettings s = client.trash().getSettings("finance");
        assertEquals(7, s.getRetentionDays());
        assertTrue(s.isEnabled());
        assertTrue(Boolean.TRUE.equals(s.getInherited()));
    }

    @Test
    @DisplayName("updateGlobalSettings round-trips")
    void updateGlobalSettings() throws Exception {
        respond("/api/v1/admin/trash/settings/global",
                200,
                "{\"scope\":\"global\",\"retention_days\":60,\"enabled\":true}");
        TrashSettings got = client.trash().updateGlobalSettings(60, true);
        assertEquals(60, got.getRetentionDays());
    }

    // ── Restore ─────────────────────────────────────────────────────

    @Test
    @DisplayName("restoreCascade parses camelCase RestoreResult")
    void restoreCascade() throws Exception {
        respond("/api/v1/namespaces/prod/trash/restore-cascade/grp-1",
                200,
                "{\"cascadeGroupId\":\"grp-1\",\"restoredCount\":5}");
        RestoreResult r = client.trash().restoreCascade("prod", "grp-1");
        assertEquals("grp-1", r.getCascadeGroupId());
        assertEquals(5, r.getRestoredCount());
    }

    @Test
    @DisplayName("restoreCascade rejects blank id")
    void restoreCascadeRejectsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> client.trash().restoreCascade("ns", "  "));
    }

    @Test
    @DisplayName("bulkRestore decodes partial-success envelope")
    void bulkRestorePartial() throws Exception {
        respond("/api/v1/namespaces/ns/trash/restore",
                200,
                "{\"restored\":[{\"cascade_group_id\":\"g1\",\"entity_type\":\"mock\",\"restored_count\":2}]," +
                        "\"failed\":[{\"cascade_group_id\":\"g2\",\"error\":\"denied\"}]," +
                        "\"not_found\":[\"g3\"]}");
        BulkRestoreResult got = client.trash().bulkRestore("ns",
                List.of("g1", "g2", "g3"), "fix");
        assertEquals(1, got.getRestored().size());
        assertEquals(1, got.getFailed().size());
        assertEquals(List.of("g3"), got.getNotFound());
    }

    @Test
    @DisplayName("bulkRestore rejects empty ids")
    void bulkRestoreRejectsEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> client.trash().bulkRestore("ns", List.of(), null));
    }

    // ── Purge ───────────────────────────────────────────────────────

    @Test
    @DisplayName("bulkPurge refuses missing confirmation phrase client-side")
    void bulkPurgeMissingConfirmation() {
        assertThrows(PurgeConfirmationException.class,
                () -> client.trash().bulkPurge("ns", List.of("g1"), "", "reason"));
    }

    @Test
    @DisplayName("bulkPurge refuses a wrong confirmation phrase client-side")
    void bulkPurgeWrongConfirmation() {
        assertThrows(PurgeConfirmationException.class,
                () -> client.trash().bulkPurge("ns", List.of("g1"), "yes delete", null));
    }

    @Test
    @DisplayName("bulkPurge happy path carries the phrase in the body")
    void bulkPurgeHappy() throws Exception {
        respond("/api/v1/namespaces/ns/trash/purge",
                200,
                "{\"purged\":[{\"cascade_group_id\":\"g1\",\"entity_type\":\"mock\",\"rows_deleted\":7}]," +
                        "\"failed\":[],\"not_found\":[]}");
        BulkPurgeResult r = client.trash().bulkPurge("ns",
                List.of("g1"),
                TrashApi.PURGE_CONFIRMATION_PHRASE,
                "GDPR");
        assertEquals(1, r.getPurged().size());
        assertEquals(7, r.getPurged().get(0).getRowsDeleted());
        assertTrue(r.getFailed().isEmpty());
        assertFalse(r.getPurged().isEmpty());
    }

    @Test
    @DisplayName("adminBulkPurge 403 surfaces as MockartyForbiddenException")
    void adminBulkPurgeForbidden() {
        respond("/api/v1/admin/trash/purge",
                403,
                "{\"error\":\"support cannot purge\",\"code\":\"forbidden\"}");
        assertThrows(MockartyForbiddenException.class,
                () -> client.trash().adminBulkPurge(
                        List.of("g1"),
                        TrashApi.PURGE_CONFIRMATION_PHRASE,
                        null));
    }

    @Test
    @DisplayName("adminPurgeNow decodes counters")
    void adminPurgeNow() throws Exception {
        respond("/api/v1/admin/trash/purge-now",
                200,
                "{\"status\":\"ok\",\"purged_total\":123,\"namespaces_scanned\":4}");
        PurgeNowResult r = client.trash().adminPurgeNow();
        assertEquals(123L, r.getPurgedTotal());
        assertEquals(4, r.getNamespacesScanned());
    }
}
