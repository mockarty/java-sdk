// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * gRPC facet — the fluent assertion layer over a user-supplied
 * {@link GrpcInvoker}. Mirrors {@code sdk/go-sdk/tester/grpc.go} and the Python
 * port so a gRPC suite translates 1:1 across the three SDKs.
 *
 * <p>The facet itself performs NO protobuf reflection — it delegates the call
 * to the injected invoker (the {@code mockarty-protocols} GrpcClient satisfies
 * it via a one-line adapter; tests pass an in-memory fake to stay offline).
 * Requests/responses are JSON-shaped at the SDK boundary.</p>
 */
public final class GrpcFacet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The minimal contract the gRPC facet needs from a client. */
    @FunctionalInterface
    public interface GrpcInvoker {
        /**
         * Invokes a unary method. {@code fullMethod} is the canonical
         * "Package.Service/Method" form; {@code req} is a JSON-shaped value
         * (map / list / scalar) or null for empty-request calls. Returns the
         * response as a JSON-shaped map; THROWS on a gRPC-level error (the
         * facet's {@code expectError} asserts on that throw).
         */
        Map<String, Object> invokeJson(String fullMethod, Object req) throws Exception;
    }

    private final Tester t;
    private final GrpcInvoker client;

    GrpcFacet(Tester t, GrpcInvoker client) {
        this.t = t;
        this.client = client;
    }

    /** Start a unary gRPC call. Pass req=null for empty-request calls. */
    public GrpcStep call(String fullMethod, Object req) {
        t.flushPending();
        GrpcStep s = new GrpcStep(t, client, Interpolate.apply(fullMethod, t.snapshotVars()), req);
        t.setPending(s);
        return s;
    }

    public static final class GrpcStep implements Committable {
        private final Tester t;
        private final GrpcInvoker client;
        private final String fullMethod;
        private Object req;

        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private Map<String, Object> resp = new LinkedHashMap<>();
        private Exception err;
        private final List<String> failures = new ArrayList<>();

        GrpcStep(Tester t, GrpcInvoker client, String fullMethod, Object req) {
            this.t = t;
            this.client = client;
            this.fullMethod = fullMethod;
            this.req = req;
        }

        // ── assertions ────────────────────────────────────────────────

        public GrpcStep expectOk() {
            if (!ensureSent()) { return this; }
            if (err != null) { fail("expectOk: " + err.getMessage()); }
            return this;
        }
        public GrpcStep expectError() {
            ensureSent();
            if (err == null) { fail("expectError: call succeeded"); }
            return this;
        }
        /** Alias for {@link #expectJsonPath} — matches the canonical chain verb. */
        public GrpcStep expectField(String path, Object want) {
            return expectJsonPath(path, want);
        }
        public GrpcStep expectJsonPath(String path, Object want) {
            if (!ensureSent()) { return this; }
            if (err != null) { fail("expectJsonPath " + path + ": call errored: " + err.getMessage()); return this; }
            try {
                Object got = JsonPath.resolve(resp, path);
                if (!JsonPath.equalsLoose(got, want)) {
                    fail("expectJsonPath " + path + ": want " + want + ", got " + got);
                }
            } catch (Exception e) {
                fail("expectJsonPath " + path + ": " + e.getMessage());
            }
            return this;
        }
        public GrpcStep extract(String path, String name) {
            if (!ensureSent()) { return this; }
            if (err != null) { fail("extract " + path + ": call errored: " + err.getMessage()); return this; }
            try {
                Object got = JsonPath.resolve(resp, path);
                t.setVar(name, TesterScalar.stringify(got));
            } catch (Exception e) {
                fail("extract " + path + ": " + e.getMessage());
            }
            return this;
        }
        /** The decoded response body — escape hatch for custom matching. */
        public Map<String, Object> response() {
            ensureSent();
            return new LinkedHashMap<>(resp);
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        // ── internals ─────────────────────────────────────────────────

        private void fail(String msg) { failures.add(msg); }

        @SuppressWarnings("unchecked")
        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            // Per-step request interpolation — round-trip through JSON so {{var}}
            // substitution is consistent with HTTP / Kafka.
            if (req != null) {
                try {
                    String json = Interpolate.apply(MAPPER.writeValueAsString(req), t.snapshotVars());
                    req = MAPPER.readValue(json, Object.class);
                } catch (Exception ignored) {
                    // leave req as-is if it isn't JSON-serialisable
                }
            }
            startedAt = Instant.now();
            try {
                Map<String, Object> r = client.invokeJson(fullMethod, req);
                resp = r != null ? r : new LinkedHashMap<>();
            } catch (Exception e) {
                err = e;
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
            rec.protocol = "grpc";
            rec.method = "unary";
            rec.name = "grpc " + fullMethod;
            rec.url = fullMethod;
            rec.statusOrCode = err == null ? 0 : 1;
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }
}
