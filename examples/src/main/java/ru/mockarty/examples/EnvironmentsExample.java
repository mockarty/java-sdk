// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.Environment;

import java.util.List;
import java.util.Map;

/**
 * API Tester environment management examples demonstrating
 * CRUD operations and environment activation for test execution.
 *
 * <p>Environments store variables (base URLs, tokens, credentials)
 * that are injected into API Tester requests at runtime. This allows
 * the same test collection to target different backends.</p>
 */
public class EnvironmentsExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createEnvironments(client);
            listAndInspect(client);
            activateEnvironment(client);
            updateAndDelete(client);
        }
    }

    /**
     * Create multiple environments for different stages.
     */
    static void createEnvironments(MockartyClient client) {
        System.out.println("=== Create Environments ===");

        // Development environment
        Environment dev = new Environment()
                .name("Development")
                .variables(Map.of(
                        "baseUrl", "http://localhost:8080",
                        "apiKey", "dev-api-key-12345",
                        "dbHost", "localhost:5432",
                        "logLevel", "debug",
                        "timeout", "30000"
                ));

        Environment createdDev = client.environments().create(dev);
        System.out.println("Created environment: " + createdDev.getName() +
                " (id: " + createdDev.getId() + ")");

        // Staging environment
        Environment staging = new Environment()
                .name("Staging")
                .variables(Map.of(
                        "baseUrl", "https://staging-api.example.com",
                        "apiKey", "staging-key-67890",
                        "dbHost", "staging-db.internal:5432",
                        "logLevel", "info",
                        "timeout", "15000"
                ));

        Environment createdStaging = client.environments().create(staging);
        System.out.println("Created environment: " + createdStaging.getName());

        // Production environment
        Environment prod = new Environment()
                .name("Production")
                .variables(Map.of(
                        "baseUrl", "https://api.example.com",
                        "apiKey", "prod-key-secure",
                        "dbHost", "prod-db.internal:5432",
                        "logLevel", "error",
                        "timeout", "10000"
                ));

        Environment createdProd = client.environments().create(prod);
        System.out.println("Created environment: " + createdProd.getName());
    }

    /**
     * List all environments and inspect the active one.
     */
    static void listAndInspect(MockartyClient client) {
        System.out.println("\n=== List Environments ===");

        // List all environments
        List<Environment> envs = client.environments().list();
        System.out.println("Total environments: " + envs.size());

        for (Environment env : envs) {
            System.out.println("  " + env.getName() + " (id: " + env.getId() + ")");
            Map<String, String> vars = env.getVariables();
            if (vars != null) {
                System.out.println("    Variables: " + vars.keySet());
            }
        }

        // Get the currently active environment
        try {
            Environment active = client.environments().getActive();
            System.out.println("Active environment: " + active.getName());
            System.out.println("  Base URL: " + active.getVariables().get("baseUrl"));
        } catch (Exception e) {
            System.out.println("No active environment set");
        }
    }

    /**
     * Activate an environment for use in API Tester requests.
     */
    static void activateEnvironment(MockartyClient client) {
        System.out.println("\n=== Activate Environment ===");

        List<Environment> envs = client.environments().list();
        if (envs.isEmpty()) {
            System.out.println("No environments available");
            return;
        }

        // Activate the first environment (e.g., Development)
        Environment target = envs.get(0);
        client.environments().activate(target.getId());
        System.out.println("Activated environment: " + target.getName());

        // Verify it's active
        Environment active = client.environments().getActive();
        System.out.println("Confirmed active: " + active.getName());
        System.out.println("  Base URL: " + active.getVariables().get("baseUrl"));
    }

    /**
     * Update environment variables and delete environments.
     */
    static void updateAndDelete(MockartyClient client) {
        System.out.println("\n=== Update and Delete ===");

        List<Environment> envs = client.environments().list();
        if (envs.isEmpty()) {
            System.out.println("No environments to update");
            return;
        }

        // Update an environment (e.g., change the base URL for staging)
        Environment toUpdate = envs.get(0);
        Environment updated = client.environments().update(toUpdate.getId(),
                new Environment()
                        .name(toUpdate.getName() + " (updated)")
                        .variables(Map.of(
                                "baseUrl", "http://localhost:9090",
                                "apiKey", "updated-api-key",
                                "timeout", "20000",
                                "retryCount", "3"
                        ))
        );
        System.out.println("Updated environment: " + updated.getName());

        // Get environment by ID
        Environment fetched = client.environments().get(toUpdate.getId());
        System.out.println("Fetched: " + fetched.getName());

        // Delete an environment
        // client.environments().delete(toUpdate.getId());
        // System.out.println("Deleted environment: " + toUpdate.getId());
    }
}
