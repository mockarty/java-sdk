// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ready-to-use Model Context Protocol (MCP) client.
 *
 * <p>Speaks to a Mockarty MCP endpoint (the admin node's streamable-HTTP
 * {@code /mcp}) so SDK users can discover and call the same agent-facing tool
 * surface an AI agent would — {@link #listTools()} then {@link #callTool} —
 * programmatically. Auth reuses the SDK client's credentials; feature/licence
 * gating is enforced server-side. The client performs the MCP {@code initialize}
 * handshake lazily on first use and reuses the negotiated session.
 */
public class McpApi {

    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final MockartyClient client;
    private final ObjectMapper mapper;
    private final String endpoint;

    private boolean initialized = false;
    private String sessionId = null;
    private long nextId = 0;

    public McpApi(MockartyClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
        String base = client.getConfig().getBaseUrl();
        this.endpoint = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/mcp";
    }

    /** A tool advertised by the MCP server. */
    public static final class McpTool {
        public final String name;
        public final String description;
        public final JsonNode inputSchema;

        McpTool(String name, String description, JsonNode inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }
    }

    /** Structured result of a {@link McpApi#callTool}. */
    public static final class McpToolResult {
        public final List<JsonNode> content;
        public final boolean isError;

        McpToolResult(List<JsonNode> content, boolean isError) {
            this.content = content;
            this.isError = isError;
        }

        /** Concatenated text of every text content block. */
        public String text() {
            StringBuilder sb = new StringBuilder();
            for (JsonNode c : content) {
                if ("text".equals(c.path("type").asText())) {
                    sb.append(c.path("text").asText());
                }
            }
            return sb.toString();
        }
    }

    /**
     * Performs the MCP handshake explicitly. Called automatically by
     * {@link #listTools()} / {@link #callTool} — use it to fail fast on a bad
     * token.
     */
    public synchronized void initialize() throws MockartyException {
        ensureInit();
    }

    private void ensureInit() throws MockartyException {
        if (initialized) {
            return;
        }
        Map<String, Object> params = Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "mockarty-java-sdk", "version", "1.0.0"));
        call("initialize", params);
        notifyMethod("notifications/initialized");
        initialized = true;
    }

    /** Returns every tool the MCP server advertises. */
    public synchronized List<McpTool> listTools() throws MockartyException {
        ensureInit();
        JsonNode result = call("tools/list", Map.of());
        List<McpTool> tools = new ArrayList<>();
        for (JsonNode t : result.path("tools")) {
            tools.add(new McpTool(
                    t.path("name").asText(),
                    t.path("description").asText(""),
                    t.get("inputSchema")));
        }
        return tools;
    }

    /**
     * Invokes a tool by name and returns its structured result.
     *
     * @param name      the tool name
     * @param arguments the tool arguments ({@code null} for a no-arg tool)
     */
    public synchronized McpToolResult callTool(String name, Map<String, Object> arguments) throws MockartyException {
        ensureInit();
        JsonNode result = call("tools/call", Map.of(
                "name", name,
                "arguments", arguments == null ? Map.of() : arguments));
        List<JsonNode> content = new ArrayList<>();
        for (JsonNode c : result.path("content")) {
            content.add(c);
        }
        return new McpToolResult(content, result.path("isError").asBoolean(false));
    }

    private JsonNode call(String method, Object params) throws MockartyException {
        nextId++;
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0", "id", nextId, "method", method, "params", params);
        JsonNode frame = roundtrip(body, true);
        JsonNode error = frame.get("error");
        if (error != null && !error.isNull()) {
            throw new MockartyException("mcp: rpc error " + error.path("code").asInt()
                    + ": " + error.path("message").asText());
        }
        return frame.path("result");
    }

    private void notifyMethod(String method) throws MockartyException {
        roundtrip(Map.of("jsonrpc", "2.0", "method", method), false);
    }

    private JsonNode roundtrip(Object body, boolean wantResponse) throws MockartyException {
        try {
            String payload = mapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(client.getConfig().getTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            String apiKey = client.getConfig().getApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            String ns = client.getConfig().getNamespace();
            if (ns != null && !ns.isEmpty()) {
                builder.header("X-Mockarty-Namespace", ns);
            }
            if (sessionId != null) {
                builder.header("Mcp-Session-Id", sessionId);
            }

            HttpClient http = client.getHttpClient();
            HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Optional<String> sid = resp.headers().firstValue("Mcp-Session-Id");
            sid.ifPresent(s -> this.sessionId = s);

            if (resp.statusCode() >= 400) {
                throw new MockartyException("mcp: HTTP " + resp.statusCode() + " from " + endpoint
                        + ": " + truncate(resp.body(), 200));
            }
            if (!wantResponse) {
                return null;
            }
            String contentType = resp.headers().firstValue("Content-Type").orElse("");
            String bodyStr = contentType.contains("text/event-stream")
                    ? extractSse(resp.body()) : resp.body();
            return mapper.readTree(bodyStr);
        } catch (MockartyException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new MockartyException("mcp: cannot reach " + endpoint + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MockartyException("mcp: interrupted calling " + endpoint, e);
        }
    }

    /** Pulls the JSON-RPC frame out of an SSE ({@code data:} line) response. */
    private static String extractSse(String body) throws MockartyException {
        StringBuilder data = new StringBuilder();
        // Split on \r?\n so a CRLF stream's blank separator is recognised as an
        // empty line (a bare "\n" split would leave "\r" and never break on the
        // event boundary, concatenating every event's data).
        for (String line : body.split("\r?\n")) {
            if (line.startsWith("data:")) {
                data.append(line.substring("data:".length()).trim());
            } else if (line.isEmpty() && data.length() > 0) {
                break;
            }
        }
        if (data.length() == 0) {
            throw new MockartyException("mcp: empty SSE response");
        }
        return data.toString();
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
