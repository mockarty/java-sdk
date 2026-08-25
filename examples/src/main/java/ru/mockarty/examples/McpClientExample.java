// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.api.McpApi;

import java.util.List;
import java.util.Map;

/**
 * Drives Mockarty's agent-facing MCP tool surface from the SDK.
 *
 * <p>Connects to the admin node's streamable-HTTP {@code /mcp} endpoint, lists
 * every tool the server advertises, then calls one and reads its structured
 * result. The MCP client reuses the SDK client's server URL + API key;
 * feature/licence gating for the tools is enforced server-side.
 */
public class McpClientExample {

    public static void main(String[] args) throws Exception {
        String baseUrl = System.getenv().getOrDefault("MOCKARTY_SERVER", "http://localhost:5770");
        String apiKey = System.getenv("MOCKARTY_API_KEY");

        try (MockartyClient client = MockartyClient.create(baseUrl, apiKey)) {
            McpApi mcp = client.mcp();

            // 1. Discover the tools the server exposes.
            List<McpApi.McpTool> tools = mcp.listTools();
            System.out.printf("Server advertises %d MCP tools:%n", tools.size());
            for (McpApi.McpTool t : tools) {
                System.out.printf("  - %-28s %s%n", t.name, t.description);
            }

            // 2. Call a read-only tool and read its JSON result.
            McpApi.McpToolResult result = mcp.callTool("list_mocks", Map.of());
            if (result.isError) {
                throw new RuntimeException("tool returned an error: " + result.text());
            }
            System.out.println("\nlist_mocks result:");
            System.out.println(result.text());
        }
    }
}
