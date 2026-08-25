// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsCapabilitiesApiTest {

    @Test
    void listCapabilitiesDecodesCanonicalDescriptor() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/capabilities", exchange -> {
            byte[] body = ("{\"capabilities\":[{\"contractVersion\":\"mockarty.capability/v1\"," +
                    "\"key\":\"mission.coder\",\"version\":\"1.0.0\",\"provider\":\"mockarty.missions\"," +
                    "\"kind\":\"mission-component\",\"title\":\"Coder\",\"description\":\"Codes.\"," +
                    "\"hosts\":[\"admin\"],\"policy\":{\"sideEffect\":\"external_write\"}," +
                    "\"provenance\":{\"sourceKind\":\"builtin\",\"sourceRef\":\"mockarty:coder\",\"publisher\":\"mockarty\"}," +
                    "\"availability\":{\"available\":true}}],\"count\":1,\"skipped\":0}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("k")
                .namespace("team-a")
                .build()) {
            CapabilityCatalog catalog = client.stats().listCapabilities();
            assertEquals(1, catalog.getCount());
            assertEquals("mockarty.capability/v1", catalog.getCapabilities().get(0).getContractVersion());
            assertEquals("external_write", catalog.getCapabilities().get(0).getPolicy().getSideEffect());
        } finally {
            server.stop(0);
        }
    }
}
