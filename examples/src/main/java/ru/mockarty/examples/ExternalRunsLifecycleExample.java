// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.api.ExternalRunsApi;

import java.util.List;
import java.util.Map;

/**
 * Streams a long test's results incrementally via the lifecycle API.
 *
 * <p>Unlike {@code report} (one-shot upload of a finished run), the lifecycle
 * API reports as the suite runs: startRun → appendSteps (repeatedly) →
 * finishRun. The finished view carries the resolved TCM case/run ids.
 */
public class ExternalRunsLifecycleExample {

    public static void main(String[] args) throws Exception {
        String baseUrl = System.getenv().getOrDefault("MOCKARTY_SERVER", "http://localhost:5770");
        String ns = System.getenv().getOrDefault("MOCKARTY_NAMESPACE", "sandbox");
        try (MockartyClient client = MockartyClient.create(baseUrl, System.getenv("MOCKARTY_API_KEY"))) {
            ExternalRunsApi er = client.externalRuns();

            JsonNode run = er.startRun(ns, Map.of(
                    "name", "checkout smoke",
                    "framework", "custom",
                    "full_name", "suites.checkout.smoke"));
            String runId = run.path("id").asText();
            System.out.println("started run " + runId);

            List<Map<String, Object>> steps = List.of(
                    Map.of("step_key", "login", "name", "log in", "status", "passed", "duration_ms", 120),
                    Map.of("step_key", "cart", "name", "add to cart", "status", "passed", "duration_ms", 80),
                    Map.of("step_key", "pay", "name", "pay", "status", "failed", "message", "gateway 500", "duration_ms", 210));
            for (Map<String, Object> step : steps) {
                er.appendSteps(ns, runId, List.of(step));
            }

            JsonNode fin = er.finishRun(ns, runId, "failed", "payment gateway returned 500");
            System.out.printf("finished: status=%s case=%s run=%s%n",
                    fin.path("status").asText(),
                    fin.path("resolved_case_id").asText(),
                    fin.path("resolved_run_id").asText());
        }
    }
}
