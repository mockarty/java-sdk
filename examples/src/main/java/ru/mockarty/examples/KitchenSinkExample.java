// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.MockartyClient;
import ru.mockarty.MockartyConfig;
import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalRunResponse;
import ru.mockarty.tester.ExternalRunBridge;
import ru.mockarty.tester.HttpFacet;
import ru.mockarty.tester.Tester;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kitchen-sink — end-to-end Mockarty Java SDK Tester showcase.
 *
 * <p>Java mirror of {@code sdk/go-sdk/examples/kitchen_sink} and
 * {@code sdk/py-sdk/examples/kitchen_sink}. One executable that
 * exercises every Tester facet plus the reporting / upstream-tracker
 * side-channels you'd want in a real CI pipeline.</p>
 *
 * <p>What each step demonstrates:</p>
 * <ol>
 *   <li>HTTP — token issue → extract → reuse via {@code {{token}}}
 *       interpolation (testbackend {@code /api/v1/token-chain/{issue,validate}})</li>
 *   <li>GraphQL — typed query with variables + Bearer header</li>
 *   <li>HTTP — every {@code expect*} kind on a single response</li>
 *   <li>Jira mock — auto-file a Bug ticket on failure (no real Jira)</li>
 *   <li>GitLab mock — trigger pipeline + poll until success</li>
 *   <li>Mockarty TCM — upload aggregated run via
 *       {@code client.externalRuns().report(...)}</li>
 *   <li>Exit code — non-zero on failure → {@code set -e} friendly</li>
 * </ol>
 *
 * <p>Run it:</p>
 * <pre>
 *   # 1. Start testbackend (provides token-chain + Jira/GitLab mocks)
 *   mockarty-testbackend &amp;
 *
 *   # 2. (optional) Mockarty admin on 5770
 *   mockarty &amp;
 *
 *   # 3. Run
 *   TESTBACKEND_URL=http://127.0.0.1:18770 \
 *   MOCKARTY_URL=http://127.0.0.1:5770 \
 *   MOCKARTY_API_KEY=mk_... \
 *   MOCKARTY_NAMESPACE=sandbox \
 *   ./gradlew :examples:run -PmainClass=ru.mockarty.examples.KitchenSinkExample
 * </pre>
 */
public final class KitchenSinkExample {

