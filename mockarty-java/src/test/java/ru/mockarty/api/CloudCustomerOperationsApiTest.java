// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudCustomerOperationsApiTest {
    private final List<String> requests = new ArrayList<>();
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud", this::handle);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("cloud-token").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    @Test void canonicalCustomerAndOperatorRoutes() {
        client.cloudCustomer().listLoyaltyRedemptions("space/1", "next", 25);
        client.cloudCustomer().redeemLoyalty("space/1", "WELCOME", "RU", "redeem-1");
        client.cloudCustomer().listSupportCases("space/1", "open", "cursor", 20);
        client.cloudCustomer().openSupportCase("space/1", "Help", "billing", "normal", "Please help", "case-1");
        client.cloudCustomer().getSupportCase("space/1", "case/1");
        client.cloudCustomer().replySupportCase("space/1", "case/1", "Reply", "reply-1");
        client.cloudCustomer().getRiskAppeal("risk/1");
        client.cloudCustomer().submitRiskAppeal("risk/1", "This decision needs review", "appeal-1");
        client.cloudOperations().listSupportCases("open", "op-next", 50);
        client.cloudOperations().getSupportCase("case/1");
        client.cloudOperations().replySupportCase("case/1", "Operator reply", "customer", "op-reply-1");
        client.cloudOperations().assignSupportCase("case/1", "user/1", 7);
        client.cloudOperations().transitionSupportCase("case/1", "resolved", 8);
        client.cloudOperations().productAnalytics(30);
        client.cloudSpaces().rename("space/1", "Renamed", "\"space-r7\"", "rename-1");

        assertEquals(List.of(
                "GET /api/v1/cloud/spaces/space%2F1/loyalty/redemptions?cursor=next&limit=25",
                "POST /api/v1/cloud/spaces/space%2F1/loyalty/redemptions",
                "GET /api/v1/cloud/spaces/space%2F1/support/cases?cursor=cursor&limit=20&status=open",
                "POST /api/v1/cloud/spaces/space%2F1/support/cases",
                "GET /api/v1/cloud/spaces/space%2F1/support/cases/case%2F1",
                "POST /api/v1/cloud/spaces/space%2F1/support/cases/case%2F1/messages",
                "GET /api/v1/cloud/risk/cases/risk%2F1/appeal",
                "POST /api/v1/cloud/risk/cases/risk%2F1/appeal",
                "GET /api/v1/cloud/operator/support/cases?cursor=op-next&limit=50&status=open",
                "GET /api/v1/cloud/operator/support/cases/case%2F1",
                "POST /api/v1/cloud/operator/support/cases/case%2F1/messages",
                "POST /api/v1/cloud/operator/support/cases/case%2F1/assign",
                "POST /api/v1/cloud/operator/support/cases/case%2F1/transition",
                "GET /api/v1/cloud/operator/analytics/product?days=30",
                "PATCH /api/v1/cloud/spaces/space%2F1"), requests);
    }

    @Test void productAnalyticsRejectsUnsupportedWindow() {
        assertThrows(IllegalArgumentException.class, () -> client.cloudOperations().productAnalytics(0));
        assertThrows(IllegalArgumentException.class, () -> client.cloudOperations().productAnalytics(91));
        assertEquals(List.of(), requests);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
