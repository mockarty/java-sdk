// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the JSON wire shape of the Mockarty-extension fields that the Go and
 * Python SDKs already carried but the Java SDK was missing: testCaseId,
 * fullName, caseDescription, caseExpectedResult, customFields and
 * claimCaseOwnership. Without these a Java caller could not pin a case by its
 * author-id/full-name or claim ownership — parity gap with the other SDKs.
 */
class ExternalRunRequestFieldsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("extension fields serialize to their server JSON keys")
    void serializesExtensionFields() throws Exception {
        ExternalRunRequest req = new ExternalRunRequest()
                .status(ExternalRunRequest.STATUS_PASSED)
                .caseName("login works")
                .testCaseId("CASE-1")
                .fullName("pkg.Auth.login_works")
                .caseDescription("desc")
                .caseExpectedResult("200 OK")
                .claimCaseOwnership(true)
                .customFields(List.of(new CustomField("team", "auth").type("string")));

        JsonNode node = mapper.readTree(mapper.writeValueAsString(req));

        assertEquals("CASE-1", node.get("testCaseId").asText());
        assertEquals("pkg.Auth.login_works", node.get("fullName").asText());
        assertEquals("desc", node.get("caseDescription").asText());
        assertEquals("200 OK", node.get("caseExpectedResult").asText());
        assertTrue(node.get("claimCaseOwnership").asBoolean());
        assertEquals("team", node.get("customFields").get(0).get("name").asText());
        assertEquals("auth", node.get("customFields").get(0).get("value").asText());
        assertEquals("string", node.get("customFields").get(0).get("type").asText());
    }

    @Test
    @DisplayName("unset extension fields are omitted (NON_DEFAULT)")
    void omitsUnsetFields() throws Exception {
        ExternalRunRequest req = new ExternalRunRequest()
                .status(ExternalRunRequest.STATUS_PASSED)
                .caseName("c");

        JsonNode node = mapper.readTree(mapper.writeValueAsString(req));

        assertFalse(node.has("testCaseId"), "unset testCaseId must not be emitted");
        assertFalse(node.has("fullName"), "unset fullName must not be emitted");
        assertFalse(node.has("claimCaseOwnership"), "false flag must not be emitted");
        assertFalse(node.has("customFields"), "null list must not be emitted");
    }

    @Test
    @DisplayName("getters round-trip the extension fields")
    void gettersRoundTrip() {
        ExternalRunRequest req = new ExternalRunRequest()
                .testCaseId("CASE-9")
                .fullName("fn")
                .customFields(Collections.emptyList())
                .claimCaseOwnership(true);

        assertEquals("CASE-9", req.getTestCaseId());
        assertEquals("fn", req.getFullName());
        assertEquals(Collections.emptyList(), req.getCustomFields());
        assertTrue(req.isClaimCaseOwnership());
    }
}
