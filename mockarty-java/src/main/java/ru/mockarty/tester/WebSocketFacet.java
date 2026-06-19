// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Raw WebSocket facet. Mirrors {@code sdk/go-sdk/tester/websocket.go} and the
 * Python port so a WS suite translates 1:1 across the three SDKs. Distinct from
 * {@link SocketIOFacet} — this speaks plain WebSocket frames (text + binary),
 * no Engine.IO/Socket.IO framing, over the JDK {@link WebSocket} API (no
 * external dependency).
 *
 * <p>Connect, flush the queued outbound frames, collect inbound frames for a
 * bounded {@link WSStep#listen(Duration) window} (the window elapsing or a clean
 * peer close is NOT a failure), then assert on the received messages.</p>
 */
public final class WebSocketFacet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Tester t;
    private final String url;

    WebSocketFacet(Tester t, String url) {
        this.t = t;
        this.url = url;
    }

    /** Start a WebSocket chain; the dial fires lazily on the first send/assert. */
    public WSStep connect() {
        t.flushPending();
        WSStep s = new WSStep(t, Interpolate.apply(url, t.snapshotVars()));
        t.setPending(s);
        return s;
    }

    /** One inbound WebSocket message. */
    public static final class Message {
        public boolean text;   // true → text frame, false → binary
        public byte[] data = new byte[0];

        /** The payload as a UTF-8 string (text frame, or binary decoded as UTF-8). */
        public String asText() { return new String(data, StandardCharsets.UTF_8); }
    }

    public static final class WSStep implements Committable {
        private final Tester t;
        private final String url;
        private Duration listen = Duration.ofSeconds(5);
        private final List<String[]> headers = new ArrayList<>();
        private final List<Object[]> outbound = new ArrayList<>(); // {Boolean isText, byte[]/String payload}
        private final List<Message> received = new ArrayList<>();

        private Throwable dialErr;
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private final List<String> failures = new ArrayList<>();

        WSStep(Tester t, String url) {
            this.t = t;
            this.url = url;
        }

        // ── builders ──────────────────────────────────────────────────

        public WSStep header(String k, String v) {
            if (guard("header")) { return this; }
            headers.add(new String[]{k, Interpolate.apply(v, t.snapshotVars())});
            return this;
        }
        public WSStep listen(Duration d) {
            if (guard("listen")) { return this; }
            if (d != null && !d.isZero() && !d.isNegative()) { this.listen = d; }
            return this;
        }
        public WSStep send(String text) {
            if (guard("send")) { return this; }
            outbound.add(new Object[]{Boolean.TRUE, Interpolate.apply(text, t.snapshotVars())});
            return this;
        }
        public WSStep sendJson(Object value) {
            if (guard("sendJson")) { return this; }
            try {
                outbound.add(new Object[]{Boolean.TRUE, MAPPER.writeValueAsString(value)});
            } catch (Exception e) {
                fail("sendJson: " + e.getMessage());
            }
            return this;
        }
        public WSStep sendBinary(byte[] payload) {
            if (guard("sendBinary")) { return this; }
            outbound.add(new Object[]{Boolean.FALSE, payload == null ? new byte[0] : payload});
            return this;
        }

        // ── assertions ────────────────────────────────────────────────

        public WSStep expectConnected() {
            ensureSent();
            if (dialErr != null) { fail("expectConnected: " + dialErr.getMessage()); }
            return this;
        }
        public WSStep expectReceivedCount(int n) {
            if (!ensureSent()) { return this; }
            if (received.size() != n) { fail("expectReceivedCount: want " + n + ", got " + received.size()); }
            return this;
        }
        public WSStep expectReceivedAtLeast(int n) {
            if (!ensureSent()) { return this; }
            if (received.size() < n) { fail("expectReceivedAtLeast: want >=" + n + ", got " + received.size()); }
            return this;
        }
        public WSStep expectMessageContains(int idx, String sub) {
            if (!ensureSent()) { return this; }
            Message m = at(idx);
            if (m == null) { fail("expectMessageContains[" + idx + "]: no such message (" + received.size() + " received)"); return this; }
            if (!m.asText().contains(sub)) { fail("expectMessageContains[" + idx + "]: \"" + sub + "\" not in \"" + m.asText() + "\""); }
            return this;
        }
        public WSStep expectJsonPath(int idx, String path, Object want) {
            if (!ensureSent()) { return this; }
            try {
                Object got = evalAt(idx, path);
                if (!JsonPath.equalsLoose(got, want)) {
                    fail("expectJsonPath[" + idx + "] " + path + ": want " + want + ", got " + got);
                }
            } catch (Exception e) {
                fail("expectJsonPath[" + idx + "] " + path + ": " + e.getMessage());
            }
            return this;
        }
        public WSStep extract(int idx, String path, String name) {
            if (!ensureSent()) { return this; }
            try {
                Object got = evalAt(idx, path);
                t.setVar(name, TesterScalar.stringify(got));
            } catch (Exception e) {
                fail("extract[" + idx + "] " + path + ": " + e.getMessage());
            }
            return this;
        }
        public List<Message> received() {
            ensureSent();
            return new ArrayList<>(received);
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        // ── internals ─────────────────────────────────────────────────

        private boolean guard(String method) {
            if (sent) { fail(method + "() called after connect"); return true; }
            return false;
        }
        private void fail(String msg) { failures.add(msg); }

        private Message at(int idx) {
            return (idx < 0 || idx >= received.size()) ? null : received.get(idx);
        }
        private Object evalAt(int idx, String path) throws Exception {
            Message m = at(idx);
            if (m == null) { throw new IllegalStateException("no message at index " + idx); }
            JsonNode doc = MAPPER.readTree(m.asText());
            return JsonPath.resolve(MAPPER.convertValue(doc, Object.class), path);
        }

        private String wsURL() {
            String u = url;
            if (u.startsWith("ws://") || u.startsWith("wss://")) { return u; }
            if (u.startsWith("http://")) { return "ws://" + u.substring("http://".length()); }
            if (u.startsWith("https://")) { return "wss://" + u.substring("https://".length()); }
            String base = t.baseUrl();
            if (base.startsWith("http://")) { base = "ws://" + base.substring("http://".length()); }
            else if (base.startsWith("https://")) { base = "wss://" + base.substring("https://".length()); }
            if (!u.startsWith("/")) { u = "/" + u; }
            return base + u;
        }

        private boolean ensureSent() {
            if (sent) { return dialErr == null && !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            startedAt = Instant.now();
            WSConn conn = null;
            try {
                conn = WSConn.dial(wsURL(), headers, listen);
                for (Object[] row : outbound) {
                    boolean isText = (Boolean) row[0];
                    if (isText) { conn.sendText((String) row[1]); }
                    else { conn.sendBinary((byte[]) row[1]); }
                }
                received.addAll(conn.collect(listen));
            } catch (Exception e) {
                dialErr = e;
                fail("ws: " + e.getMessage());
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
            rec.protocol = "websocket";
            rec.method = "message";
            rec.name = "ws " + url;
            rec.url = url;
            rec.statusOrCode = received.size();
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }

    /** Minimal raw-WebSocket connection over the JDK WebSocket API. */
    static final class WSConn implements AutoCloseable {
        private final WebSocket socket;
        private final Queue<Message> inbox;
        private final Object lock;
        private final boolean[] closedFlag;

        private WSConn(WebSocket socket, Queue<Message> inbox, Object lock, boolean[] closedFlag) {
            this.socket = socket;
            this.inbox = inbox;
            this.lock = lock;
            this.closedFlag = closedFlag;
        }

        static WSConn dial(String wsUrl, List<String[]> headers, Duration timeout) throws Exception {
            final Queue<Message> inbox = new LinkedList<>();
            final Object lock = new Object();
            final boolean[] closedFlag = {false};
            WebSocket.Listener listener = new WebSocket.Listener() {
                private final StringBuilder textBuf = new StringBuilder();
                private final java.io.ByteArrayOutputStream binBuf = new java.io.ByteArrayOutputStream();
                @Override
                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    textBuf.append(data);
                    if (last) {
                        Message m = new Message();
                        m.text = true;
                        m.data = textBuf.toString().getBytes(StandardCharsets.UTF_8);
                        synchronized (lock) { inbox.add(m); lock.notifyAll(); }
                        textBuf.setLength(0);
                    }
                    ws.request(1);
                    return null;
                }
                @Override
                public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                    byte[] chunk = new byte[data.remaining()];
                    data.get(chunk);
                    binBuf.write(chunk, 0, chunk.length);
                    if (last) {
                        Message m = new Message();
                        m.text = false;
                        m.data = binBuf.toByteArray();
                        synchronized (lock) { inbox.add(m); lock.notifyAll(); }
                        binBuf.reset();
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
            HttpClient http = HttpClient.newBuilder().connectTimeout(timeout).build();
            WebSocket.Builder wsb = http.newWebSocketBuilder().connectTimeout(timeout);
            for (String[] h : headers) { wsb.header(h[0], h[1]); }
            WebSocket ws;
            try {
                ws = wsb.buildAsync(URI.create(wsUrl), listener)
                        .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Throwable root = e;
                while (root.getCause() != null) { root = root.getCause(); }
                throw new Exception("dial " + wsUrl + ": " + root.getMessage());
            }
            return new WSConn(ws, inbox, lock, closedFlag);
        }

        void sendText(String s) {
            socket.sendText(s, true).toCompletableFuture().join();
        }
        void sendBinary(byte[] b) {
            socket.sendBinary(ByteBuffer.wrap(b), true).toCompletableFuture().join();
        }

        /** Drain inbound frames until the window elapses or the peer closes. */
        List<Message> collect(Duration window) {
            List<Message> out = new ArrayList<>();
            long deadline = System.nanoTime() + window.toNanos();
            synchronized (lock) {
                while (true) {
                    while (!inbox.isEmpty()) { out.add(inbox.poll()); }
                    long remMs = (deadline - System.nanoTime()) / 1_000_000L;
                    if (remMs <= 0 || closedFlag[0]) {
                        while (!inbox.isEmpty()) { out.add(inbox.poll()); }
                        break;
                    }
                    try { lock.wait(remMs); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            return out;
        }

        @Override
        public void close() {
            try { socket.sendClose(WebSocket.NORMAL_CLOSURE, "done"); } catch (Exception ignored) { }
        }
    }
}
