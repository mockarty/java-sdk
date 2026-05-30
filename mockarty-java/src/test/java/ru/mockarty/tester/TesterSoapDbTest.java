// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Java Tester SOAP + DB facet coverage. SOAP uses a tiny HttpServer
 *  to stand in for the real SOAP endpoint; DB uses an in-memory fake. */
public class TesterSoapDbTest {

    private HttpServer server;
    private String base;

    private static final String USER_RESPONSE = String.join("\n",
            "<?xml version=\"1.0\"?>",
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">",
            "  <soap:Body>",
            "    <GetUserResponse xmlns=\"urn:test\">",
            "      <user>",
            "        <id>42</id>",
            "        <name>Alice</name>",
            "      </user>",
            "    </GetUserResponse>",
            "  </soap:Body>",
            "</soap:Envelope>");

    private static final String FAULT_RESPONSE = String.join("\n",
            "<?xml version=\"1.0\"?>",
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">",
            "  <soap:Body>",
            "    <soap:Fault>",
            "      <faultcode>soap:Client</faultcode>",
            "      <faultstring>Invalid user id</faultstring>",
            "    </soap:Fault>",
            "  </soap:Body>",
            "</soap:Envelope>");

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    private void route(String body, int status, AtomicReference<String> captureBody, AtomicReference<String> captureAction) {
        server.createContext("/", (HttpExchange ex) -> {
            if (captureBody != null) {
                byte[] b = ex.getRequestBody().readAllBytes();
                captureBody.set(new String(b, StandardCharsets.UTF_8));
            }
            if (captureAction != null) {
                captureAction.set(ex.getRequestHeaders().getFirst("SOAPAction"));
            }
            ex.getResponseHeaders().add("Content-Type", "text/xml");
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, out.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(out); }
        });
    }

    // ── SOAP ──────────────────────────────────────────────────────────

    @Test
    void soapHappyPath() {
        AtomicReference<String> action = new AtomicReference<>();
        route(USER_RESPONSE, 200, null, action);
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.soap("/svc")
                .call("urn:test#GetUser", "<GetUser xmlns=\"urn:test\"><id>42</id></GetUser>")
                .expectStatus(200)
                .expectNoFault()
                .expectXPath("//*[local-name()='name']", "Alice")
                .expectXPathContains("//*[local-name()='name']", "Ali")
                .extract("//*[local-name()='name']", "user");
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
        assertEquals("Alice", t.vars().get("user"));
        assertEquals("urn:test#GetUser", action.get());
    }

    @Test
    void soapFaultDetected() {
        route(FAULT_RESPONSE, 500, null, null);
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.soap("/svc").call("op", "<X/>")
                .expectFault("Client")
                .expectXPath("//*[local-name()='faultstring']", "Invalid user id");
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
    }

    @Test
    void soapExpectNoFaultFailsOnFault() {
        route(FAULT_RESPONSE, 500, null, null);
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.soap("/svc").call("op", "<X/>").expectNoFault();
        t.finish();
        assertFalse(t.ok());
    }

    @Test
    void soapBodyInterpolation() {
        AtomicReference<String> body = new AtomicReference<>();
        route(USER_RESPONSE, 200, body, null);
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.setVar("id", "42");
        t.soap("/svc")
                .call("op", "<GetUser><id>{{id}}</id></GetUser>")
                .expectStatus(200);
        t.finish();
        assertTrue(body.get().contains("<id>42</id>"), () -> "body=" + body.get());
    }

    @Test
    void soapXPathMissingFails() {
        route(USER_RESPONSE, 200, null, null);
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.soap("/svc").call("op", "<X/>")
                .expectXPath("//*[local-name()='missing']", "x");
        t.finish();
        assertFalse(t.ok());
    }

    // ── DB ────────────────────────────────────────────────────────────

    static final class FakeDB implements DBFacet.SQLConn {
        final Map<String, List<Map<String, Object>>> queries = new HashMap<>();
        final Map<String, DBFacet.ExecResult> execs = new HashMap<>();
        final Map<String, Exception> errs = new HashMap<>();
        final List<Object[]> calls = new ArrayList<>();

        @Override
        public List<Map<String, Object>> query(String sql, Object... args) throws Exception {
            calls.add(new Object[]{"query", sql, args});
            if (errs.containsKey(sql)) { throw errs.get(sql); }
            return queries.getOrDefault(sql, new ArrayList<>());
        }

        @Override
        public DBFacet.ExecResult exec(String sql, Object... args) throws Exception {
            calls.add(new Object[]{"exec", sql, args});
            if (errs.containsKey(sql)) { throw errs.get(sql); }
            return execs.getOrDefault(sql, new DBFacet.ExecResult());
        }
    }

    @Test
    void dbQueryHappyPath() {
        FakeDB db = new FakeDB();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Alice");
        db.queries.put("SELECT id, name FROM users WHERE id = ?", List.of(row));
        Tester t = new Tester.Builder().build();
        t.db(db).query("SELECT id, name FROM users WHERE id = ?", 42)
                .expectOK()
                .expectRowCount(1)
                .expectColumn("name", "Alice")
                .expectField(0, "id", 42)
                .extract(0, "name", "user");
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
        assertEquals("Alice", t.vars().get("user"));
    }

    @Test
    void dbExecHappyPath() {
        FakeDB db = new FakeDB();
        db.execs.put("UPDATE users SET name = ? WHERE id = ?",
                new DBFacet.ExecResult(1, 0));
        Tester t = new Tester.Builder().build();
        t.db(db).exec("UPDATE users SET name = ? WHERE id = ?", "Bob", 42)
                .expectOK()
                .expectAffected(1);
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
    }

    @Test
    void dbExpectError() {
        FakeDB db = new FakeDB();
        db.errs.put("BAD", new RuntimeException("syntax error"));
        Tester t = new Tester.Builder().build();
        t.db(db).query("BAD").expectError();
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
    }

    @Test
    void dbExpectFieldRowOutOfRange() {
        FakeDB db = new FakeDB();
        Tester t = new Tester.Builder().build();
        t.db(db).query("SELECT * FROM nothing")
                .expectField(0, "id", 1)
                .extract(0, "id", "x");
        t.finish();
        assertFalse(t.ok());
    }

    @Test
    void dbArgInterpolation() {
        FakeDB db = new FakeDB();
        Map<String, Object> row = new HashMap<>();
        row.put("x", "alice");
        db.queries.put("SELECT ?", List.of(row));
        Tester t = new Tester.Builder().build();
        t.setVar("user", "alice");
        t.db(db).query("SELECT ?", "{{user}}").expectRowCount(1);
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
        Object[] firstCall = db.calls.get(0);
        Object[] args = (Object[]) firstCall[2];
        assertEquals("alice", args[0]);
    }

    @Test
    void dbMisuseExpectAffectedAfterQuery() {
        FakeDB db = new FakeDB();
        db.queries.put("X", new ArrayList<>());
        Tester t = new Tester.Builder().build();
        t.db(db).query("X").expectAffected(1);
        t.finish();
        assertFalse(t.ok());
    }
}
