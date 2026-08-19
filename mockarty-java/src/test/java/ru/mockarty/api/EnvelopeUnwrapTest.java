// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.FuzzingSchedule;
import ru.mockarty.model.MockFolder;
import ru.mockarty.model.Pact;
import ru.mockarty.model.RecorderSession;
import ru.mockarty.model.Tag;
import ru.mockarty.model.TemplateFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards that the list endpoints which the server returns as a WRAPPED object
 * (not a bare array) are unwrapped by the SDK — matching the Go/Python SDKs.
 * These three regressed to a Jackson "cannot deserialize ArrayList from
 * START_OBJECT" failure before the fix (found via live dogfood).
 */
class EnvelopeUnwrapTest {

    private HttpServer server;
    private MockartyClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("k")
                .namespace("sandbox")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    private void respond(String path, String json) {
        server.createContext(path, exchange -> {
            byte[] body = json.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    @Test
    void tagsListUnwrapsStringArray() {
        respond("/api/v1/tags", "{\"namespace\":\"sandbox\",\"tags\":[\"users\",\"orders\"]}");
        List<Tag> tags = client.tags().list();
        assertEquals(2, tags.size());
        assertEquals("users", tags.get(0).getName());
        assertEquals("orders", tags.get(1).getName());
    }

    @Test
    void templatesListUnwrapsStringArray() {
        respond("/api/v1/templates", "{\"namespace\":\"sandbox\",\"total\":1,\"templates\":[\"welcome.txt\"]}");
        List<TemplateFile> files = client.templates().list();
        assertEquals(1, files.size());
        assertEquals("welcome.txt", files.get(0).getName());
        assertEquals("sandbox", files.get(0).getNamespace());
    }

    @Test
    void recorderSessionsUnwrapsObjectArray() {
        respond("/api/v1/recorder/sessions",
                "{\"sessions\":[{\"id\":\"s1\",\"status\":\"stopped\"},{\"id\":\"s2\",\"status\":\"recording\"}]}");
        List<RecorderSession> sessions = client.recorder().listSessions();
        assertEquals(2, sessions.size());
        assertEquals("s1", sessions.get(0).getId());
        assertEquals("s2", sessions.get(1).getId());
    }

    @Test
    void foldersListUnwrapsObjectArray() {
        respond("/api/v1/mock-folders",
                "{\"folders\":[{\"id\":\"f1\",\"name\":\"demo\",\"namespace\":\"sandbox\"}]}");
        List<MockFolder> folders = client.folders().list();
        assertEquals(1, folders.size());
        assertEquals("f1", folders.get(0).getId());
    }

    @Test
    void fuzzingSchedulesUnwrapsObjectArray() {
        respond("/api/v1/fuzzing/schedules", "{\"schedules\":[{\"id\":\"sch1\"}]}");
        List<FuzzingSchedule> schedules = client.fuzzing().listSchedules();
        assertEquals(1, schedules.size());
        assertEquals("sch1", schedules.get(0).getId());
    }

    @Test
    void pactListDeserializesPartyObjects() {
        respond("/api/v1/contract/pacts",
                "[{\"id\":\"p1\",\"consumer\":{\"name\":\"WebApp\"},\"provider\":{\"name\":\"OrderAPI\"}}]");
        List<Pact> pacts = client.contracts().listPacts();
        assertEquals(1, pacts.size());
        assertEquals("WebApp", pacts.get(0).getConsumer().getName());
        assertEquals("OrderAPI", pacts.get(0).getProvider().getName());
    }
}
