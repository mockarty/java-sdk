// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.LLMPrice;
import ru.mockarty.model.LLMBudget;
import ru.mockarty.model.ResourcePrice;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EconomicsApiTest {
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/admin/llm-prices", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                if (body.contains("\"eventKind\"")) {
                    reply(exchange, resourcePriceJSON());
                } else {
                    reply(exchange, "{\"id\":\"p1\",\"provider\":\"openai\",\"model\":\"gpt\",\"currency\":\"USD\",\"effectiveFrom\":\"2026-01-01T00:00:00Z\"}");
                }
            } else if (exchange.getRequestURI().getRawQuery() != null &&
                    exchange.getRequestURI().getRawQuery().contains("eventKind=")) {
                reply(exchange, "{\"resourcePrices\":[" + resourcePriceJSON() + "]}");
            } else {
                reply(exchange, "{\"prices\":[]}");
            }
        });
        server.createContext("/api/v1/admin/llm-usage", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/statement.csv")) {
                reply(exchange, "event_id,event_kind\ne1,llm_tokens\n");
            } else if (path.endsWith("/e1/refund")) {
                reply(exchange, "{\"id\":\"r1\",\"createdAt\":\"2026-08-19T00:00:00Z\",\"originalEventId\":\"e1\",\"refundEventId\":\"e2\",\"reason\":\"invalid response\"}");
            } else {
                reply(exchange, "{\"totals\":{\"calls\":2,\"totalTokens\":10},\"rows\":[],\"costs\":[],\"resourceTotals\":[{\"eventKind\":\"runner_seconds\",\"unit\":\"seconds\",\"events\":1,\"quantity\":12}],\"unpricedCalls\":1,\"unpricedEvents\":1}");
            }
        });
        server.createContext("/api/v1/admin/llm-budgets", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) reply(exchange, "{\"budgets\":[]}");
            else reply(exchange, "{\"id\":\"b1\",\"namespace\":\"team-a\",\"scopeType\":\"workspace\",\"currency\":\"USD\",\"periodStart\":\"2026-01-01T00:00:00Z\",\"periodEnd\":\"2026-02-01T00:00:00Z\"}");
        });
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("k").timeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    @Test void priceBookAndUsage() throws Exception {
        LLMPrice price = new LLMPrice().provider("openai").model("gpt").currency("USD")
                .effectiveFrom("2026-01-01T00:00:00Z");
        assertEquals("p1", client.economics().appendPrice(price).getId());
        assertEquals(0, client.economics().listPrices("openai", null, 20).getPrices().size());
        assertEquals(1, client.economics().getUsage("module", 30).getUnpricedCalls());
        assertEquals(1, client.economics().getUsage("module", 30).getUnpricedEvents());
        assertEquals(12, client.economics().getUsage("module", 30).getResourceTotals().get(0).getQuantity());
    }

    @Test void resourcePriceBook() throws Exception {
        ResourcePrice price = new ResourcePrice().eventKind("tool_call").provider("mockarty-agent")
                .resource("web_search").unit("calls").currency("USD")
                .providerMicrosPerUnit(200).customerMicrosPerUnit(300)
                .effectiveFrom("2026-08-20T00:00:00Z");
        assertEquals("rp1", client.economics().appendResourcePrice(price).getId());
        assertEquals(1, client.economics().listResourcePrices(
                "tool_call", "mockarty-agent", "web_search", "calls", 20).getResourcePrices().size());
    }

    @Test void validatesRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> client.economics().appendPrice(new LLMPrice()));
        assertThrows(IllegalArgumentException.class, () -> client.economics().appendResourcePrice(
                new ResourcePrice().eventKind("tool_call").provider("mockarty").resource("tool")
                        .unit("seconds").currency("USD").effectiveFrom("2026-08-20T00:00:00Z")));
        assertThrows(IllegalArgumentException.class, () -> client.economics().listResourcePrices(
                "runner_seconds", null, null, "calls", 20));
    }

    @Test void budgets() throws Exception {
        LLMBudget budget = new LLMBudget().namespace("team-a").scopeType("workspace").currency("USD")
                .periodStart("2026-01-01T00:00:00Z").periodEnd("2026-02-01T00:00:00Z");
        assertEquals("b1", client.economics().createBudget(budget).getId());
        assertEquals(0, client.economics().listBudgets("team-a", true, 100).getBudgets().size());
    }

    @Test void statementAndRefund() throws Exception {
        assertEquals(true, new String(client.economics().downloadUsageStatement(null, null, "team-a", null, 100), StandardCharsets.UTF_8).contains("llm_tokens"));
        assertEquals("r1", client.economics().refundUsage("e1", "invalid response").getId());
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private static String resourcePriceJSON() {
        return "{\"id\":\"rp1\",\"eventKind\":\"tool_call\",\"provider\":\"mockarty-agent\"," +
                "\"resource\":\"web_search\",\"unit\":\"calls\",\"currency\":\"USD\"," +
                "\"providerMicrosPerUnit\":200,\"customerMicrosPerUnit\":300," +
                "\"effectiveFrom\":\"2026-08-20T00:00:00Z\"}";
    }
}
