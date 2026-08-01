// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.AllureUploadEmptyException;
import ru.mockarty.exception.AllureUploadPartialException;
import ru.mockarty.model.ExternalRunResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the honesty of {@link ExternalRunsApi#uploadAllureDir}: a CI upload
 * that loses results must never look like a clean one. Covers the three ways
 * an upload can report nothing while appearing to succeed — a missing
 * directory, an empty directory, and a directory where some files fail.
 */
class ExternalRunsApiUploadAllureDirTest {

    @TempDir
    Path results;

    private HttpServer server;
    private MockartyClient client;
    private final AtomicInteger posts = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/namespaces/qa/tcm/external-runs", exchange -> {
            posts.incrementAndGet();
            byte[] body = "{\"runId\":\"r-1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("mk_test")
                .namespace("qa")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    private void writeResult(String fileName, String json) throws IOException {
        Files.writeString(results.resolve(fileName), json, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("every *-result.json is reported")
    void uploadsEveryResult() throws Exception {
        writeResult("a-result.json", "{\"uuid\":\"a\",\"name\":\"a\",\"status\":\"passed\"}");
        writeResult("b-result.json", "{\"uuid\":\"b\",\"name\":\"b\",\"status\":\"failed\"}");

        List<ExternalRunResponse> out = client.externalRuns().uploadAllureDir("qa", results);

        assertEquals(2, out.size());
        assertEquals(2, posts.get());
    }

    @Test
    @DisplayName("a malformed result surfaces as AllureUploadPartialException, not a silent drop")
    void partialUploadIsReported() throws Exception {
        writeResult("a-result.json", "{\"uuid\":\"a\",\"name\":\"a\",\"status\":\"passed\"}");
        writeResult("b-result.json", "{ this is not json");

        AllureUploadPartialException ex = assertThrows(
                AllureUploadPartialException.class,
                () -> client.externalRuns().uploadAllureDir("qa", results));

        // The good file still landed, and the caller can recover it…
        assertEquals(1, ex.getUploaded());
        assertEquals(1, ex.getResults().size());
        assertEquals(1, posts.get());
        // …while learning exactly which result never reached Mockarty.
        assertEquals(1, ex.getSkipped().size());
        assertTrue(ex.getSkipped().get(0).startsWith("b-result.json:"),
                "skip entry must name the offending file: " + ex.getSkipped());
    }

    @Test
    @DisplayName("an empty results directory is an error, not an empty success")
    void emptyDirectoryIsAnError() {
        assertThrows(AllureUploadEmptyException.class,
                () -> client.externalRuns().uploadAllureDir("qa", results));
        assertEquals(0, posts.get());
    }

    @Test
    @DisplayName("a missing results directory is an error, not an empty success")
    void missingDirectoryIsAnError() {
        assertThrows(FileNotFoundException.class,
                () -> client.externalRuns().uploadAllureDir("qa", results.resolve("nope")));
    }
}
