// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.ImportResult;
import ru.mockarty.model.RecorderEntry;
import ru.mockarty.model.RecorderSession;

import java.util.List;
import java.util.Map;

/**
 * Traffic recorder examples showing how to record live API traffic,
 * inspect captured requests, convert recordings into mocks, manage
 * recorder configurations, CA certificates, and entry annotations.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>Recording sessions (start, stop, entries, mocks, export)</li>
 *   <li>Recorder configurations (save, list, export, delete)</li>
 *   <li>CA certificate management (status, generate, download)</li>
 *   <li>Entry operations (annotate, replay, modifications)</li>
 *   <li>Session management and cleanup</li>
 * </ul>
 */
public class RecorderExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            recordAndCreateMocks(client);
            manageSessions(client);
            recorderConfigs(client);
            certificateAuthority(client);
            entryAnnotations(client);
            modificationsAndReplay(client);
        }
    }

    /**
     * Record traffic from a real API, inspect entries, and convert to mocks.
     */
    static void recordAndCreateMocks(MockartyClient client) {
        // Start a recording session
        // All traffic proxied through Mockarty to the target URL will be captured
        RecorderSession session = client.recorder().start(Map.of(
                "name", "User Service Recording",
                "targetUrl", "https://api.production.com",
                "namespace", "sandbox",
                "description", "Capture user service traffic for mock generation",
                "filterPaths", List.of("/api/users", "/api/users/*"),
                "filterMethods", List.of("GET", "POST", "PUT")
        ));

        System.out.println("Recording session started: " + session.getId());
        System.out.println("  Name: " + session.getName());
        System.out.println("  Target: " + session.getTargetUrl());

        // At this point, send real requests through Mockarty's proxy:
        //   curl http://localhost:5770/api/users -H "X-Recorder-Session: <session-id>"
        // Each request/response pair is captured.

        // ... (some time passes with real traffic flowing) ...

        // Get session details
        RecorderSession updated = client.recorder().getSession(session.getId());
        System.out.println("Session status: " + updated.getStatus());

        // List recorded entries
        List<RecorderEntry> entries = client.recorder().getEntries(session.getId());
        System.out.println("Recorded entries: " + entries.size());

        for (RecorderEntry entry : entries) {
            System.out.println("  " + entry.getMethod() + " " + entry.getPath() +
                    " -> " + entry.getStatusCode());
        }

        // Stop recording
        RecorderSession stopped = client.recorder().stopRecording(session.getId());
        System.out.println("Recording stopped: " + stopped.getStatus());

        // Convert recorded entries into mocks
        ImportResult mockResult = client.recorder().createMocks(session.getId(), Map.of(
                "namespace", "sandbox",
                "generateIds", true,
                "includeHeaders", false,
                "mergeDuplicates", true
        ));

        System.out.println("Mocks created from recording:");
        System.out.println("  Total: " + mockResult.getTotal());
        System.out.println("  Created: " + mockResult.getCreated());

        // Export the recording session
        ImportResult exportResult = client.recorder().export(session.getId(), Map.of(
                "format", "mockarty"
        ));
        System.out.println("Session exported: " + exportResult);
    }

    /**
     * List, inspect, and manage recording sessions.
     */
    static void manageSessions(MockartyClient client) {
        System.out.println("\n=== Manage Sessions ===");

        // List all recording sessions
        List<RecorderSession> sessions = client.recorder().listSessions();
        System.out.println("Total recording sessions: " + sessions.size());

        for (RecorderSession session : sessions) {
            System.out.println("  Session: " + session.getId());
            System.out.println("    Name: " + session.getName());
            System.out.println("    Status: " + session.getStatus());
            System.out.println("    Target: " + session.getTargetUrl());

            // Get entries for each session
            List<RecorderEntry> entries = client.recorder().getEntries(session.getId());
            System.out.println("    Entries: " + entries.size());
        }

        // Check recorder ports
        Map<String, Object> ports = client.recorder().getPorts();
        System.out.println("Recorder ports: " + ports);
    }

    /**
     * Manage recorder configurations: save reusable recording setups,
     * list, export, and delete them.
     */
    static void recorderConfigs(MockartyClient client) {
        System.out.println("\n=== Recorder Configs ===");

        // Save a recorder configuration for reuse
        client.recorder().saveConfig(Map.of(
                "name", "Production API Capture",
                "targetUrl", "https://api.production.com",
                "filterPaths", List.of("/api/v1/*", "/api/v2/*"),
                "filterMethods", List.of("GET", "POST", "PUT", "DELETE"),
                "excludePaths", List.of("/api/v1/health", "/api/v1/metrics"),
                "captureHeaders", true,
                "captureBody", true,
                "maxEntries", 10000
        ));
        System.out.println("Saved recorder config");

        // List all saved configs
        List<Map<String, Object>> configs = client.recorder().listConfigs();
        System.out.println("Recorder configs: " + configs.size());
        for (Map<String, Object> config : configs) {
            System.out.println("  - " + config.get("name") + " (target: " + config.get("targetUrl") + ")");
        }

        // Export a config
        if (!configs.isEmpty()) {
            String configId = configs.get(0).get("id").toString();
            byte[] exported = client.recorder().exportConfig(configId);
            System.out.println("Exported config: " + exported.length + " bytes");
        }

        // Delete a config
        // if (!configs.isEmpty()) {
        //     client.recorder().deleteConfig(configs.get(0).get("id").toString());
        //     System.out.println("Deleted config");
        // }
    }

    /**
     * Manage the Certificate Authority for HTTPS traffic recording.
     * The CA certificate must be trusted by clients for HTTPS interception.
     */
    static void certificateAuthority(MockartyClient client) {
        System.out.println("\n=== Certificate Authority ===");

        // Check CA status
        Map<String, Object> caStatus = client.recorder().getCAStatus();
        System.out.println("CA status:");
        System.out.println("  Generated: " + caStatus.get("generated"));
        System.out.println("  Expires: " + caStatus.get("expiresAt"));
        System.out.println("  Fingerprint: " + caStatus.get("fingerprint"));

        // Generate a new CA certificate (if needed)
        if (!Boolean.TRUE.equals(caStatus.get("generated"))) {
            Map<String, Object> generateResult = client.recorder().generateCA();
            System.out.println("Generated new CA certificate: " + generateResult);
        }

        // Download the CA certificate (for installing in trust stores)
        byte[] caCert = client.recorder().downloadCA();
        System.out.println("Downloaded CA certificate: " + caCert.length + " bytes");
        // Save to file: Files.write(Path.of("mockarty-ca.pem"), caCert);
        // Then install in your system/browser trust store
    }

    /**
     * Annotate recorded entries with metadata for documentation
     * and future reference.
     */
    static void entryAnnotations(MockartyClient client) {
        System.out.println("\n=== Entry Annotations ===");

        List<RecorderSession> sessions = client.recorder().listSessions();
        if (sessions.isEmpty()) {
            System.out.println("No sessions available for annotation");
            return;
        }

        String sessionId = sessions.get(0).getId();
        List<RecorderEntry> entries = client.recorder().getEntries(sessionId);
        if (entries.isEmpty()) {
            System.out.println("No entries to annotate");
            return;
        }

        // Annotate an entry with metadata
        String entryId = entries.get(0).getId();
        client.recorder().annotateEntry(sessionId, entryId, Map.of(
                "description", "User login endpoint - captures auth token",
                "tags", List.of("auth", "critical-path"),
                "category", "authentication",
                "notes", "This request establishes the session token for subsequent calls"
        ));
        System.out.println("Annotated entry: " + entryId);

        // Annotate another entry
        if (entries.size() >= 2) {
            client.recorder().annotateEntry(sessionId, entries.get(1).getId(), Map.of(
                    "description", "Fetch user profile after login",
                    "tags", List.of("user-data"),
                    "category", "data-retrieval"
            ));
            System.out.println("Annotated entry: " + entries.get(1).getId());
        }
    }

    /**
     * Work with request/response modifications and entry replay.
     */
    static void modificationsAndReplay(MockartyClient client) {
        System.out.println("\n=== Modifications and Replay ===");

        List<RecorderSession> sessions = client.recorder().listSessions();
        if (sessions.isEmpty()) {
            System.out.println("No sessions available");
            return;
        }

        String sessionId = sessions.get(0).getId();

        // Get current modifications for a session
        Map<String, Object> modifications = client.recorder().getModifications(sessionId);
        System.out.println("Current modifications: " + modifications);

        // Update modifications (mask sensitive data, add headers)
        client.recorder().updateModifications(sessionId, Map.of(
                "requestModifications", Map.of(
                        "headers", Map.of(
                                "Authorization", "Bearer [REDACTED]"
                        ),
                        "bodyMasks", List.of("password", "secret", "token")
                ),
                "responseModifications", Map.of(
                        "headers", Map.of(
                                "Set-Cookie", "[REDACTED]"
                        ),
                        "bodyMasks", List.of("ssn", "creditCard")
                )
        ));
        System.out.println("Updated session modifications");

        // Replay a specific entry
        List<RecorderEntry> entries = client.recorder().getEntries(sessionId);
        if (!entries.isEmpty()) {
            client.recorder().replayEntry(sessionId, entries.get(0).getId());
            System.out.println("Replayed entry: " + entries.get(0).getId());
        }
    }
}
