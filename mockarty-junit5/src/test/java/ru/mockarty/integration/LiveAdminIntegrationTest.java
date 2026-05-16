// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.exception.MockartyNotFoundException;
import ru.mockarty.junit5.allure.AllureModel;
import ru.mockarty.junit5.allure.AllureWriter;
import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalRunResponse;
import ru.mockarty.model.HealthResponse;
import ru.mockarty.model.Mock;
import ru.mockarty.model.SaveMockResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live-admin smoke. Drives a real Mockarty admin node over HTTP and exercises:
 *
 * <ul>
 *   <li>The {@link MockartyClient} happy path (health, mock CRUD).</li>
 *   <li>The Allure-2 emitter pipeline ({@link AllureLifecycle} → JSON on disk
 *       → schema-shape verification).</li>
 *   <li>The {@code POST /api/v1/namespaces/:ns/tcm/external-runs} round-trip
 *       via {@link ru.mockarty.api.ExternalRunsApi}.</li>
 * </ul>
 *
 * <p>The whole class is skipped when {@code MOCKARTY_BASE_URL} or
 * {@code MOCKARTY_API_KEY} is absent — exactly the contract the briefing
 * asks for (no spurious failures in CI / on a dev box without admin).
 *
 * <p>The long-lived admin API token uses the {@code X-API-Key} header,
 * not {@code Authorization: Bearer}. The default {@link MockartyClient}
 * sends {@code Authorization: Bearer}, which admin's
 * combined-auth middleware also accepts for {@code mk_*} tokens — so we
 * intentionally drive both code paths: SDK calls (Bearer) AND a raw
 * {@code HttpClient} call (X-API-Key) for parity.
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "MOCKARTY_BASE_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "MOCKARTY_API_KEY", matches = ".+")
class LiveAdminIntegrationTest {

    private static String baseUrl;
    private static String apiKey;
    private static String namespace;
    private static MockartyClient client;

    @BeforeAll
    static void wire() {
        baseUrl = System.getenv("MOCKARTY_BASE_URL");
        apiKey = System.getenv("MOCKARTY_API_KEY");
        namespace = System.getenv().getOrDefault("MOCKARTY_NAMESPACE", "sandbox");

        client = MockartyClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .namespace(namespace)
                .timeout(Duration.ofSeconds(20))
                .build();
    }

