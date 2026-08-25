// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.ExperienceRecordRequest;
import ru.mockarty.model.ExperienceReviewRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExperienceApiTest {
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("k").timeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    @Test void searchAndRecord() throws Exception {
        server.createContext("/api/v1/autotester/context/knowledge/search", exchange -> reply(exchange, "{\"results\":[{\"id\":\"e1\",\"kind\":\"pitfall\",\"text\":\"retry\",\"source\":\"run\",\"provenance\":\"external\"}],\"total\":1,\"available\":true}"));
		server.createContext("/api/v1/autotester/context/knowledge", exchange -> reply(exchange, "{\"id\":\"e1\",\"kind\":\"pitfall\",\"provenance\":\"external\",\"state\":\"candidate\",\"reviewRequired\":true}"));
        assertEquals("e1", client.experience().search("retry", List.of("pitfall"), null, 5).getResults().get(0).getId());
		var recorded = client.experience().record(new ExperienceRecordRequest().text("retry").source("run").kind("pitfall"));
		assertEquals("external", recorded.getProvenance());
		assertEquals("candidate", recorded.getState());
		assertEquals(true, recorded.isReviewRequired());
    }

    @Test void validatesRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> client.experience().search("  "));
        assertThrows(IllegalArgumentException.class, () -> client.experience().record(new ExperienceRecordRequest().text("x")));
    }

    @Test void reviewAutomation() throws Exception {
		var listURI = new AtomicReference<String>();
        server.createContext("/api/v1/autotester/context/knowledge/review", exchange -> {
            if (exchange.getRequestURI().getPath().endsWith("/k-1")) {
                if (exchange.getRequestMethod().equals("POST")) {
                    reply(exchange, "{\"item\":{\"id\":\"k-1\",\"state\":\"deleted\",\"version\":2}}");
                } else {
                    reply(exchange, "{\"item\":{\"id\":\"k-1\",\"state\":\"candidate\",\"version\":1,\"metadata\":{\"instruction\":\"untrusted\"},\"contentSha256\":\"abc123\"},\"relations\":[],\"history\":[]}");
                }
                return;
            }
			listURI.set(exchange.getRequestURI().toString());
            reply(exchange, "{\"items\":[{\"id\":\"k-1\",\"state\":\"candidate\",\"version\":1}],\"nextCursor\":\"next\"}");
        });
        assertEquals("next", client.experience().listReview(20, null).getNextCursor());
		assertEquals("next", client.experience().listReview("published", 20, "next/page").getNextCursor());
		assertEquals("/api/v1/autotester/context/knowledge/review?state=published&limit=20&cursor=next%2Fpage", listURI.get());
		var detail = client.experience().getReview("k-1").getItem();
		assertEquals(1, detail.getVersion());
		assertEquals("abc123", detail.getContentSha256());
		assertEquals("untrusted", detail.getMetadata().get("instruction"));
        var request = new ExperienceReviewRequest().decision("reject").expectedVersion(1)
				.reason("unsupported").idempotencyKey("review-1").supersedesId("   ");
		assertDoesNotThrow(() -> client.experience().review("k-1", request));
        assertEquals("deleted", client.experience().review("k-1", request).getItem().getState());
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }
}
