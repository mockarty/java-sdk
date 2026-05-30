// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalStep;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Allure → ExternalRunRequest mapping inside
 * {@link ExternalRunsApi#allureToExternalRun}. Tests the parsing path
 * without hitting the wire — wire-side flow is covered by the
 * integration suite under {@code tests/integration/}.
 */
class ExternalRunsApiAllureMappingTest {

    @TempDir
    Path tmp;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Allure passed result → ExternalRunRequest.status=passed + duration + caseName")
    void passedResultMapping() throws Exception {
        String json = "{"
                + "\"uuid\":\"u-1\","
                + "\"name\":\"login\","
                + "\"fullName\":\"ru.example.LoginTest.login\","
                + "\"status\":\"passed\","
                + "\"start\":1000,\"stop\":1250,"
                + "\"labels\":[{\"name\":\"feature\",\"value\":\"Login\"}],"
                + "\"steps\":[{\"name\":\"send\",\"status\":\"passed\",\"start\":1010,\"stop\":1240}]"
                + "}";
            JsonNode root = mapper.readTree(json);
        ExternalRunRequest req = ExternalRunsApi.allureToExternalRun(root, tmp, mapper);

        assertEquals(ExternalRunRequest.STATUS_PASSED, req.getStatus());
        assertEquals("login", req.getTestDisplayName());
        assertEquals("ru.example.LoginTest.login", req.getCaseName());
        assertEquals("u-1", req.getExternalId());
        assertEquals(250L, req.getDurationMs());
        assertTrue(req.isAutoCreate(), "no AS_ID label → autoCreate must be true");
        assertNotNull(req.getSteps());
        assertEquals(1, req.getSteps().size());
        ExternalStep es = req.getSteps().get(0);
        assertEquals("send", es.getName());
        assertEquals(230L, es.getDurationMs());
    }

    @Test
    @DisplayName("Allure failed status → ExternalRunRequest.status=failed + error from statusDetails")
    void failedStatusMapping() throws Exception {
        String json = "{"
                + "\"uuid\":\"u-2\","
                + "\"name\":\"login fail\","
                + "\"fullName\":\"ru.x.LoginTest.fail\","
                + "\"status\":\"failed\","
                + "\"start\":0,\"stop\":0,"
                + "\"statusDetails\":{\"message\":\"AssertionError\",\"trace\":\"trace\"}"
                + "}";
        JsonNode root = mapper.readTree(json);
        ExternalRunRequest req = ExternalRunsApi.allureToExternalRun(root, tmp, mapper);
        assertEquals(ExternalRunRequest.STATUS_FAILED, req.getStatus());
        assertTrue(req.getError().contains("AssertionError"));
        assertTrue(req.getError().contains("trace"));
    }

    @Test
    @DisplayName("AS_ID label pins caseId and clears autoCreate")
    void asIdLabelPinsCaseId() throws Exception {
        String json = "{"
                + "\"uuid\":\"u-3\","
                + "\"name\":\"login\","
                + "\"status\":\"passed\","
                + "\"labels\":[{\"name\":\"AS_ID\",\"value\":\"CASE-LOGIN-1\"}]"
                + "}";
        JsonNode root = mapper.readTree(json);
        ExternalRunRequest req = ExternalRunsApi.allureToExternalRun(root, tmp, mapper);
        assertEquals("CASE-LOGIN-1", req.getCaseId());
        assertFalse(req.isAutoCreate(), "AS_ID label must disable autoCreate");
    }

    @Test
    @DisplayName("Nested step trees flatten with slash-joined names")
    void nestedStepsFlatten() throws Exception {
        String json = "{"
                + "\"uuid\":\"u-4\",\"name\":\"x\",\"status\":\"passed\","
                + "\"steps\":[{\"name\":\"outer\",\"status\":\"passed\","
                + "  \"steps\":[{\"name\":\"inner\",\"status\":\"passed\"}]}]}";
        JsonNode root = mapper.readTree(json);
        ExternalRunRequest req = ExternalRunsApi.allureToExternalRun(root, tmp, mapper);
        assertEquals(2, req.getSteps().size());
        assertEquals("outer", req.getSteps().get(0).getName());
        assertEquals("outer / inner", req.getSteps().get(1).getName());
    }

    @Test
    @DisplayName("Attachment body is loaded from disk when source file exists")
    void attachmentBodyLoaded() throws Exception {
        Path src = tmp.resolve("att-1.txt");
        Files.write(src, "hello".getBytes());
        String json = "{"
                + "\"uuid\":\"u-5\",\"name\":\"x\",\"status\":\"passed\","
                + "\"attachments\":[{\"name\":\"log\",\"source\":\"att-1.txt\",\"type\":\"text/plain\"}]}";
        JsonNode root = mapper.readTree(json);
        ExternalRunRequest req = ExternalRunsApi.allureToExternalRun(root, tmp, mapper);
        assertNotNull(req.getAttachments());
        assertEquals(1, req.getAttachments().size());
        assertEquals("log", req.getAttachments().get(0).getName());
        assertEquals("text/plain", req.getAttachments().get(0).getContentType());
        // ExternalAttachment.body() stores bytes as base64 — decode back to verify.
        String b64 = req.getAttachments().get(0).getBodyB64();
        assertNotNull(b64);
        assertEquals("hello", new String(java.util.Base64.getDecoder().decode(b64)));
    }

    @Test
    @DisplayName("Unknown Allure status maps to broken")
    void unknownStatusMapsToBroken() throws Exception {
        String json = "{\"uuid\":\"u-6\",\"name\":\"x\",\"status\":\"unknown\"}";
        JsonNode root = mapper.readTree(json);
        ExternalRunRequest req = ExternalRunsApi.allureToExternalRun(root, tmp, mapper);
        assertEquals(ExternalRunRequest.STATUS_BROKEN, req.getStatus());
    }
}
