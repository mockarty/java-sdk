// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.verifier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.pact.broker.BrokerClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VerifierTest {

    private static final String SIMPLE_PACT = """
        {
          "consumer": {"name": "OrderClient"},
          "provider": {"name": "OrderAPI"},
          "interactions": [
            {
              "description": "fetch order 42",
              "providerStates": [{"name": "order 42 exists"}],
              "request":  {"method": "GET", "path": "/orders/42"},
              "response": {"status": 200,
                           "headers": {"Content-Type": "application/json"},
                           "body": {"id": 42}}
            }
          ]
        }
        """;

    private static final String V3_PACT = """
        {
          "consumer": {"name": "OrderClient"},
          "provider": {"name": "OrderAPI"},
          "interactions": [
            {
              "description": "fetch one",
              "providerState": "data exists",
              "request":  {"method": "GET", "path": "/x"},
              "response": {"status": 204}
            }
          ]
        }
        """;

    private HttpServer provider;
    private int providerPort;
    private final AtomicInteger respStatus = new AtomicInteger(200);
    private final AtomicReference<byte[]> respBody =
        new AtomicReference<>("{\"id\":42}".getBytes(StandardCharsets.UTF_8));
    private final AtomicReference<String> respCT = new AtomicReference<>("application/json");
    private final AtomicReference<String> lastAuth = new AtomicReference<>("");

    @BeforeEach
    void startProvider() throws IOException {
        provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        providerPort = provider.getAddress().getPort();
        provider.createContext("/", this::handle);
        provider.start();
    }

    @AfterEach
    void stopProvider() { provider.stop(0); }

    private void handle(HttpExchange ex) throws IOException {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null) lastAuth.set(auth);
        try (InputStream is = ex.getRequestBody()) { is.readAllBytes(); }
        ex.getResponseHeaders().add("Content-Type", respCT.get());
        byte[] body = respBody.get();
        ex.sendResponseHeaders(respStatus.get(), body.length == 0 ? -1 : body.length);
        if (body.length > 0) ex.getResponseBody().write(body);
        ex.close();
    }

    private String providerUrl() { return "http://127.0.0.1:" + providerPort; }

    @Test
    void providerUrlRequired() {
        assertThrows(NullPointerException.class,
            () -> Verifier.builder().build());
    }

    @Test
    void happyPath() throws Exception {
        Verifier v = Verifier.builder().providerUrl(providerUrl()).build();
        VerificationResult res = v.verifyPactBytes(SIMPLE_PACT.getBytes());
        assertTrue(res.ok(), res.summary());
        assertEquals("order 42 exists", res.interactions().get(0).state());
    }

    @Test
    void statusMismatch() throws Exception {
        respStatus.set(500);
        Verifier v = Verifier.builder().providerUrl(providerUrl()).build();
        VerificationResult res = v.verifyPactBytes(SIMPLE_PACT.getBytes());
        assertFalse(res.ok());
        assertTrue(res.interactions().get(0).mismatches().stream()
            .anyMatch(m -> "$.status".equals(m.path())));
    }

    @Test
    void bodyMismatch() throws Exception {
        respBody.set("{\"id\":99}".getBytes(StandardCharsets.UTF_8));
        Verifier v = Verifier.builder().providerUrl(providerUrl()).build();
        VerificationResult res = v.verifyPactBytes(SIMPLE_PACT.getBytes());
        assertFalse(res.ok());
        assertTrue(res.interactions().get(0).mismatches().stream()
            .anyMatch(m -> m.path().contains("id")));
    }

    @Test
    void bigIntComparisonNoDoubleLoss() throws Exception {
        // 9007199254740993 = 2^53 + 1 — first integer beyond IEEE-754
        // double precision. Pre-fix the verifier compared via
        // asDouble(), so an actual value of 9007199254740992 (2^53)
        // would falsely match the expected 2^53+1.
        String pact = """
            {
              "consumer": {"name": "c"},
              "provider": {"name": "p"},
              "interactions": [{
                "description": "id check",
                "request":  {"method": "GET", "path": "/orders/42"},
                "response": {"status": 200,
                             "headers": {"Content-Type":"application/json"},
                             "body": {"id": 9007199254740993}}
              }]
            }
            """;
        respBody.set("{\"id\": 9007199254740992}".getBytes(StandardCharsets.UTF_8));
        Verifier v = Verifier.builder().providerUrl(providerUrl()).build();
        VerificationResult res = v.verifyPactBytes(pact.getBytes());
        assertFalse(res.ok(), "BigInteger precision lost: " + res.interactions().get(0).mismatches());
    }

    @Test
    void numericComparisonAcceptsIntAndDecimalEquivalence() throws Exception {
        // Same numeric value rendered as int (1) and as float (1.0)
        // must compare equal — BigDecimal.compareTo + stripTrailingZeros.
        String pact = """
            {
              "consumer": {"name": "c"},
              "provider": {"name": "p"},
              "interactions": [{
                "description": "n",
                "request":  {"method": "GET", "path": "/orders/42"},
                "response": {"status": 200,
                             "headers": {"Content-Type":"application/json"},
                             "body": {"id": 1}}
              }]
            }
            """;
        respBody.set("{\"id\": 1.0}".getBytes(StandardCharsets.UTF_8));
        Verifier v = Verifier.builder().providerUrl(providerUrl()).build();
        VerificationResult res = v.verifyPactBytes(pact.getBytes());
        assertTrue(res.ok(), "1 and 1.0 should compare equal: "
            + res.interactions().get(0).mismatches());
    }

    @Test
    void stateHandlerInvoked() throws Exception {
        AtomicInteger seen = new AtomicInteger(0);
        Verifier v = Verifier.builder()
            .providerUrl(providerUrl())
            .stateHandler("order 42 exists", (state, params) -> seen.incrementAndGet())
            .build();
        VerificationResult res = v.verifyPactBytes(SIMPLE_PACT.getBytes());
        assertTrue(res.ok(), res.summary());
        assertEquals(1, seen.get());
    }

    @Test
    void stateHandlerErrorBlocksReplay() throws Exception {
        Verifier v = Verifier.builder()
            .providerUrl(providerUrl())
            .stateHandler("order 42 exists", (state, params) -> {
                throw new RuntimeException("DB down");
            })
            .build();
        VerificationResult res = v.verifyPactBytes(SIMPLE_PACT.getBytes());
        assertFalse(res.ok());
        assertTrue(res.interactions().get(0).error().contains("state setup"));
        assertTrue(res.interactions().get(0).error().contains("DB down"));
    }

    @Test
    void stateSetupUrl() throws Exception {
        AtomicInteger hits = new AtomicInteger(0);
        AtomicReference<byte[]> setupBody = new AtomicReference<>(new byte[0]);
        HttpServer setup = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        setup.createContext("/", ex -> {
            hits.incrementAndGet();
            try (InputStream is = ex.getRequestBody()) {
                setupBody.set(is.readAllBytes());
            }
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        setup.start();
        try {
            Verifier v = Verifier.builder()
                .providerUrl(providerUrl())
                .stateSetupUrl("http://127.0.0.1:" + setup.getAddress().getPort() + "/setup")
                .build();
            VerificationResult res = v.verifyPactBytes(SIMPLE_PACT.getBytes());
            assertTrue(res.ok(), res.summary());
            assertEquals(1, hits.get());
            assertTrue(new String(setupBody.get(), StandardCharsets.UTF_8).contains("order 42 exists"));
        } finally {
            setup.stop(0);
        }
    }

    @Test
    void requestFilter() throws Exception {
        Verifier v = Verifier.builder()
            .providerUrl(providerUrl())
            .requestFilter(req -> req.headers().put("Authorization", "Bearer test"))
            .build();
        VerificationResult res = v.verifyPactBytes(SIMPLE_PACT.getBytes());
        assertTrue(res.ok(), res.summary());
        assertEquals("Bearer test", lastAuth.get());
    }

    @Test
    void v3SingularState() throws Exception {
        respStatus.set(204);
        respBody.set(new byte[0]);
        AtomicReference<String> seen = new AtomicReference<>("");
        Verifier v = Verifier.builder()
            .providerUrl(providerUrl())
            .stateHandler("data exists", (state, params) -> seen.set(state))
            .build();
        VerificationResult res = v.verifyPactBytes(V3_PACT.getBytes());
        assertTrue(res.ok(), res.summary());
        assertEquals("data exists", seen.get());
    }

    @Test
    void verifyFromFile(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path p = tmp.resolve("pact.json");
        Files.writeString(p, SIMPLE_PACT);
        Verifier v = Verifier.builder().providerUrl(providerUrl()).build();
        VerificationResult res = v.verifyPactFile(p);
        assertTrue(res.ok());
    }

    @Test
    void verifyFromBrokerRequiresBroker() {
        Verifier v = Verifier.builder().providerUrl("http://x").build();
        assertThrows(IllegalStateException.class,
            () -> v.verifyFromBroker("c", "p", "v"));
    }

    @Test
    void verifyFromBrokerRoundtrip() throws Exception {
        HttpServer brokerSrv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        brokerSrv.createContext("/", ex -> {
            byte[] body = SIMPLE_PACT.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        brokerSrv.start();
        try {
            BrokerClient bc = BrokerClient.builder()
                .baseUrl("http://127.0.0.1:" + brokerSrv.getAddress().getPort())
                .build();
            Verifier v = Verifier.builder()
                .providerUrl(providerUrl())
                .broker(bc)
                .build();
            VerificationResult res = v.verifyFromBroker("OrderClient", "OrderAPI", "1.0");
            assertTrue(res.ok(), res.summary());
        } finally {
            brokerSrv.stop(0);
        }
    }

    @Test
    void publishResults() throws Exception {
        AtomicReference<byte[]> captured = new AtomicReference<>(new byte[0]);
        AtomicReference<String> capturedAuth = new AtomicReference<>("");
        AtomicReference<String> capturedPath = new AtomicReference<>("");
        HttpServer brokerSrv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        brokerSrv.createContext("/", ex -> {
            capturedPath.set(ex.getRequestURI().toString());
            String a = ex.getRequestHeaders().getFirst("Authorization");
            if (a != null) capturedAuth.set(a);
            try (InputStream is = ex.getRequestBody()) {
                captured.set(is.readAllBytes());
            }
            ex.sendResponseHeaders(201, -1);
            ex.close();
        });
        brokerSrv.start();
        try {
            BrokerClient bc = BrokerClient.builder()
                .baseUrl("http://127.0.0.1:" + brokerSrv.getAddress().getPort())
                .token("tok-42")
                .build();
            Verifier v = Verifier.builder()
                .providerUrl("http://x")
                .providerName("OrderAPI")
                .providerVersion("1.2.3")
                .broker(bc)
                .build();
            VerificationResult res = new VerificationResult("OrderAPI",
                java.time.Instant.now(), java.time.Instant.now(),
                List.of(new InteractionResult("fetch order 42", "", 200, true, "", List.of())));
            v.publishResults("OrderClient", "OrderAPI", "1.0", res);
            String payload = new String(captured.get(), StandardCharsets.UTF_8);
            assertTrue(payload.contains("\"success\":true"), payload);
            assertTrue(payload.contains("\"providerApplicationVersion\":\"1.2.3\""), payload);
            assertTrue(capturedPath.get().endsWith("/verification-results"), capturedPath.get());
            assertEquals("Bearer tok-42", capturedAuth.get());
        } finally {
            brokerSrv.stop(0);
        }
    }

    @Test
    void publishResultsRequiresVersion() {
        BrokerClient bc = BrokerClient.builder().baseUrl("http://x").build();
        Verifier v = Verifier.builder().providerUrl("http://y").broker(bc).build();
        assertThrows(IllegalStateException.class,
            () -> v.publishResults("c", "p", "v",
                new VerificationResult("", java.time.Instant.now(),
                    java.time.Instant.now(), List.of())));
    }

    @Test
    void garbagePactRejected() {
        Verifier v = Verifier.builder().providerUrl("http://x").build();
        assertThrows(IllegalArgumentException.class,
            () -> v.verifyPactBytes("<<not json>>".getBytes()));
    }

    @Test
    void nonDictInteractionEntriesCoerced() throws Exception {
        // Adversarial pact: non-object interaction entries. Verifier
        // should coerce them to empty interactions (which then fail at
        // HTTP step against unreachable provider URL) — not panic.
        String adversarial = "{\"interactions\": [\"not-a-dict\", 42, null, {}]}";
        Verifier v = Verifier.builder().providerUrl("http://127.0.0.1:1").build();
        VerificationResult res = v.verifyPactBytes(adversarial.getBytes());
        assertEquals(4, res.interactions().size());
        for (InteractionResult ir : res.interactions()) {
            assertFalse(ir.passed());
        }
    }
}
