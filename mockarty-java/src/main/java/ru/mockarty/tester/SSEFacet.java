// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Server-Sent-Events (SSE) facet. Mirrors {@code sdk/go-sdk/tester/sse.go}
 * and the Python port so an SSE suite translates 1:1 across the three SDKs.
 *
 * <p>Opens a streaming {@code GET} with {@code Accept: text/event-stream},
 * collects events for a bounded {@link #listen(Duration) window}, parses the
 * WHATWG SSE wire format, then exposes count / event / data / JSONPath
 * assertions and an {@code extract} into the shared variable store. The listen
 * window elapsing is NOT a failure (an empty stream is visible via
 * {@link SSEStep#expectMinEvents}).</p>
 */
public final class SSEFacet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Tester t;
    private final String endpoint;

    SSEFacet(Tester t, String endpoint) {
        this.t = t;
        this.endpoint = endpoint;
    }

    /** Start the SSE chain; the GET fires lazily on the first assertion/extract. */
    public SSEStep subscribe() {
        t.flushPending();
        SSEStep s = new SSEStep(t, Interpolate.apply(endpoint, t.snapshotVars()));
        t.setPending(s);
        return s;
    }

    /** One parsed Server-Sent-Events record. */
    public static final class Event {
        public String event = "";  // "" → spec default of "message"
        public String data = "";   // concatenated data: lines, \n-joined
        public String id = "";     // last-event-id field
        public int retry;          // retry hint in ms, 0 if absent
    }

    public static final class SSEStep implements Committable {
        private final Tester t;
        private final String endpoint;
        private Duration listen = Duration.ofSeconds(5);
        private final List<String[]> headers = new ArrayList<>();
        private String lastEventID = "";

        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private int statusCode;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private final List<Event> events = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();

        SSEStep(Tester t, String endpoint) {
            this.t = t;
            this.endpoint = endpoint;
        }

        // ── builders ──────────────────────────────────────────────────

        /** Maximum collection window (default 5s). */
        public SSEStep listen(Duration d) {
            if (guard("listen")) { return this; }
            if (d != null && !d.isZero() && !d.isNegative()) { this.listen = d; }
            return this;
        }

        /** Sets a request header on the underlying GET, {@code {{var}}}-interpolated. */
        public SSEStep header(String k, String v) {
            if (guard("header")) { return this; }
            headers.add(new String[]{k, Interpolate.apply(v, t.snapshotVars())});
            return this;
        }

        /** Shorthand for {@code header("Last-Event-ID", id)} — resume a stream. */
        public SSEStep lastEventID(String id) {
            if (guard("lastEventID")) { return this; }
            this.lastEventID = Interpolate.apply(id, t.snapshotVars());
            return this;
        }

        // ── assertions ────────────────────────────────────────────────

        public SSEStep expectMinEvents(int n) {
            if (!ensureSent()) { return this; }
            if (events.size() < n) { fail("expectMinEvents: want >=" + n + ", got " + events.size()); }
            return this;
        }

        public SSEStep expectExactEvents(int n) {
            if (!ensureSent()) { return this; }
            if (events.size() != n) { fail("expectExactEvents: want " + n + ", got " + events.size()); }
            return this;
        }

        /** Asserts an event with the given name was received ("" = "message"). */
        public SSEStep expectEvent(String name) {
            if (!ensureSent()) { return this; }
            if (find(name) == null) { fail("expectEvent \"" + name + "\": not received (" + events.size() + " events)"); }
            return this;
        }

        /** Asserts an event with the given name carries the exact data string. */
        public SSEStep expectEventData(String name, String data) {
            if (!ensureSent()) { return this; }
            Event ev = find(name);
            if (ev == null) { fail("expectEventData \"" + name + "\": event not received"); return this; }
            if (!ev.data.equals(data)) { fail("expectEventData \"" + name + "\": want \"" + data + "\", got \"" + ev.data + "\""); }
            return this;
        }

        /** Asserts a JSONPath value inside the data of the FIRST event matching eventName. */
        public SSEStep expectJsonPath(String eventName, String path, Object want) {
            if (!ensureSent()) { return this; }
            try {
                Object got = evalEvent(eventName, path);
                if (!JsonPath.equalsLoose(got, want)) {
                    fail("expectJsonPath[" + eventName + "] " + path + ": want " + want + ", got " + got);
                }
            } catch (Exception e) {
                fail("expectJsonPath[" + eventName + "] " + path + ": " + e.getMessage());
            }
            return this;
        }

        /** Resolves a JSONPath inside the FIRST event matching eventName into varName. */
        public SSEStep extract(String eventName, String path, String varName) {
            if (!ensureSent()) { return this; }
            try {
                Object got = evalEvent(eventName, path);
                t.setVar(varName, TesterScalar.stringify(got));
            } catch (Exception e) {
                fail("extract[" + eventName + "] " + path + ": " + e.getMessage());
            }
            return this;
        }

        /** A copy of the collected events — escape hatch for custom matching. */
        public List<Event> events() {
            ensureSent();
            return new ArrayList<>(events);
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        // ── internals ─────────────────────────────────────────────────

        private boolean guard(String method) {
            if (sent) { fail(method + "() called after subscribe"); return true; }
            return false;
        }

        private void fail(String msg) { failures.add(msg); }

        private Event find(String name) {
            String want = name.isEmpty() ? "message" : name;
            for (Event e : events) {
                String ev = e.event.isEmpty() ? "message" : e.event;
                if (ev.equals(want)) { return e; }
            }
            return null;
        }

        private Object evalEvent(String eventName, String path) throws Exception {
            Event ev = find(eventName);
            if (ev == null) { throw new IllegalStateException("event \"" + eventName + "\" not received"); }
            JsonNode doc = MAPPER.readTree(ev.data);
            return JsonPath.resolve(MAPPER.convertValue(doc, Object.class), path);
        }

        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }

            String url = endpoint;
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                String base = t.baseUrl();
                if (!base.isEmpty()) {
                    if (!url.startsWith("/")) { url = "/" + url; }
                    url = base + url;
                }
            }

            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(listen)
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache")
                    .GET();
            if (!lastEventID.isEmpty()) { b.header("Last-Event-ID", lastEventID); }
            for (String[] h : headers) { b.header(h[0], h[1]); }

            startedAt = Instant.now();
            HttpResponse<Stream<String>> resp;
            try {
                resp = t.http2().send(b.build(), HttpResponse.BodyHandlers.ofLines());
            } catch (Exception e) {
                endedAt = Instant.now();
                // A pre-data timeout means we listened for the full window with
                // no response — a legitimate empty outcome, not a step failure.
                if (isDeadline(e)) { return true; }
                fail("sse: " + e.getMessage());
                abortChain = true;
                return false;
            }
            statusCode = resp.statusCode();
            if (statusCode / 100 != 2) {
                endedAt = Instant.now();
                fail("sse: HTTP " + statusCode);
                abortChain = true;
                return false;
            }
            try (Stream<String> lines = resp.body()) {
                events.addAll(parse(lines));
            } catch (Exception e) {
                // Mid-stream timeout = window elapsed; keep the events collected
                // so far (Go treats the deadline as a non-failure).
                if (!isDeadline(e)) {
                    fail("sse: " + e.getMessage());
                }
            }
            endedAt = Instant.now();
            return true;
        }

        @Override
        public void commit() {
            if (committed) { return; }
            committed = true;
            if (!sent) { ensureSent(); }
            StepRecord rec = new StepRecord();
            rec.protocol = "sse";
            rec.method = "GET";
            rec.name = "sse " + endpoint;
            rec.url = endpoint;
            rec.statusOrCode = statusCode;
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }

        private static boolean isDeadline(Throwable e) {
            for (Throwable c = e; c != null; c = c.getCause()) {
                String n = c.getClass().getSimpleName();
                if (n.contains("HttpTimeout") || n.contains("Timeout")) { return true; }
                String m = c.getMessage();
                if (m != null && (m.contains("timed out") || m.contains("deadline") || m.contains("closed"))) {
                    return true;
                }
            }
            return false;
        }

        /**
         * WHATWG Server-Sent-Events parser. Lines split by the JDK
         * {@code ofLines()} (\n / \r\n / \r); a ":" line is a comment; a
         * "field: value" line strips one leading space; a blank line dispatches
         * the buffered event; "data" lines are \n-joined within a record.
         */
        private static List<Event> parse(Stream<String> lines) {
            List<Event> out = new ArrayList<>();
            Event[] cur = {new Event()};
            StringBuilder[] dataB = {new StringBuilder()};
            boolean[] seen = {false};
            lines.forEach(line -> {
                if (line.isEmpty()) {
                    if (seen[0]) {
                        cur[0].data = dataB[0].toString();
                        out.add(cur[0]);
                        cur[0] = new Event();
                        dataB[0] = new StringBuilder();
                        seen[0] = false;
                    }
                    return;
                }
                if (line.startsWith(":")) { return; }
                int idx = line.indexOf(':');
                String field = idx < 0 ? line : line.substring(0, idx);
                String value = idx < 0 ? "" : line.substring(idx + 1);
                if (value.startsWith(" ")) { value = value.substring(1); }
                switch (field) {
                    case "event":
                        cur[0].event = value; seen[0] = true; break;
                    case "data":
                        if (dataB[0].length() > 0) { dataB[0].append('\n'); }
                        dataB[0].append(value); seen[0] = true; break;
                    case "id":
                        cur[0].id = value; seen[0] = true; break;
                    case "retry":
                        int n = 0; boolean ok = !value.isEmpty();
                        for (int i = 0; i < value.length(); i++) {
                            char ch = value.charAt(i);
                            if (ch < '0' || ch > '9') { ok = false; break; }
                            n = n * 10 + (ch - '0');
                        }
                        cur[0].retry = ok ? n : 0; seen[0] = true; break;
                    default:
                        break;
                }
            });
            if (seen[0]) {
                cur[0].data = dataB[0].toString();
                out.add(cur[0]);
            }
            return out;
        }
    }
}