    private static final ObjectMapper M = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public static void main(String[] args) throws Exception {
        String backend = env("TESTBACKEND_URL", "http://127.0.0.1:18770");
        String mockartyURL = env("MOCKARTY_URL", "");
        String apiKey = env("MOCKARTY_API_KEY", "");
        String namespace = env("MOCKARTY_NAMESPACE", "sandbox");
        String jiraProject = env("JIRA_PROJECT_KEY", "QA");
        String gitlabProject = env("GITLAB_PROJECT_ID", "1");

        try (Tester t = new Tester.Builder().baseUrl(backend).build()) {

            // 1+2. Token chain — issue, extract, reuse. Wrapped under
            // one Allure parent step so the report renders the two
            // child requests as a tree.
            t.wrap("token issue + authorised validate", () -> {
                t.http().get("/api/v1/token-chain/issue")
                        .expectStatus(200)
                        .expectJsonPath("$.token", "tok-abc123-deterministic")
                        .extract("$.token", "token");

                t.http().post("/api/v1/token-chain/validate")
                        .header("Authorization", "Bearer {{token}}")
                        .json(Map.of("action", "ping"))
                        .expectStatus(200)
                        .expectJsonPath("$.authorization", "Bearer tok-abc123-deterministic");
            });

            // 3. GraphQL — typed query against testbackend's seeded users.
            t.graphql(backend + "/graphql")
                    .query("query GetUser($id: ID!) { user(id: $id) { name email } }",
                            Map.of("id", "user-1"))
                    .header("Authorization", "Bearer {{token}}")
                    .expectStatus(200)
                    .expectNoErrors()
                    .expectField("$.data.user.name", "Admin User");

            // 4. Assertion variety. /api/v1/users → {items: [...]}.
            t.http().get("/api/v1/users")
                    .expectStatus(200)
                    .expectHeader("Content-Type", "application/json; charset=utf-8")
                    .expectBodyContains("Admin User")
                    .expectJsonPath("$.items[0].name", "Admin User");

            t.finish();

            // 4+5. Upstream tracker side-channels — only when failed.
            if (!t.ok()) {
                List<String> errs = t.errors();
                System.out.printf("kitchen-sink: %d failed step(s); filing tracker artefacts%n", errs.size());
                fileJiraTicket(backend, jiraProject, errs);
                triggerGitLabPipeline(backend, gitlabProject);
            }

            // 6. Mockarty TCM upload — skip when no admin configured.
            if (!mockartyURL.isEmpty() && !apiKey.isEmpty()) {
                try (MockartyClient client = MockartyClient.create(mockartyURL, apiKey)) {
                    ExternalRunBridge.Options opts = new ExternalRunBridge.Options()
                            .caseName("kitchen-sink")
                            .framework("mockarty-java-tester")
                            .autoCreate(true)
                            .fullName("ru.mockarty.examples.KitchenSinkExample");
                    ExternalRunRequest req = ExternalRunBridge.toExternalRunRequest(t, opts);
                    ExternalRunResponse resp = client.externalRuns().report(namespace, req);
                    System.out.printf("mockarty TCM: %s (run=%s case=%s)%n",
                            resp.getStatus(), resp.getRunId(), resp.getCaseId());
                } catch (Exception e) {
                    System.err.println("mockarty TCM upload: " + e.getMessage());
                }
            }

            // 7. Exit code = run status.
            if (!t.ok()) {
                System.exit(1);
            }
            System.out.println("kitchen-sink: ok");
        }
    }

    // ── Jira / GitLab side-channels (mock-aware) ─────────────────────

    private static void fileJiraTicket(String backend, String project, List<String> errs) {
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("project", Map.of("key", project));
            fields.put("summary", "kitchen-sink run failed: " + truncate(String.join("; ", errs), 80));
            fields.put("issuetype", Map.of("name", "Bug"));
            byte[] body = M.writeValueAsBytes(Map.of("fields", fields));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(backend + "/rest/api/2/issue"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 201) {
                JsonNode tree = M.readTree(r.body());
                System.out.printf("jira: filed %s for %d failure(s)%n",
                        tree.path("key").asText(), errs.size());
            } else {
                System.err.printf("jira: %d %s%n", r.statusCode(), r.body());
            }
        } catch (Exception e) {
            System.err.println("jira create: " + e.getMessage());
        }
    }

    private static void triggerGitLabPipeline(String backend, String project) {
        try {
            HttpRequest trig = HttpRequest.newBuilder()
                    .uri(URI.create(backend + "/api/v4/projects/" + project + "/trigger/pipeline?ref=main"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> r = HTTP.send(trig, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 201) {
                System.err.printf("gitlab trigger: %d%n", r.statusCode());
                return;
            }
            JsonNode pl = M.readTree(r.body());
            int id = pl.path("id").asInt();
            String status = pl.path("status").asText();
            for (int i = 0; i < 5 && !status.equals("success") && !status.equals("failed"); i++) {
                Thread.sleep(100);
                HttpRequest poll = HttpRequest.newBuilder()
                        .uri(URI.create(backend + "/api/v4/projects/" + project + "/pipelines/" + id))
                        .GET().build();
                HttpResponse<String> p = HTTP.send(poll, HttpResponse.BodyHandlers.ofString());
                status = M.readTree(p.body()).path("status").asText();
            }
            System.out.printf("gitlab pipeline #%d → %s%n", id, status);
        } catch (Exception e) {
            System.err.println("gitlab pipeline trigger: " + e.getMessage());
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }

    private static String truncate(String s, int n) {
        if (s.length() <= n) return s;
        return s.substring(0, n) + "…";
    }

    private KitchenSinkExample() {} // static
}
