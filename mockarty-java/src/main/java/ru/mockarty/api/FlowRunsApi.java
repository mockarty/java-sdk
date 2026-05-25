// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.FlowRunRequest;
import ru.mockarty.model.FlowRunResponse;

import java.util.Map;

/**
 * Client for {@code POST /api/v1/api-tester/flow-runs} — the server-side
 * IR runner.
 *
 * <p>Companion to the admin handler in
 * {@code internal/webui/api_tester_flow_run_handler.go}. Lets the Java
 * SDK ship a canonical Mockarty IR Flow at the server and receive the
 * aggregated RunResult, without dragging a goja runtime into the Java
 * test process.</p>
 *
 * <p>Wire shape mirrors the Go + Python SDKs 1:1 so multi-language
 * harnesses stay portable.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * Map<String, Object> flow = Map.of(
 *     "ir_version", 1,
 *     "name", "smoke",
 *     "steps", List.of() // IR steps go here
 * );
 * FlowRunResponse resp = client.flowRuns().execute(flow, "http://api.test");
 * if (!"passed".equals(resp.getStatus())) {
 *     throw new AssertionError(resp.getErrors());
 * }
 * }</pre>
 */
public class FlowRunsApi {

    private static final String PATH = "/api/v1/api-tester/flow-runs";

    private final MockartyClient client;

    public FlowRunsApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Ship a Flow at the server and return the aggregated RunResult.
     *
     * @param flow    the canonical IR Flow as a Jackson-serialisable
     *                object — typically a {@code Map<String,Object>} or
     *                a pre-built POJO. The SDK has no hard dependency
     *                on the server's internal IR types so callers can
     *                pass whatever shape they have.
     * @param baseURL optional HTTP base prefix injected into the
     *                generated JS (equivalent to
     *                {@code iruir.RunIROptions.BaseURL}). Pass
     *                {@code null} or empty when the IR carries
     *                absolute URLs.
     * @return the aggregated run result.
     */
    public FlowRunResponse execute(Object flow, String baseURL) throws MockartyException {
        if (flow == null) {
            throw new IllegalArgumentException("flow is required");
        }
        FlowRunRequest request = new FlowRunRequest();
        request.setFlow(flow);
        if (baseURL != null && !baseURL.isEmpty()) {
            request.setBaseURL(baseURL);
        }
        return client.post(PATH, request, FlowRunResponse.class);
    }

    /** Convenience overload — no base-url override. */
    public FlowRunResponse execute(Object flow) throws MockartyException {
        return execute(flow, null);
    }

    /**
     * Lower-level entry point — accepts the request wrapper directly,
     * exposed for callers that already speak the wire envelope (e.g.
     * a generic round-tripper). Same wire-shape contract.
     */
    public FlowRunResponse execute(FlowRunRequest request) throws MockartyException {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        return client.post(PATH, request, FlowRunResponse.class);
    }

    /**
     * Convenience: parse a JSON string into a generic shape and execute.
     * Useful when the caller has the IR on disk as a {@code .json} file.
     */
    public FlowRunResponse executeJson(String flowJson, String baseURL) throws MockartyException {
        if (flowJson == null || flowJson.isEmpty()) {
            throw new IllegalArgumentException("flowJson is empty");
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tree = mapper.readTree(flowJson);
            if (!tree.isObject()) {
                throw new IllegalArgumentException("flow JSON must decode to an object");
            }
            // Use a generic Map view to stay decoupled from internal IR types.
            @SuppressWarnings("unchecked")
            Map<String, Object> flowMap = mapper.convertValue(tree, Map.class);
            return execute(flowMap, baseURL);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("flow is not valid JSON: " + e.getMessage(), e);
        }
    }
}
