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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GraphQL entry point reached via {@link Tester#graphql(String)}. */
public final class GraphQLFacet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Tester t;
    private final String endpoint;

    GraphQLFacet(Tester t, String endpoint) {
        this.t = t;
        this.endpoint = endpoint;
    }

    public GraphQLStep query(String operation, Map<String, Object> variables) {
        t.flushPending();
        GraphQLStep s = new GraphQLStep(t, endpoint, operation, variables);
        t.setPending(s);
        return s;
    }

    /** One GraphQL operation. */
    public static final class GraphQLStep implements Committable {

        private final Tester t;
        private final String endpoint;
        private final String operation;
        private final Map<String, Object> variables;
        private final Map<String, String> headers = new HashMap<>();
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private HttpResponse<byte[]> resp;
        private Map<String, Object> parsed;
        private final java.util.List<String> failures = new java.util.ArrayList<>();
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;

        GraphQLStep(Tester t, String endpoint, String operation, Map<String, Object> variables) {
            Map<String, String> v = t.snapshotVars();
            this.t = t;
            this.endpoint = Interpolate.apply(endpoint, v);
            this.operation = Interpolate.apply(operation, v);
            this.variables = variables;
        }

        public GraphQLStep header(String k, String v) {
            if (sent) { return fail("header() after send"); }
            headers.put(k, Interpolate.apply(v, t.snapshotVars()));
            return this;
        }

        public GraphQLStep expectStatus(int code) {
            if (!ensureSent()) { return this; }
            if (resp != null && resp.statusCode() != code) {
                fail("expectStatus: want " + code + ", got " + resp.statusCode());
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public GraphQLStep expectNoErrors() {
            if (!ensureSent()) { return this; }
            Object errs = parsed.get("errors");
            if (errs instanceof List && !((List<Object>) errs).isEmpty()) {
                fail("expectNoErrors: " + ((List<Object>) errs).size() + " error(s)");
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public GraphQLStep expectErrors(int n) {
            if (!ensureSent()) { return this; }
            Object errs = parsed.get("errors");
            int got = errs instanceof List ? ((List<Object>) errs).size() : 0;
            if (got < n) {
                fail("expectErrors: want >=" + n + ", got " + got);
            }
            return this;
        }

        public GraphQLStep expectField(String jsonPath, Object want) {
            if (!ensureSent()) { return this; }
            try {
                Object got = JsonPath.resolve(parsed, jsonPath);
                if (!JsonPath.equalsLoose(got, want)) {
                    fail("expectField " + jsonPath + ": want " + want + ", got " + got);
                }
            } catch (JsonPathException e) {
                fail("expectField " + jsonPath + ": " + e.getMessage());
            }
            return this;
        }

        public GraphQLStep extract(String jsonPath, String name) {
            if (!ensureSent()) { return this; }
            try {
                Object got = JsonPath.resolve(parsed, jsonPath);
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

        private GraphQLStep fail(String msg) { failures.add(msg); return this; }

        @SuppressWarnings("unchecked")
        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query", operation);
            if (variables != null) { body.put("variables", variables); }
            String raw;
            try {
                raw = MAPPER.writeValueAsString(body);
            } catch (JsonProcessingException e) {
                abortChain = true;
                fail("marshal body: " + e.getMessage());
                return false;
            }
            raw = Interpolate.apply(raw, t.snapshotVars());

            String url = endpoint;
            if (!url.startsWith("http://") && !url.startsWith("https://") && !t.baseUrl().isEmpty()) {
                if (!url.startsWith("/")) { url = "/" + url; }
                url = t.baseUrl() + url;
            }
            HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url));
            b.header("Content-Type", "application/json");
            for (Map.Entry<String, String> e : t.defaultHeaders().entrySet()) {
                if (!headers.containsKey(e.getKey())) {
                    b.header(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<String, String> e : headers.entrySet()) {
                b.header(e.getKey(), e.getValue());
            }
            b.POST(HttpRequest.BodyPublishers.ofString(raw, StandardCharsets.UTF_8));
            try {
                startedAt = Instant.now();
                resp = t.http2().send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
                endedAt = Instant.now();
            } catch (Exception e) {
                endedAt = Instant.now();
                fail("graphql: " + e.getMessage());
                abortChain = true;
                return false;
            }
            try {
                parsed = (Map<String, Object>) MAPPER.readValue(resp.body(), Object.class);
            } catch (Exception e) {
                fail("graphql: parse response: " + e.getMessage());
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
            rec.protocol = "graphql";
            rec.method = "POST";
            rec.name = "graphql " + endpoint;
            rec.url = endpoint;
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
