// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;

/** Draft -> dry-run -> immutable publication without executing the workflow. */
public final class WorkflowDefinitionsExample {
    private WorkflowDefinitionsExample() {}

    public static void main(String[] args) throws Exception {
        String namespace = env("MOCKARTY_NAMESPACE", "sandbox");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(env("MOCKARTY_URL", "http://127.0.0.1:5770"))
                .apiKey(System.getenv("MOCKARTY_API_TOKEN"))
                .build()) {
            JsonNode definition = client.getObjectMapper().readTree("{"
                    + "\"contractVersion\":\"mockarty.workflow/v1\","
                    + "\"namespace\":\"" + namespace + "\","
                    + "\"id\":\"release-check\",\"version\":\"1.0.0\",\"status\":\"draft\","
                    + "\"entryNode\":\"inspect\","
                    + "\"nodes\":[{\"id\":\"inspect\",\"capability\":{"
                    + "\"key\":\"mission.inspect\",\"version\":\"1.0.0\"}}],\"transitions\":[]}");
            JsonNode created = client.workflowDefinitions().createDraft(definition);
            long revision = created.path("revision").asLong();
            JsonNode dryRun = client.workflowDefinitions().dryRun(namespace, "release-check", "1.0.0", revision);
            if (!dryRun.path("ready").asBoolean()) {
                throw new IllegalStateException("workflow is blocked: " + dryRun.path("blockers"));
            }
            JsonNode published = client.workflowDefinitions().publish(namespace, "release-check", "1.0.0", revision);
            System.out.println("published revision=" + published.path("revision").asLong());
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
