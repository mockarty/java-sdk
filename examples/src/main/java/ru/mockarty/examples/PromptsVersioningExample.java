// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

import java.util.List;
import java.util.Map;

/**
 * Prompts Storage — create, update, list history, rollback.
 */
public class PromptsVersioningExample {

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            Map<String, Object> p = client.prompts().create(
                    "tcm-step-summarizer",
                    "Summarize the following test step in one sentence: {{.step}}",
                    Map.of("model", "claude-opus-4-7", "tags", List.of("tcm", "summary")));
            String promptId = (String) p.get("id");
            System.out.printf("[1] created %s v%s%n", p.get("name"), p.get("version"));

            try {
                client.prompts().update(promptId, Map.of(
                        "body", "Summarize in ≤15 words: {{.step}}"));
                p = client.prompts().update(promptId, Map.of(
                        "body", "One sentence summary, verb-first: {{.step}}"));
                System.out.printf("[2] current v%s%n", p.get("version"));

                List<Map<String, Object>> versions = client.prompts().listVersions(promptId);
                System.out.printf("[3] history: %d versions%n", versions.size());

                Map<String, Object> rolled = client.prompts().rollback(promptId, 1);
                System.out.printf("[4] rolled back; new v%s%n", rolled.get("version"));
            } finally {
                client.prompts().delete(promptId);
            }
        }
    }
}
