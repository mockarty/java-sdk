// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.MockVersion;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contract tests for the mock version-history endpoints. They return revision
 * ROWS, not mocks: the mock body of a revision hangs off the row's
 * {@code mock} key, and the list comes wrapped in
 * {@code {mock_id, versions, count}}. Decoding the envelope as a bare
 * {@code List<Mock>} yielded an empty list for every mock that had a history.
 */
class MockApiVersionsTest {

    private static final String ROW_V2 = "{\"id\":\"ver-2\",\"mock_id\":\"versioned-mock\","
            + "\"version\":2,\"created_at\":1700000200,\"lifecycle_state\":\"active\","
            + "\"tags\":[\"v2\"],"
            + "\"mock\":{\"id\":\"versioned-mock\",\"namespace\":\"sandbox\",\"tags\":[\"v2\"]}}";
    private static final String ROW_V1 = "{\"id\":\"ver-1\",\"mock_id\":\"versioned-mock\","
            + "\"version\":1,\"created_at\":1700000100,\"lifecycle_state\":\"active\","
            + "\"tags\":[\"v1\"],"
            + "\"mock\":{\"id\":\"versioned-mock\",\"namespace\":\"sandbox\",\"tags\":[\"v1\"]}}";

    private HttpServer server;
    private MockartyClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
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

    private void serve(String path, String json) {
        server.createContext(path, exchange -> {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    @Test
    @DisplayName("listVersions unwraps the envelope into revision rows with their mock body")
    void listVersionsUnwrapsEnvelope() throws Exception {
        serve("/api/v1/mocks/versioned-mock/versions",
                "{\"mock_id\":\"versioned-mock\",\"count\":2,\"versions\":["
                        + ROW_V2 + "," + ROW_V1 + "]}");

        List<MockVersion> versions = client.mocks().listVersions("versioned-mock");

        assertEquals(2, versions.size(), "the {versions: [...]} envelope must be unwrapped");
        assertEquals(2, versions.get(0).getVersion());
        assertEquals(1, versions.get(1).getVersion());
        assertEquals("ver-2", versions.get(0).getId());
        assertEquals("versioned-mock", versions.get(0).getMockId());
        assertEquals(1700000200L, versions.get(0).getCreatedAt());
        // The revision's mock body must survive the decode.
        assertNotNull(versions.get(0).getMock());
        assertEquals("sandbox", versions.get(0).getMock().getNamespace());
        assertEquals(List.of("v1"), versions.get(1).getTags());
    }

    @Test
    @DisplayName("getVersion unwraps {version, previous_version}")
    void getVersionUnwrapsEnvelope() throws Exception {
        serve("/api/v1/mocks/versioned-mock/versions/2",
                "{\"version\":" + ROW_V2 + ",\"previous_version\":" + ROW_V1 + "}");

        MockVersion current = client.mocks().getVersion("versioned-mock", "2");
        assertEquals(2, current.getVersion());
        assertNotNull(current.getMock());
        assertEquals("sandbox", current.getMock().getNamespace());

        MockVersion[] pair = client.mocks().getVersionWithPrevious("versioned-mock", "2");
        assertEquals(2, pair[0].getVersion());
        assertNotNull(pair[1]);
        assertEquals(1, pair[1].getVersion());
    }

    @Test
    @DisplayName("the first revision has no previous")
    void firstRevisionHasNoPrevious() throws Exception {
        serve("/api/v1/mocks/versioned-mock/versions/1",
                "{\"version\":" + ROW_V1 + ",\"previous_version\":null}");

        MockVersion[] pair = client.mocks().getVersionWithPrevious("versioned-mock", "1");
        assertEquals(1, pair[0].getVersion());
        assertNull(pair[1]);
    }

    @Test
    @DisplayName("a missing revision is an error, not an empty row")
    void missingRevisionThrows() {
        serve("/api/v1/mocks/versioned-mock/versions/9",
                "{\"version\":null,\"previous_version\":null}");
        // An empty MockVersion would read as "revision 0 exists".
        assertThrows(MockartyException.class,
                () -> client.mocks().getVersion("versioned-mock", "9"));
    }
}
