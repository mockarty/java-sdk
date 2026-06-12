// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.pact.Matchers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Synchronous (request/response) message-pact tests — Pact V4
 * Synchronous/Messages. Async covered in MessagePactTest. */
class MessagePactSyncTest {

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    @DisplayName("sync message: contents=request, response[]=replies with matchingRules")
    void syncShape() throws Exception {
        MessagePact mp = new MessagePact("rpc-consumer", "rpc-provider")
                .given("a user exists")
                .expectsToReceive("get-user request/response")
                .withContent(Map.of("op", "getUser", "id", Matchers.integer(7)))
                .expectsResponse(Map.of("id", Matchers.integer(7), "name", Matchers.like("Alice")))
                .withResponseMetadata(Map.of("status", "ok"));

        JsonNode ix = M.readTree(mp.toJson()).path("interactions").get(0);
        assertEquals("Synchronous/Messages", ix.path("type").asText());
        assertFalse(ix.path("contents").isMissingNode(), "request contents");
        JsonNode resp = ix.path("response");
        assertTrue(resp.isArray() && resp.size() == 1, "one response");
        JsonNode r0 = resp.get(0);
        assertFalse(r0.path("contents").isMissingNode(), "reply contents");
        assertEquals("ok", r0.path("metadata").path("status").asText());
        // matchers in the reply produce body matchingRules rooted at $.
        assertEquals("integer",
                r0.path("matchingRules").path("body").path("$.id").path("matchers").get(0).path("match").asText());
    }

    @Test
    @DisplayName("async message still serialises as Asynchronous/Messages")
    void asyncUnchanged() throws Exception {
        MessagePact mp = new MessagePact("c", "p")
                .given("s").expectsToReceive("evt").withContent(Map.of("a", 1));
        JsonNode ix = M.readTree(mp.toJson()).path("interactions").get(0);
        assertEquals("Asynchronous/Messages", ix.path("type").asText());
        assertTrue(ix.path("response").isMissingNode());
    }
}
