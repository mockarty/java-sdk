// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Socket.IO facet. Mirrors {@code sdk/go-sdk/tester/socketio.go} and the
 * Python port. Distinct from a raw WebSocket — it speaks the
 * Engine.IO/Socket.IO v4 framing (handshake, namespace connect, named
 * events) over the JDK {@link WebSocket} API (no external dependency).
 *
 * <p>Transport: WebSocket only. Out of scope: binary attachments, ack
 * callbacks, the polling transport.</p>
 */
public final class SocketIOFacet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Tester t;
    private final String url;

    SocketIOFacet(Tester t, String url) {
        this.t = t;
        this.url = url;
    }

    public SocketIOStep connect() {
        t.flushPending();
        SocketIOStep s = new SocketIOStep(t, Interpolate.apply(url, t.snapshotVars()));
        t.setPending(s);
        return s;
    }

    /** One inbound Socket.IO event. */
    public static final class Event {
        public String name = "";
        public String namespace = "/";
        public List<JsonNode> args = new ArrayList<>();
    }

    public static final class SocketIOStep implements Committable {
        private final Tester t;
        private final String url;
        private String namespace = "/";
        private Duration window = Duration.ofSeconds(3);
        private Duration connWait = Duration.ofSeconds(3);
        private final List<String[]> headers = new ArrayList<>();
        private final List<Object[]> outbound = new ArrayList<>();
        private final List<Event> received = new ArrayList<>();

        private Throwable connectErr;
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private final List<String> failures = new ArrayList<>();

        SocketIOStep(Tester t, String url) {
            this.t = t;
            this.url = url;
        }

        // ── builders ──────────────────────────────────────────────────

        public SocketIOStep namespace(String ns) {
            if (guard("namespace")) { return this; }
            this.namespace = Interpolate.apply(ns, t.snapshotVars());
            return this;
        }
        public SocketIOStep header(String k, String v) {
            if (guard("header")) { return this; }
            headers.add(new String[]{k, Interpolate.apply(v, t.snapshotVars())});
            return this;
        }
        public SocketIOStep connectTimeout(Duration d) {
            if (guard("connectTimeout")) { return this; }
            if (d != null && !d.isZero() && !d.isNegative()) { this.connWait = d; }
            return this;
        }
        public SocketIOStep emit(String event, Object... args) {
            if (guard("emit")) { return this; }
            Object[] row = new Object[args.length + 1];
            row[0] = Interpolate.apply(event, t.snapshotVars());
            for (int i = 0; i < args.length; i++) {
                row[i + 1] = (args[i] instanceof String)
                        ? Interpolate.apply((String) args[i], t.snapshotVars()) : args[i];
            }
            outbound.add(row);
            return this;
        }
        public SocketIOStep collect(Duration d) {
            if (!sent && d != null && !d.isZero() && !d.isNegative()) { this.window = d; }
            ensureSent();
            return this;
        }

        // ── assertions ────────────────────────────────────────────────

        public SocketIOStep expectConnected() {
            ensureSent();
            if (connectErr != null) { fail("expectConnected: " + connectErr.getMessage()); }
            return this;
        }
        public SocketIOStep expectEvent(String name) {
            if (!ensureSent()) { return this; }
            if (find(name) < 0) { fail("expectEvent: \"" + name + "\" not received"); }
            return this;
        }
        public SocketIOStep expectEventCount(String name, int n) {
            if (!ensureSent()) { return this; }
            int count = 0;
            for (Event e : received) { if (e.name.equals(name)) { count++; } }
            if (count != n) { fail("expectEventCount[" + name + "]: want " + n + ", got " + count); }
            return this;
        }
        public SocketIOStep expectReceivedCount(int n) {
            if (!ensureSent()) { return this; }
            if (received.size() != n) { fail("expectReceivedCount: want " + n + ", got " + received.size()); }
            return this;
        }
        public SocketIOStep expectEventArgContains(String name, String sub) {
            if (!ensureSent()) { return this; }
            int idx = find(name);
            if (idx < 0) { return fail("expectEventArgContains: \"" + name + "\" not received"); }
            if (received.get(idx).args.isEmpty()) {
                return fail("expectEventArgContains[" + name + "]: event has no args");
            }
            if (!received.get(idx).args.get(0).toString().contains(sub)) {
                fail("expectEventArgContains[" + name + "]: \"" + sub + "\" not found");
            }
            return this;
        }
        public SocketIOStep expectEventJsonPath(String name, String path, Object want) {
            if (!ensureSent()) { return this; }
            Object got;
            try {
                got = evalArg(name, path);
            } catch (Exception e) {
                return fail("expectEventJsonPath[" + name + "] " + path + ": " + e.getMessage());
            }
            if (!JsonPath.equalsLoose(got, want)) {
                fail("expectEventJsonPath[" + name + "] " + path + ": want " + want + ", got " + got);
            }
            return this;
        }
        public SocketIOStep extract(String name, String path, String varName) {
            if (!ensureSent()) { return this; }
            Object got;
            try {
                got = evalArg(name, path);
            } catch (Exception e) {
                return fail("extract[" + name + "] " + path + ": " + e.getMessage());
            }
            t.setVar(varName, TesterScalar.stringify(got));
            return this;
        }

        public List<Event> received() {
            ensureSent();
            return new ArrayList<>(received);
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        // ── internals ─────────────────────────────────────────────────

        private SocketIOStep fail(String msg) { failures.add(msg); return this; }

        private boolean guard(String method) {
            if (sent) { fail(method + "() called after connect"); return true; }
            return false;
        }

        private int find(String name) {
            for (int i = 0; i < received.size(); i++) {
                if (received.get(i).name.equals(name)) { return i; }
            }
            return -1;
        }

        private Object evalArg(String name, String path) throws Exception {
            int idx = find(name);
            if (idx < 0) { throw new IllegalStateException("event \"" + name + "\" not received"); }
            if (received.get(idx).args.isEmpty()) {
                throw new IllegalStateException("event \"" + name + "\" has no args");
            }
            Object doc = MAPPER.treeToValue(received.get(idx).args.get(0), Object.class);
            return JsonPath.resolve(doc, path);
        }

        private boolean ensureSent() {
            if (sent) { return connectErr == null && !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            startedAt = Instant.now();
            SocketIOConn conn = null;
            try {
                conn = SocketIOConn.dial(url, headers, connWait);
                conn.connectNamespace(namespace, connWait);
                for (Object[] row : outbound) {
                    String event = (String) row[0];
                    Object[] args = new Object[row.length - 1];
                    System.arraycopy(row, 1, args, 0, args.length);
                    conn.emit(namespace, event, args);
                }
                received.addAll(conn.collect(window));
            } catch (Exception e) {
                connectErr = e;
                fail("socket.io: " + e.getMessage());
                abortChain = true;
                endedAt = Instant.now();
                if (conn != null) { conn.close(); }
                return false;
            }
            conn.close();
            endedAt = Instant.now();
            return true;
        }

        @Override
        public void commit() {
            if (committed) { return; }
            committed = true;
            if (!sent) { ensureSent(); }
            StepRecord rec = new StepRecord();
            rec.protocol = "socketio";
            rec.method = "event";
            rec.name = "socket.io " + url;
            rec.url = url;
            rec.statusOrCode = received.size();
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }

    /**
     * Minimal Engine.IO v4 / Socket.IO v4 connection over the JDK
     * WebSocket API. Bridges the async WebSocket to a sync queue.
     */
    static final class SocketIOConn implements AutoCloseable {
        private static final char EIO_OPEN = '0';
        private static final char EIO_CLOSE = '1';
        private static final char EIO_PING = '2';
        private static final char EIO_PONG = '3';
        private static final char EIO_MESSAGE = '4';
        private static final char SIO_CONNECT = '0';
        private static final char SIO_EVENT = '2';
        private static final char SIO_CONNECT_ERROR = '4';

        private final WebSocket socket;

        private SocketIOConn(WebSocket socket) {
            this.socket = socket;
        }

        static SocketIOConn dial(String url, List<String[]> headers, Duration timeout) throws Exception {
            String wsUrl = normalizeUrl(url);
            HttpClient http = HttpClient.newBuilder().connectTimeout(timeout).build();
            final Queue<String> inbox = new LinkedList<>();
            final Object lock = new Object();
            final boolean[] closedFlag = {false};
            WebSocket.Listener listener = new WebSocket.Listener() {
                private final StringBuilder buf = new StringBuilder();
                @Override
                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    buf.append(data);
                    if (last) {
                        synchronized (lock) { inbox.add(buf.toString()); lock.notifyAll(); }
                        buf.setLength(0);
                    }
                    ws.request(1);
                    return null;
                }
                @Override
                public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                    synchronized (lock) { closedFlag[0] = true; lock.notifyAll(); }
                    return null;
                }
                @Override
                public void onError(WebSocket ws, Throwable error) {
                    synchronized (lock) { closedFlag[0] = true; lock.notifyAll(); }
                }
            };
            WebSocket.Builder wsb = http.newWebSocketBuilder().connectTimeout(timeout);
            for (String[] h : headers) { wsb.header(h[0], h[1]); }
            WebSocket ws;
            try {
                ws = wsb.buildAsync(URI.create(wsUrl), listener)
                        .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new Exception("dial " + wsUrl + ": " + rootMessage(e));
            }
            // Bind the listener's shared queue/lock to the connection and
            // consume the Engine.IO open handshake before returning.
            return new SocketIOConn(ws).bind(inbox, lock, closedFlag);
        }

        // Bind the listener-side queue to this connection (set after dial).
        private Queue<String> boundInbox;
        private Object boundLock;
        private boolean[] boundClosed;

        private SocketIOConn bind(Queue<String> inbox, Object lock, boolean[] closed) {
            this.boundInbox = inbox;
            this.boundLock = lock;
            this.boundClosed = closed;
            // Read the Engine.IO open handshake.
            try {
                String open = readRaw(Duration.ofSeconds(10));
                if (open == null || open.isEmpty() || open.charAt(0) != EIO_OPEN) {
                    throw new IllegalStateException("expected open handshake, got: " + open);
                }
            } catch (Exception e) {
                close();
                throw new RuntimeException("handshake: " + e.getMessage(), e);
            }
            return this;
        }

        void connectNamespace(String namespace, Duration wait) throws Exception {
            String ns = (namespace == null || namespace.isEmpty()) ? "/" : namespace;
            String pkt = "" + EIO_MESSAGE + SIO_CONNECT + (ns.equals("/") ? "" : ns + ",");
            send(pkt);
            Instant deadline = Instant.now().plus(wait);
            while (Instant.now().isBefore(deadline)) {
                String[] frame = readSocketIO(Duration.between(Instant.now(), deadline));
                if (frame == null) { continue; }
                char typ = frame[0].charAt(0);
                String fns = frame[1];
                if (typ == SIO_CONNECT && nsMatch(fns, ns)) { return; }
                if (typ == SIO_CONNECT_ERROR) {
                    throw new IllegalStateException("connect error: " + frame[2]);
                }
            }
            throw new IllegalStateException("connect to \"" + ns + "\" timed out");
        }

        void emit(String namespace, String event, Object... args) throws Exception {
            String ns = (namespace == null || namespace.isEmpty()) ? "/" : namespace;
            ArrayNode arr = MAPPER.createArrayNode();
            arr.add(event);
            for (Object a : args) { arr.add(MAPPER.valueToTree(a)); }
            String pkt = "" + EIO_MESSAGE + SIO_EVENT
                    + (ns.equals("/") ? "" : ns + ",") + MAPPER.writeValueAsString(arr);
            send(pkt);
        }

        List<Event> collect(Duration window) {
            List<Event> out = new ArrayList<>();
            Instant deadline = Instant.now().plus(window);
            while (Instant.now().isBefore(deadline)) {
                String[] frame;
                try {
                    frame = readSocketIO(Duration.between(Instant.now(), deadline));
                } catch (Exception e) {
                    break;
                }
                if (frame == null) { continue; }
                if (frame[0].charAt(0) == SIO_EVENT) {
                    Event ev = parseEvent(frame[1], frame[2]);
                    if (ev != null) { out.add(ev); }
                }
            }
            return out;
        }

        private void send(String text) throws Exception {
            socket.sendText(text, true).get(10, TimeUnit.SECONDS);
        }

        /**
         * Reads one Engine.IO frame, answering pings, and returns
         * {@code [sioType, namespace, body]} for the next message frame,
         * or {@code null} on timeout / skipped frame.
         */
        private String[] readSocketIO(Duration timeout) throws Exception {
            String text = readRaw(timeout);
            if (text == null || text.isEmpty()) { return null; }
            char head = text.charAt(0);
            if (head == EIO_PING) {
                send("" + EIO_PONG + text.substring(1));
                return null;
            }
            if (head == EIO_PONG || head == EIO_OPEN) { return null; }
            if (head == EIO_CLOSE) { throw new IllegalStateException("server closed connection"); }
            if (head != EIO_MESSAGE) { return null; }
            String sio = text.substring(1);
            if (sio.isEmpty()) { return null; }
            char typ = sio.charAt(0);
            String rest = sio.substring(1);
            String ns = "/";
            if (rest.startsWith("/")) {
                int comma = rest.indexOf(',');
                if (comma >= 0) {
                    ns = rest.substring(0, comma);
                    rest = rest.substring(comma + 1);
                }
            }
            // Strip a leading ack id (digits) if present.
            int i = 0;
            while (i < rest.length() && Character.isDigit(rest.charAt(i))) { i++; }
            rest = rest.substring(i);
            return new String[]{String.valueOf(typ), ns, rest};
        }

        private String readRaw(Duration timeout) throws Exception {
            Instant deadline = Instant.now().plus(
                    timeout.isNegative() || timeout.isZero() ? Duration.ofMillis(10) : timeout);
            synchronized (boundLock) {
                while (true) {
                    String head = boundInbox.poll();
                    if (head != null) { return head; }
                    if (boundClosed[0]) { return null; }
                    long left = Duration.between(Instant.now(), deadline).toMillis();
                    if (left <= 0) { return null; }
                    boundLock.wait(left);
                }
            }
        }

        @Override
        public void close() {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "client closing").get(2, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // best-effort
            }
        }

        private static Event parseEvent(String ns, String body) {
            try {
                JsonNode node = MAPPER.readTree(body);
                if (!node.isArray() || node.isEmpty()) { return null; }
                JsonNode first = node.get(0);
                if (!first.isTextual()) { return null; }
                Event ev = new Event();
                ev.name = first.asText();
                ev.namespace = ns;
                for (int i = 1; i < node.size(); i++) { ev.args.add(node.get(i)); }
                return ev;
            } catch (Exception e) {
                return null;
            }
        }

        private static boolean nsMatch(String got, String want) {
            if (got == null || got.isEmpty()) { got = "/"; }
            if (want == null || want.isEmpty()) { want = "/"; }
            return got.equals(want);
        }

        static String normalizeUrl(String url) {
            String u = url;
            if (u.startsWith("http://")) { u = "ws://" + u.substring("http://".length()); }
            else if (u.startsWith("https://")) { u = "wss://" + u.substring("https://".length()); }
            String base = u;
            String query = "";
            int q = u.indexOf('?');
            if (q >= 0) { base = u.substring(0, q); query = u.substring(q + 1); }
            if (!base.contains("/socket.io")) {
                base = stripTrailingSlash(base) + "/socket.io/";
            }
            if (!query.contains("EIO=")) {
                query = (query.isEmpty() ? "" : query + "&") + "EIO=4&transport=websocket";
            }
            return base + "?" + query;
        }

        private static String stripTrailingSlash(String s) {
            return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
        }

        private static String rootMessage(Throwable t) {
            Throwable c = t;
            while (c.getCause() != null) { c = c.getCause(); }
            return c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
        }
    }
}
