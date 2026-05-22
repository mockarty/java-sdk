// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Entry point returned by {@link Tester#http()}. */
public final class HttpFacet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Tester t;

    HttpFacet(Tester t) {
        this.t = t;
    }

    public HttpStep get(String path)    { return req("GET", path); }
    public HttpStep post(String path)   { return req("POST", path); }
    public HttpStep put(String path)    { return req("PUT", path); }
    public HttpStep patch(String path)  { return req("PATCH", path); }
    public HttpStep delete(String path) { return req("DELETE", path); }
    public HttpStep head(String path)   { return req("HEAD", path); }

    private HttpStep req(String method, String path) {
        t.flushPending();
        HttpStep s = new HttpStep(t, method, path);
        t.setPending(s);
        return s;
    }

    /** One HTTP call. Lazy-send on first Expect/Extract. */
    public static final class HttpStep implements Committable {

        private final Tester t;
        private final String method;
        private final String path;
        private final Map<String, String> headers = new HashMap<>();
        private byte[] body;
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private HttpResponse<byte[]> resp;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private final java.util.List<String> failures = new java.util.ArrayList<>();

        HttpStep(Tester t, String method, String path) {
            this.t = t;
            this.method = method;
            this.path = path;
        }

        // ── builders ──────────────────────────────────────────────────

        public HttpStep header(String k, String v) {
            if (sent) { return fail("header() after send"); }
            headers.put(k, Interpolate.apply(v, t.snapshotVars()));
            return this;
        }

        public HttpStep json(Object value) {
            if (sent) { return fail("json() after send"); }
            try {
                String raw = MAPPER.writeValueAsString(value);
                body = Interpolate.apply(raw, t.snapshotVars()).getBytes(StandardCharsets.UTF_8);
            } catch (JsonProcessingException e) {
                abortChain = true;
                return fail("marshal body: " + e.getMessage());
            }
            headers.putIfAbsent("Content-Type", "application/json");
            return this;
        }

        public HttpStep body(byte[] b, String contentType) {
            if (sent) { return fail("body() after send"); }
            if (contentType != null) {
                String lower = contentType.toLowerCase(Locale.ROOT);
                if (lower.startsWith("text/") || lower.equals("application/x-www-form-urlencoded")) {
                    String s = new String(b, StandardCharsets.UTF_8);
                    b = Interpolate.apply(s, t.snapshotVars()).getBytes(StandardCharsets.UTF_8);
                }
                headers.put("Content-Type", contentType);
            }
            this.body = b.clone();
            return this;
        }

        // ── verdict + assertions ──────────────────────────────────────

        public HttpStep send() { ensureSent(); return this; }

        public HttpStep expectStatus(int code) {
            if (!ensureSent()) { return this; }
            if (resp.statusCode() != code) {
                fail("expectStatus: want " + code + ", got " + resp.statusCode());
            }
            return this;
        }

        public HttpStep expectHeader(String k, String v) {
            if (!ensureSent()) { return this; }
            String got = resp.headers().firstValue(k).orElse("");
            if (!got.equals(v)) {
                fail("expectHeader " + k + ": want \"" + v + "\", got \"" + got + "\"");
            }
            return this;
        }

        public HttpStep expectBodyContains(String sub) {
            if (!ensureSent()) { return this; }
            String body = new String(resp.body(), StandardCharsets.UTF_8);
            if (!body.contains(sub)) {
                fail("expectBodyContains: \"" + sub + "\" not found");
            }
            return this;
        }

        public HttpStep expectJsonPath(String jsonPath, Object want) {
            if (!ensureSent()) { return this; }
            try {
                Object got = JsonPath.resolve(parse(), jsonPath);
                if (!JsonPath.equalsLoose(got, want)) {
                    fail("expectJsonPath " + jsonPath + ": want " + want + ", got " + got);
                }
            } catch (JsonPathException e) {
                fail("expectJsonPath " + jsonPath + ": " + e.getMessage());
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public HttpStep expectJsonArrayLen(String jsonPath, int n) {
            if (!ensureSent()) { return this; }
            try {
                Object got = JsonPath.resolve(parse(), jsonPath);
                if (!(got instanceof java.util.List)) {
                    fail("expectJsonArrayLen " + jsonPath + ": not an array");
                    return this;
                }
                int len = ((java.util.List<Object>) got).size();
                if (len != n) {
                    fail("expectJsonArrayLen " + jsonPath + ": want " + n + ", got " + len);
                }
            } catch (JsonPathException e) {
                fail("expectJsonArrayLen " + jsonPath + ": " + e.getMessage());
            }
            return this;
        }

        public HttpStep extract(String jsonPath, String name) {
            if (!ensureSent()) { return this; }
            try {
                Object got = JsonPath.resolve(parse(), jsonPath);
                t.setVar(name, stringify(got));
            } catch (JsonPathException e) {
                fail("extract " + jsonPath + ": " + e.getMessage());
            }
            return this;
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        // ── internals ─────────────────────────────────────────────────

        private HttpStep fail(String msg) { failures.add(msg); return this; }

        private Object parse() {
            try {
                return MAPPER.readValue(resp.body(), Object.class);
            } catch (Exception e) {
                throw new JsonPathException("response is not JSON: " + e.getMessage(), e);
            }
        }

        private String buildUrl() {
            String p = Interpolate.apply(path, t.snapshotVars());
            if (p.startsWith("http://") || p.startsWith("https://")) {
                return p;
            }
            String base = t.baseUrl();
            if (base.isEmpty()) { return p; }
            if (!p.startsWith("/")) { p = "/" + p; }
            return base + p;
        }

        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            try {
                HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(buildUrl()));
                HttpRequest.BodyPublisher pub = body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body);
                b.method(method, pub);
                for (Map.Entry<String, String> e : t.defaultHeaders().entrySet()) {
                    if (!headers.containsKey(e.getKey())) {
                        b.header(e.getKey(), e.getValue());
                    }
                }
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    b.header(e.getKey(), e.getValue());
                }
                startedAt = Instant.now();
                resp = t.http2().send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
                endedAt = Instant.now();
            } catch (Exception e) {
                endedAt = Instant.now();
                fail("http: " + e.getMessage());
                abortChain = true;
                return false;
            }
            return true;
        }

        @Override
        public void commit() {
            if (committed) { return; }
            committed = true;
            if (!sent) { ensureSent(); }
            StepRecord rec = new StepRecord();
            rec.protocol = "http";
            rec.method = method;
            rec.url = buildUrl();
            rec.name = method + " " + rec.url;
            rec.statusOrCode = resp == null ? 0 : resp.statusCode();
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }

        private static String stringify(Object v) {
            if (v == null) { return ""; }
            if (v instanceof Boolean) { return ((Boolean) v) ? "true" : "false"; }
            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return Long.toString((long) d);
                }
                return Double.toString(d);
            }
            if (v instanceof String) { return (String) v; }
            try {
                return MAPPER.writeValueAsString(v);
            } catch (JsonProcessingException e) {
                return v.toString();
            }
        }
    }
}
