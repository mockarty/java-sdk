// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.model.FlowRunRequest;
import ru.mockarty.model.FlowRunResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the FlowRunsApi wire-shape + the FlowRunRequest /
 * Response Jackson serialisation. Wire-level live tests live in the
 * integration suite under {@code tests/integration/}.
 */
class FlowRunsApiTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("FlowRunRequest serialises flow + base_url (snake_case) with NON_NULL inclusion")
    void requestSerialisesWireShape() throws Exception {
        FlowRunRequest req = new FlowRunRequest()
                .flow(Map.of("ir_version", 1, "name", "smoke"))
                .baseURL("http://api.test");
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"flow\""), json);
        assertTrue(json.contains("\"base_url\":\"http://api.test\""), json);
        assertTrue(json.contains("\"ir_version\":1"), json);
    }

    @Test
    @DisplayName("FlowRunRequest omits null base_url from the wire")
    void requestOmitsNullBaseURL() throws Exception {
        FlowRunRequest req = new FlowRunRequest().flow(Map.of("ir_version", 1));
        String json = mapper.writeValueAsString(req);
        assertFalse(json.contains("base_url"), json);
    }

    @Test
    @DisplayName("FlowRunResponse deserialises camelCase admin envelope")
    void responseDeserialises() throws Exception {
        String json = "{"
                + "\"status\":\"passed\","
                + "\"durationMs\":42,"
                + "\"startedAt\":\"2026-05-25T12:00:00Z\","
                + "\"finishedAt\":\"2026-05-25T12:00:01Z\","
                + "\"variables\":{\"a\":1},"
                + "\"logs\":[\"hi\"],"
                + "\"errors\":[]"
                + "}";
        FlowRunResponse resp = mapper.readValue(json, FlowRunResponse.class);
        assertEquals("passed", resp.getStatus());
        assertEquals(42L, resp.getDurationMs());
        assertEquals("2026-05-25T12:00:00Z", resp.getStartedAt());
        assertEquals(Map.of("a", 1), resp.getVariables());
        assertEquals(List.of("hi"), resp.getLogs());
        assertNotNull(resp.getErrors());
        assertTrue(resp.getErrors().isEmpty());
    }

    @Test
    @DisplayName("FlowRunsApi#execute rejects null flow")
    void executeRejectsNullFlow() {
        FlowRunsApi api = new FlowRunsApi(null);
        assertThrows(IllegalArgumentException.class,
                () -> api.execute((Object) null));
    }

    @Test
    @DisplayName("FlowRunsApi#execute(FlowRunRequest) rejects null request")
    void executeRejectsNullRequest() {
        FlowRunsApi api = new FlowRunsApi(null);
        assertThrows(IllegalArgumentException.class,
                () -> api.execute((FlowRunRequest) null));
    }

    @Test
    @DisplayName("FlowRunsApi#executeJson rejects empty input")
    void executeJsonRejectsEmpty() {
        FlowRunsApi api = new FlowRunsApi(null);
        assertThrows(IllegalArgumentException.class, () -> api.executeJson("", null));
        assertThrows(IllegalArgumentException.class, () -> api.executeJson(null, null));
    }

    @Test
    @DisplayName("FlowRunsApi#executeJson rejects non-object JSON")
    void executeJsonRejectsNonObject() {
        FlowRunsApi api = new FlowRunsApi(null);
        assertThrows(IllegalArgumentException.class, () -> api.executeJson("[]", null));
    }

    @Test
    @DisplayName("FlowRunsApi#executeJson rejects malformed JSON")
    void executeJsonRejectsMalformed() {
        FlowRunsApi api = new FlowRunsApi(null);
        assertThrows(IllegalArgumentException.class,
                () -> api.executeJson("not json", null));
    }

    @Test
    @DisplayName("Fluent setters return self")
    void fluentSettersChain() {
        FlowRunRequest r = new FlowRunRequest().flow(Map.of()).baseURL("u");
        assertNotNull(r);
        assertEquals("u", r.getBaseURL());
    }
}
