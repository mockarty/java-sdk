// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.AgentTask;

import java.util.List;
import java.util.Map;

/**
 * AI agent task management examples demonstrating how to submit,
 * track, rerun, export, and manage AI-powered tasks.
 *
 * <p>Agent tasks allow you to delegate complex operations to the
 * Mockarty AI assistant, such as:</p>
 * <ul>
 *   <li>Generating mocks from natural language descriptions</li>
 *   <li>Analyzing API specifications and suggesting improvements</li>
 *   <li>Auto-generating test scenarios</li>
 *   <li>Converting between mock formats</li>
 * </ul>
 */
public class AgentTasksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            submitTask(client);
            trackTaskProgress(client);
            rerunAndExport(client);
            manageTasks(client);
        }
    }

    /**
     * Submit various AI agent tasks.
     */
    static void submitTask(MockartyClient client) {
        System.out.println("=== Submit Agent Tasks ===");

        // Task 1: Generate mocks from natural language
        AgentTask mockGenTask = client.agentTasks().submit(Map.of(
                "type", "generate_mocks",
                "prompt", "Create a REST API for a pet store with CRUD operations " +
                        "for pets, owners, and appointments. Include realistic Faker data " +
                        "and proper error responses (404, 422, 500).",
                "namespace", "sandbox",
                "options", Map.of(
                        "includeConditions", true,
                        "includeStores", true,
                        "responseFormat", "json"
                )
        ));
        System.out.println("Submitted mock generation task: " + mockGenTask.getId());
        System.out.println("  Status: " + mockGenTask.getStatus());

        // Task 2: Analyze an API specification
        AgentTask analysisTask = client.agentTasks().submit(Map.of(
                "type", "analyze_spec",
                "prompt", "Analyze the following OpenAPI spec and suggest improvements " +
                        "for better test coverage, edge cases, and security testing.",
                "context", Map.of(
                        "spec", "openapi: '3.0.0'\ninfo:\n  title: User API\n  version: '1.0.0'\npaths:\n  /users:\n    get:\n      responses:\n        '200':\n          description: OK"
                )
        ));
        System.out.println("Submitted analysis task: " + analysisTask.getId());

        // Task 3: Generate test scenarios
        AgentTask testGenTask = client.agentTasks().submit(Map.of(
                "type", "generate_tests",
                "prompt", "Generate comprehensive test scenarios for the user " +
                        "authentication flow including login, token refresh, " +
                        "password reset, and MFA verification.",
                "namespace", "sandbox"
        ));
        System.out.println("Submitted test generation task: " + testGenTask.getId());
    }

    /**
     * Track the progress of submitted tasks and retrieve results.
     */
    static void trackTaskProgress(MockartyClient client) {
        System.out.println("\n=== Track Task Progress ===");

        // List all tasks
        List<AgentTask> tasks = client.agentTasks().list();
        System.out.println("Total agent tasks: " + tasks.size());

        for (AgentTask task : tasks) {
            System.out.println("  Task: " + task.getId());
            System.out.println("    Type: " + task.getType());
            System.out.println("    Status: " + task.getStatus());
            System.out.println("    Created: " + task.getCreatedAt());

            // Get detailed task info
            AgentTask detail = client.agentTasks().get(task.getId());
            System.out.println("    Result preview: " + truncate(String.valueOf(detail.getResult()), 100));
        }

        // Cancel a running task (if any)
        for (AgentTask task : tasks) {
            if ("running".equals(task.getStatus())) {
                client.agentTasks().cancel(task.getId());
                System.out.println("Cancelled running task: " + task.getId());
                break;
            }
        }
    }

    /**
     * Rerun a completed task and export results.
     */
    static void rerunAndExport(MockartyClient client) {
        System.out.println("\n=== Rerun and Export ===");

        List<AgentTask> tasks = client.agentTasks().list();
        if (tasks.isEmpty()) {
            System.out.println("No tasks available");
            return;
        }

        // Find a completed task to rerun
        for (AgentTask task : tasks) {
            if ("completed".equals(task.getStatus())) {
                // Rerun the task (creates a new task based on the original)
                AgentTask rerunned = client.agentTasks().rerun(task.getId());
                System.out.println("Rerunned task: " + task.getId() + " -> " + rerunned.getId());
                System.out.println("  New status: " + rerunned.getStatus());

                // Export the original task result
                byte[] exported = client.agentTasks().export(task.getId());
                System.out.println("Exported task result: " + exported.length + " bytes");
                // Save to file: Files.write(Path.of("task-result.json"), exported);
                break;
            }
        }
    }

    /**
     * Manage tasks: delete individual tasks or clear all.
     */
    static void manageTasks(MockartyClient client) {
        System.out.println("\n=== Manage Tasks ===");

        List<AgentTask> tasks = client.agentTasks().list();
        System.out.println("Tasks before cleanup: " + tasks.size());

        // Delete individual failed tasks
        for (AgentTask task : tasks) {
            if ("failed".equals(task.getStatus())) {
                client.agentTasks().delete(task.getId());
                System.out.println("Deleted failed task: " + task.getId());
            }
        }

        // Clear all tasks (use with caution)
        // client.agentTasks().clearAll();
        // System.out.println("Cleared all agent tasks");

        tasks = client.agentTasks().list();
        System.out.println("Tasks after cleanup: " + tasks.size());
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