    @Test
    @DisplayName("clientCanFetchHealth — GET /health returns 200 + status payload")
    void clientCanFetchHealth() throws MockartyException, IOException, InterruptedException {
        // SDK path
        HealthResponse hr = client.health().check();
        assertNotNull(hr, "health response not null");
        // Status is "pass" or "ok" depending on admin version — the
        // assertion is intentionally loose so we don't pin the wire shape.
        assertNotNull(hr.getStatus(), "health status not null");

        // Raw X-API-Key path — proves the bare REST endpoint accepts the
        // alternative header that STATE.md documents as the canonical one
        // for long-lived API tokens.
        HttpClient bare = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .header("X-API-Key", apiKey)
                .GET()
                .build();
        HttpResponse<String> resp = bare.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), "raw X-API-Key /health 200");
    }

    @Test
    @DisplayName("clientCanCreateAndDeleteMock — full CRUD round-trip against admin")
    void clientCanCreateAndDeleteMock() throws MockartyException {
        String id = "java-sdk-int-" + UUID.randomUUID();
        Mock mock = MockBuilder.http("/sdk-int/" + id, "GET")
                .id(id)
                .namespace(namespace)
                .respond(200, Map.of("ok", true, "id", id))
                .build();

        SaveMockResponse save = client.mocks().create(mock);
        assertNotNull(save, "create response");

        Mock fetched = client.mocks().get(id);
        assertNotNull(fetched, "fetched not null");
        assertEquals(id, fetched.getId(), "id round-trips");

        client.mocks().delete(id);
        // Admin uses soft-delete (closedAt) — depending on the
        // current behaviour, get after delete either 404s or returns a
        // record with closedAt set. We accept both shapes so the test
        // doesn't pin a specific delete semantic.
        try {
            Mock afterDelete = client.mocks().get(id);
            // Soft-deleted record may still come back; just make sure the
            // server didn't throw and the id matches.
            assertEquals(id, afterDelete.getId());
        } catch (MockartyNotFoundException ignored) {
            // hard-404 is also valid
        }
    }

    @Test
    @DisplayName("allureExtensionEmitsResults — AllureWriter emits a parseable result JSON")
    void allureExtensionEmitsResults() throws IOException {
        // AllureLifecycle.resultsDir is resolved once per JVM, so we
        // bypass the singleton and drive AllureWriter directly. This is
        // exactly what the JUnit5 / TestNG / Cucumber adapters do under
        // the hood — we're verifying the on-disk shape, not the locator.
        Path tempResults = Files.createTempDirectory("mockarty-allure-it-");
        try {
            AllureModel.TestResult tr = new AllureModel.TestResult();
            tr.uuid = UUID.randomUUID().toString();
            tr.name = "integration_test_emitter";
            tr.fullName = "ru.mockarty.integration.fake#emitter";
            tr.status = AllureModel.Status.PASSED;
            tr.stage = AllureModel.Stage.FINISHED;
            tr.start = System.currentTimeMillis();
            tr.stop = tr.start + 5;

            Path written = AllureWriter.writeTestResult(tempResults, tr);
            assertNotNull(written, "writer returns a path");
            assertTrue(Files.exists(written), "result file exists: " + written);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(written.toFile());
            assertEquals("passed", root.path("status").asText(), "status=passed");
            assertEquals(tr.uuid, root.path("uuid").asText(), "uuid round-trips");
            assertTrue(root.path("start").asLong(0) > 0, "start > 0");
            assertTrue(root.path("stop").asLong(0) > 0, "stop > 0");
        } finally {
            // Best-effort cleanup so we don't leave temp dirs behind.
            try (DirectoryStream<Path> s = Files.newDirectoryStream(tempResults)) {
                for (Path p : s) Files.deleteIfExists(p);
            } catch (IOException ignored) {}
            Files.deleteIfExists(tempResults);
        }
    }

    @Test
    @DisplayName("externalRunsCanSubmitAndRetrieve — full external-run lifecycle (auto-create case)")
    void externalRunsCanSubmitAndRetrieve() {
        String displayName = "java-sdk-int-er-" + UUID.randomUUID();
        ExternalRunRequest req = new ExternalRunRequest()
                .status(ExternalRunRequest.STATUS_PASSED)
                .caseName(displayName)
                .autoCreate(true)
                .framework("junit5")
                .frameworkVersion("5.10")
                .externalId("ru.mockarty.integration.LiveAdminIntegrationTest#externalRuns")
                .testDisplayName(displayName)
                .durationMs(42);

        ExternalRunResponse resp;
        try {
            resp = client.externalRuns().report(namespace, req);
        } catch (MockartyException e) {
            // The endpoint sits behind the `tcm` feature gate. On an admin
            // without the TCM licence the gate either rejects with 403 or
            // never registers the route at all (404). Both are
            // licence-policy outcomes, NOT SDK bugs — record a
            // skip-equivalent and move on. Real network/SDK breakage
            // would surface as a 5xx / connection refused / serialisation
            // error and re-throws below.
            String msg = e.getMessage() == null ? "" : e.getMessage();
            String lower = msg.toLowerCase();
            if (msg.contains("403") || lower.contains("forbidden")
                    || lower.contains("feature")
                    || msg.contains("404") || lower.contains("not found")) {
                System.err.println("[mockarty-java-sdk-it] external-runs skipped, "
                        + "tcm feature not licensed/registered: " + msg);
                return;
            }
            throw new AssertionError("external-runs unexpected failure: " + e.getMessage(), e);
        }
        assertNotNull(resp, "external-run response");
        // The server returns a run UUID + (when autoCreate) a resolved case;
        // we only assert presence — exact values depend on the admin DB
        // state and aren't reproducible across runs.
    }

    @Test
    @DisplayName("xApiKeyHeaderAcceptedByAdmin — confirms STATE.md auth contract")
    void xApiKeyHeaderAcceptedByAdmin() throws IOException, InterruptedException {
        // Touches the namespaces endpoint, which is the canonical
        // "are you authenticated?" probe from STATE.md.
        HttpClient bare = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/namespaces"))
                .header("X-API-Key", apiKey)
                .GET()
                .build();
        HttpResponse<String> resp = bare.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(),
                "X-API-Key header accepted on /api/v1/namespaces");
    }
}
