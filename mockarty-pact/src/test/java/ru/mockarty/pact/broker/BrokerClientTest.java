// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.broker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BrokerClientTest {

    private static final String SAMPLE_PACT = """
        {
          "consumer": {"name": "OrderClient"},
          "provider": {"name": "OrderAPI"},
          "interactions": [],
          "metadata": {"pactSpecification": {"version": "4.0"}}
        }
        """;

    private HttpServer server;
    private int port;

    // Captured state for the latest non-tag request and all tag requests.
    private final AtomicReference<String> lastMethod = new AtomicReference<>("");
    private final AtomicReference<String> lastPath = new AtomicReference<>("");
    private final AtomicReference<String> lastAuth = new AtomicReference<>("");
    private final AtomicReference<String> lastBranch = new AtomicReference<>("");
    private final AtomicReference<byte[]> lastBody = new AtomicReference<>(new byte[0]);
    private final List<String> tagPaths = new ArrayList<>();

    // Programmable response.
    private int respStatus = 200;
    private byte[] respBody = "{}".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().toString();
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        String branch = ex.getRequestHeaders().getFirst("X-Pact-Consumer-Branch");
        byte[] body;
        try (InputStream is = ex.getRequestBody()) {
            body = is.readAllBytes();
        }
        if (path.contains("/tags/")) {
            synchronized (tagPaths) { tagPaths.add(path); }
        } else {
            lastMethod.set(method);
            lastPath.set(path);
            if (auth != null) lastAuth.set(auth);
            if (branch != null) lastBranch.set(branch);
            lastBody.set(body);
        }
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(respStatus, respBody.length);
        ex.getResponseBody().write(respBody);
        ex.close();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    void requiresBaseUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> BrokerClient.builder().build());
    }

    @Test
    void publishHappyPath() throws Exception {
        respStatus = 201;
        BrokerClient c = BrokerClient.builder().baseUrl(baseUrl()).token("tok-42").build();
        c.publish(SAMPLE_PACT.getBytes(StandardCharsets.UTF_8), "1.0.0", "", null);
        assertEquals("PUT", lastMethod.get());
        assertEquals("/pacts/provider/OrderAPI/consumer/OrderClient/version/1.0.0",
            lastPath.get());
        assertEquals("Bearer tok-42", lastAuth.get());
        assertArrayEquals(SAMPLE_PACT.getBytes(StandardCharsets.UTF_8), lastBody.get());
    }

    @Test
    void publishBasicAuthFallback() throws Exception {
        BrokerClient c = BrokerClient.builder()
            .baseUrl(baseUrl())
            .basicAuth("ci", "s3cret")
            .build();
        c.publish(SAMPLE_PACT.getBytes(StandardCharsets.UTF_8), "1.0", "", null);
        String want = "Basic " + Base64.getEncoder()
            .encodeToString("ci:s3cret".getBytes(StandardCharsets.UTF_8));
        assertEquals(want, lastAuth.get());
    }

    @Test
    void publishBearerWinsOverBasic() throws Exception {
        BrokerClient c = BrokerClient.builder()
            .baseUrl(baseUrl())
            .token("tok-x")
            .basicAuth("u", "p")
            .build();
        c.publish(SAMPLE_PACT.getBytes(StandardCharsets.UTF_8), "1.0", "", null);
        assertTrue(lastAuth.get().startsWith("Bearer "));
    }

    @Test
    void publishWithBranchAndTags() throws Exception {
        BrokerClient c = BrokerClient.builder().baseUrl(baseUrl()).build();
        c.publish(SAMPLE_PACT.getBytes(StandardCharsets.UTF_8), "2.0", "main",
            List.of("prod", "stable", ""));
        assertEquals("main", lastBranch.get());
        assertEquals(2, tagPaths.size());
        assertTrue(tagPaths.stream().anyMatch(p -> p.endsWith("/tags/prod")));
        assertTrue(tagPaths.stream().anyMatch(p -> p.endsWith("/tags/stable")));
    }

    @Test
    void publish4xxSurfacesBody() {
        respStatus = 400;
        respBody = "{\"errors\":[\"bad pact\"]}".getBytes(StandardCharsets.UTF_8);
        BrokerClient c = BrokerClient.builder().baseUrl(baseUrl()).build();
        BrokerException ex = assertThrows(BrokerException.class, () ->
            c.publish(SAMPLE_PACT.getBytes(StandardCharsets.UTF_8), "1.0", "", null));
        assertEquals(400, ex.status());
        assertTrue(ex.body().contains("bad pact"));
    }

    @Test
    void publishRejectsMalformedPact() {
        BrokerClient c = BrokerClient.builder().baseUrl("http://x").build();
        assertThrows(IllegalArgumentException.class,
            () -> c.publish("not-json".getBytes(), "1.0", "", null));
        assertThrows(IllegalArgumentException.class,
            () -> c.publish("{}".getBytes(), "1.0", "", null));
    }

    @Test
    void publishRequiresConsumerVersion() {
        BrokerClient c = BrokerClient.builder().baseUrl("http://x").build();
        assertThrows(IllegalArgumentException.class,
            () -> c.publish(SAMPLE_PACT.getBytes(), "   ", "", null));
    }

    @Test
    void fetch404RaisesSentinel() {
        respStatus = 404;
        BrokerClient c = BrokerClient.builder().baseUrl(baseUrl()).build();
        assertThrows(PactNotFoundException.class, () -> c.fetch("X", "Y", "1.0"));
    }

    @Test
    void fetchLatestUsesLatestSegment() throws Exception {
        respBody = SAMPLE_PACT.getBytes(StandardCharsets.UTF_8);
        BrokerClient c = BrokerClient.builder().baseUrl(baseUrl()).build();
        byte[] body = c.fetchLatest("X", "Y");
        assertTrue(lastPath.get().endsWith("/version/latest"));
        assertArrayEquals(SAMPLE_PACT.getBytes(StandardCharsets.UTF_8), body);
    }

    @Test
    void canIDeployDeployable() throws Exception {
        respBody = "{\"summary\":{\"deployable\":true,\"reason\":\"all verified\"}}"
            .getBytes(StandardCharsets.UTF_8);
        BrokerClient c = BrokerClient.builder().baseUrl(baseUrl()).build();
        CanIDeployResult res = c.canIDeploy("OrderClient", "1.0", "prod");
        assertTrue(res.deployable());
        assertEquals("all verified", res.reason());
        assertTrue(lastPath.get().contains("pacticipant=OrderClient"));
        assertTrue(lastPath.get().contains("environment=prod"));
    }

    @Test
    void canIDeployNotDeployable() throws Exception {
        respBody = "{\"summary\":{\"deployable\":false,\"reason\":\"unverified\"}}"
            .getBytes(StandardCharsets.UTF_8);
        BrokerClient c = BrokerClient.builder()
            .baseUrl(baseUrl())
            .timeout(Duration.ofSeconds(5))
            .build();
        CanIDeployResult res = c.canIDeploy("X", "1.0", "");
        assertFalse(res.deployable());
        assertEquals("unverified", res.reason());
    }

    @Test
    void canIDeployRequiresArgs() {
        BrokerClient c = BrokerClient.builder().baseUrl("http://x").build();
        assertThrows(IllegalArgumentException.class, () -> c.canIDeploy("", "1.0", ""));
        assertThrows(IllegalArgumentException.class, () -> c.canIDeploy("X", "", ""));
    }
}
