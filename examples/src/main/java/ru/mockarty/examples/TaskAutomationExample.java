// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.api.IssueTrackerApi;
import ru.mockarty.api.TcmApi;

import java.util.List;
import java.util.Map;

/**
 * Drives the issue tracker + TCM from the SDK — files a bug, then creates and
 * runs a test case. The kind of end-to-end automation a CI agent does.
 */
public class TaskAutomationExample {

    public static void main(String[] args) throws Exception {
        String baseUrl = System.getenv().getOrDefault("MOCKARTY_SERVER", "http://localhost:5770");
        String ns = System.getenv().getOrDefault("MOCKARTY_NAMESPACE", "sandbox");
        try (MockartyClient client = MockartyClient.create(baseUrl, System.getenv("MOCKARTY_API_KEY"))) {
            IssueTrackerApi it = client.issueTracker();
            List<JsonNode> projects = it.listProjects(ns);
            if (projects.isEmpty()) {
                throw new RuntimeException("no projects in this namespace");
            }
            String pid = projects.get(0).get("id").asText();

            JsonNode issue = it.createIssue(ns, Map.of(
                    "projectId", pid, "type", "bug", "title", "Checkout returns 500"));
            String issueId = issue.get("id").asText();
            System.out.println("filed issue " + issue.path("issueKey").asText());
            // A thrown API exception retains its response body, including
            // unknown capture handles. Do not automatically post again.
            JsonNode comment = it.addComment(ns, issueId, "reproduced on staging");
            if (comment.path("state").asText().equals("pending")) {
                System.out.println("Comment awaits confirmation; keep handles without posting again: "
                        + comment.path("operation_id").asText() + " " + comment.path("capture_id").asText());
            }
            it.moveIssue(ns, issueId, "in_progress", null);

            TcmApi tcm = client.tcm();
            JsonNode c = tcm.createCase(ns, Map.of("name", "Checkout smoke"));
            JsonNode run = tcm.runCase(ns, c.get("id").asText(), null);
            System.out.println("case run started: " + run.path("runId").asText());
        }
    }
}
