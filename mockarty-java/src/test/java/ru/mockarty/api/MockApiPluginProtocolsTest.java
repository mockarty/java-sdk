// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.PluginProtocolCatalogue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockApiPluginProtocolsTest {
    private HttpServer server;
    private MockartyClient client;

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void listPluginProtocolsUsesDefaultNamespaceAndDecodesCatalogue() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/plugin-protocols", exchange -> {
            assertEquals("namespace=team+a", exchange.getRequestURI().getRawQuery());
            assertEquals("namespace=team a", URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8));
            byte[] body = ("{\"protocols\":[{\"key\":\"acme-line\",\"name\":\"ACME Line\"," +
                    "\"transport\":\"tcp-line\",\"magic\":\"ACME \",\"pluginId\":\"acme.codec\"," +
                    "\"mockProtocol\":\"socket\",\"serverName\":\"plugin:acme-line\"}],\"count\":1}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("mk_test")
                .namespace("team a")
                .timeout(Duration.ofSeconds(5))
                .build();

        PluginProtocolCatalogue catalogue = client.mocks().listPluginProtocols();

        assertEquals(1, catalogue.getCount());
        assertEquals("acme.codec", catalogue.getProtocols().get(0).getPluginId());
        assertEquals("plugin:acme-line", catalogue.getProtocols().get(0).getServerName());
    }
}
