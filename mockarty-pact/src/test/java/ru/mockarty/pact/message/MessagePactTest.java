// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mockarty.pact.verifier.InteractionResult;
import ru.mockarty.pact.verifier.VerificationResult;
import ru.mockarty.pact.verifier.Verifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MessagePactTest {

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    void requiresConsumerAndProvider() {
        assertThrows(IllegalArgumentException.class,
            () -> new MessagePact("", "p"));
        assertThrows(IllegalArgumentException.class,
            () -> new MessagePact("c", ""));
    }

    @Test
    void v4Shape() throws Exception {
        MessagePact mp = new MessagePact("OrderConsumer", "OrderEvents")
            .given("user 42 exists")
            .expectsToReceive("an order-created event")
            .withMetadata(Map.of("topic", "orders"))
            .withContent(Map.of("orderId", 42, "status", "open"));

        JsonNode doc = M.readTree(mp.toJson());
        assertEquals("OrderConsumer", doc.path("consumer").path("name").asText());
        assertEquals(1, doc.path("interactions").size());
        JsonNode ix = doc.path("interactions").get(0);
        assertEquals(MessagePact.MESSAGE_INTERACTION_TYPE, ix.path("type").asText());
        assertEquals("an order-created event", ix.path("description").asText());
        assertEquals("application/json",
            ix.path("contents").path("contentType").asText());
    }

    @Test
    void v3Shape() throws Exception {
        MessagePact mp = new MessagePact("c", "p", "3.0.0")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("k", "v"));
        JsonNode doc = M.readTree(mp.toJson());
        assertTrue(doc.has("messages"));
        assertFalse(doc.has("interactions"));
    }

    @Test
    void andGivenAppendsToCurrentMessage() throws Exception {
        // V4 supports multi-state messages — andGiven() appends a
        // state to the CURRENT message, distinct from given() which
        // starts a NEW message.
        MessagePact mp = new MessagePact("c", "p")
            .given("state-A", Map.of("k", "v"))
            .andGiven("state-B")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 1));

        JsonNode doc = M.readTree(mp.toJson());
        assertEquals(1, doc.path("interactions").size(),
            "andGiven should NOT spawn a new message");
        JsonNode states = doc.path("interactions").get(0).path("providerStates");
        assertEquals(2, states.size());
        assertEquals("state-A", states.get(0).path("name").asText());
        assertEquals("state-B", states.get(1).path("name").asText());
    }

    @Test
    void andGivenRequiresCursor() {
        MessagePact mp = new MessagePact("c", "p");
        assertThrows(IllegalStateException.class,
            () -> mp.andGiven("first"),
            "andGiven before given() must fail loud");
    }

    @Test
    void requiresGivenCursor() {
        MessagePact mp = new MessagePact("c", "p");
        assertThrows(IllegalStateException.class,
            () -> mp.expectsToReceive("oops"));
        assertThrows(IllegalStateException.class,
            () -> mp.withContent(Map.of()));
    }

    @Test
    void consumerVerifyHappyPath() throws Exception {
        MessagePact mp = new MessagePact("c", "p")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 7));
        AtomicReference<byte[]> seen = new AtomicReference<>();
        mp.verify((bytes, meta) -> seen.set(bytes));
        assertNotNull(seen.get());
        assertTrue(new String(seen.get(), StandardCharsets.UTF_8).contains("\"id\""));
    }

    @Test
    void consumerVerifyRejects() {
        MessagePact mp = new MessagePact("c", "p")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 7));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> mp.verify((bytes, meta) -> { throw new RuntimeException("cannot parse"); }));
        assertTrue(ex.getMessage().contains("cannot parse"));
    }

    @Test
    void consumerVerifyNullHandler() {
        MessagePact mp = new MessagePact("c", "p");
        assertThrows(IllegalArgumentException.class, () -> mp.verify(null));
    }

    @Test
    void writeFile(@TempDir Path tmp) throws Exception {
        MessagePact mp = new MessagePact("OrderConsumer", "OrderEvents")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 1));
        Path out = mp.writeFile(tmp);
        assertEquals(tmp, out.getParent());
        String body = Files.readString(out);
        assertTrue(body.contains(MessagePact.MESSAGE_INTERACTION_TYPE));
    }

    @Test
    void parseV4Doc() {
        MessagePact mp = new MessagePact("c", "p")
            .given("st")
            .expectsToReceive("msg")
            .withMetadata(Map.of("k", "v"))
            .withContent(Map.of("id", 1));
        List<MessagePact.Message> msgs = MessagePactParser.parse(mp.toJson());
        assertEquals(1, msgs.size());
        assertEquals("msg", msgs.get(0).description);
        assertEquals("v", msgs.get(0).metadata.get("k"));
    }

    @Test
    void parseV3Doc() {
        MessagePact mp = new MessagePact("c", "p", "3.0.0")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 1));
        List<MessagePact.Message> msgs = MessagePactParser.parse(mp.toJson());
        assertEquals(1, msgs.size());
        assertEquals("st", msgs.get(0).states.get(0).get("name"));
    }

    @Test
    void parseGarbage() {
        assertThrows(IllegalArgumentException.class,
            () -> MessagePactParser.parse("<<not json>>".getBytes()));
    }

    @Test
    void verifierMessageHappyPath() throws Exception {
        byte[] raw = new MessagePact("c", "p")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 7))
            .toJson();
        Verifier v = Verifier.builder()
            .providerUrl("http://x")
            .messageProducer("msg",
                (desc, states) -> new Verifier.MessagePayload(
                    "{\"id\": 7}".getBytes(StandardCharsets.UTF_8), Map.of()))
            .build();
        VerificationResult res = v.verifyMessagePactBytes(raw);
        assertTrue(res.ok(), res.summary());
    }

    @Test
    void verifierMessageContentMismatch() throws Exception {
        byte[] raw = new MessagePact("c", "p")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 7))
            .toJson();
        Verifier v = Verifier.builder()
            .providerUrl("http://x")
            .messageProducer("msg",
                (desc, states) -> new Verifier.MessagePayload(
                    "{\"id\": 999}".getBytes(StandardCharsets.UTF_8), Map.of()))
            .build();
        VerificationResult res = v.verifyMessagePactBytes(raw);
        assertFalse(res.ok());
        assertFalse(res.interactions().get(0).mismatches().isEmpty());
    }

    @Test
    void verifierNoProducer() throws Exception {
        byte[] raw = new MessagePact("c", "p")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 7))
            .toJson();
        Verifier v = Verifier.builder().providerUrl("http://x").build();
        VerificationResult res = v.verifyMessagePactBytes(raw);
        assertFalse(res.ok());
        InteractionResult ir = res.interactions().get(0);
        assertTrue(ir.error().contains("no MessageProducer"), ir.error());
    }

    @Test
    void verifierProducerErrors() throws Exception {
        byte[] raw = new MessagePact("c", "p")
            .given("st")
            .expectsToReceive("msg")
            .withContent(Map.of("id", 7))
            .toJson();
        Verifier v = Verifier.builder()
            .providerUrl("http://x")
            .messageProducer("msg",
                (desc, states) -> { throw new RuntimeException("kafka down"); })
            .build();
        VerificationResult res = v.verifyMessagePactBytes(raw);
        assertTrue(res.interactions().get(0).error().contains("kafka down"));
    }
}
