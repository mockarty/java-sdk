// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S3 facet. Mirrors {@code sdk/go-sdk/tester/s3.go} and the Python port.
 * Lets a CI test author exercise an S3-compatible endpoint (a Mockarty
 * S3 mock, MinIO, or AWS) and assert on put/get/head/list/delete results.
 *
 * <p>{@link S3Client} is the contract the facet needs; {@link S3HttpClient}
 * is a path-style implementation over {@code java.net.http} (no AWS SDK
 * dependency). Tests can plug an in-memory fake.</p>
 */
public final class S3Facet {

    private final Tester t;
    private final S3Client client;

    S3Facet(Tester t, S3Client client) {
        this.t = t;
        this.client = client;
    }

    public S3Step put(String bucket, String key) { return step(Op.PUT, bucket, key); }
    public S3Step get(String bucket, String key) { return step(Op.GET, bucket, key); }
    public S3Step head(String bucket, String key) { return step(Op.HEAD, bucket, key); }
    public S3Step list(String bucket) { return step(Op.LIST, bucket, ""); }
    public S3Step delete(String bucket, String key) { return step(Op.DELETE, bucket, key); }

    private S3Step step(Op op, String bucket, String key) {
        t.flushPending();
        Map<String, String> v = t.snapshotVars();
        S3Step s = new S3Step(t, client, op,
                Interpolate.apply(bucket, v), Interpolate.apply(key, v));
        t.setPending(s);
        return s;
    }

    enum Op {
        PUT, GET, HEAD, LIST, DELETE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /** Minimal contract the S3 facet needs. */
    public interface S3Client {
        PutResult putObject(String bucket, String key, byte[] body, String contentType,
                            Map<String, String> metadata) throws Exception;
        GetResult getObject(String bucket, String key) throws Exception;
        HeadResult headObject(String bucket, String key) throws Exception;
        ListResult listObjects(String bucket, String prefix) throws Exception;
        DeleteResult deleteObject(String bucket, String key) throws Exception;
    }

    public static final class PutResult {
        public int statusCode;
        public String etag = "";
    }

    public static final class GetResult {
        public int statusCode;
        public byte[] body = new byte[0];
        public String contentType = "";
        public String etag = "";
        public Map<String, String> metadata = new HashMap<>();
    }

    public static final class HeadResult {
        public int statusCode;
        public boolean exists;
        public String contentType = "";
        public String etag = "";
        public long contentLength;
        public Map<String, String> metadata = new HashMap<>();
    }

    public static final class ObjectInfo {
        public String key = "";
        public long size;
        public String etag = "";
    }

    public static final class ListResult {
        public int statusCode;
        public boolean isTruncated;
        public List<ObjectInfo> objects = new ArrayList<>();
    }

    public static final class DeleteResult {
        public int statusCode;
    }

    public static final class S3Step implements Committable {
        private final Tester t;
        private final S3Client client;
        private final Op op;
        private final String bucket;
        private final String key;
        private final Map<String, String> metadata = new HashMap<>();
        private String prefix = "";
        private String contentType = "";
        private byte[] body = new byte[0];

        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private Throwable err;
        private final List<String> failures = new ArrayList<>();

        private PutResult putRes;
        private GetResult getRes;
        private HeadResult headRes;
        private ListResult listRes;
        private DeleteResult deleteRes;

        S3Step(Tester t, S3Client client, Op op, String bucket, String key) {
            this.t = t; this.client = client; this.op = op; this.bucket = bucket; this.key = key;
        }

        // ── builders ──────────────────────────────────────────────────

        public S3Step body(String text) {
            if (guard("body")) { return this; }
            this.body = Interpolate.apply(text, t.snapshotVars()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return this;
        }
        public S3Step bytes(byte[] b) {
            if (guard("bytes")) { return this; }
            this.body = b == null ? new byte[0] : b.clone();
            return this;
        }
        public S3Step contentType(String ct) {
            if (guard("contentType")) { return this; }
            this.contentType = ct;
            return this;
        }
        public S3Step meta(String k, String v) {
            if (guard("meta")) { return this; }
            metadata.put(k, Interpolate.apply(v, t.snapshotVars()));
            return this;
        }
        public S3Step prefix(String p) {
            if (guard("prefix")) { return this; }
            this.prefix = Interpolate.apply(p, t.snapshotVars());
            return this;
        }

        // ── assertions ────────────────────────────────────────────────

        public S3Step expectOK() {
            if (!ensureSent()) { return this; }
            if (err != null) { fail("expectOK: " + err.getMessage()); }
            return this;
        }
        public S3Step expectError() {
            ensureSent();
            if (err == null) { fail("expectError: operation succeeded"); }
            return this;
        }
        public S3Step expectStatus(int code) {
            if (!ensureSent()) { return this; }
            int got = statusCode();
            if (got != code) { fail("expectStatus: want " + code + ", got " + got); }
            return this;
        }
        public S3Step expectBodyContains(String sub) {
            if (!ensureSent()) { return this; }
            if (op != Op.GET) { return fail("expectBodyContains only valid after get()"); }
            if (!new String(getRes.body, java.nio.charset.StandardCharsets.UTF_8).contains(sub)) {
                fail("expectBodyContains: \"" + sub + "\" not found");
            }
            return this;
        }
        public S3Step expectBodyEquals(String want) {
            if (!ensureSent()) { return this; }
            if (op != Op.GET) { return fail("expectBodyEquals only valid after get()"); }
            String got = new String(getRes.body, java.nio.charset.StandardCharsets.UTF_8);
            if (!got.equals(want)) { fail("expectBodyEquals: want \"" + want + "\", got \"" + got + "\""); }
            return this;
        }
        public S3Step expectMeta(String k, String want) {
            if (!ensureSent()) { return this; }
            Map<String, String> meta;
            if (op == Op.GET) { meta = getRes.metadata; }
            else if (op == Op.HEAD) { meta = headRes.metadata; }
            else { return fail("expectMeta only valid after get() or head()"); }
            String got = meta.getOrDefault(k, "");
            if (!got.equals(want)) { fail("expectMeta[" + k + "]: want \"" + want + "\", got \"" + got + "\""); }
            return this;
        }
        public S3Step expectContentType(String want) {
            if (!ensureSent()) { return this; }
            String got;
            if (op == Op.GET) { got = getRes.contentType; }
            else if (op == Op.HEAD) { got = headRes.contentType; }
            else { return fail("expectContentType only valid after get() or head()"); }
            if (!got.equals(want)) { fail("expectContentType: want \"" + want + "\", got \"" + got + "\""); }
            return this;
        }
        public S3Step expectExists() {
            if (!ensureSent()) { return this; }
            if (op != Op.HEAD) { return fail("expectExists only valid after head()"); }
            if (!headRes.exists) { fail("expectExists: object not found"); }
            return this;
        }
        public S3Step expectAbsent() {
            if (!ensureSent()) { return this; }
            if (op != Op.HEAD) { return fail("expectAbsent only valid after head()"); }
            if (headRes.exists) { fail("expectAbsent: object exists"); }
            return this;
        }
        public S3Step expectObjectCount(int n) {
            if (!ensureSent()) { return this; }
            if (op != Op.LIST) { return fail("expectObjectCount only valid after list()"); }
            if (listRes.objects.size() != n) {
                fail("expectObjectCount: want " + n + ", got " + listRes.objects.size());
            }
            return this;
        }
        public S3Step expectKey(String key) {
            if (!ensureSent()) { return this; }
            if (op != Op.LIST) { return fail("expectKey only valid after list()"); }
            for (ObjectInfo o : listRes.objects) {
                if (o.key.equals(key)) { return this; }
            }
            return fail("expectKey: \"" + key + "\" not in listing");
        }

        // ── extraction / escape hatches ───────────────────────────────

        public S3Step extractETag(String name) {
            if (!ensureSent()) { return this; }
            String etag;
            if (op == Op.PUT) { etag = putRes.etag; }
            else if (op == Op.GET) { etag = getRes.etag; }
            else if (op == Op.HEAD) { etag = headRes.etag; }
            else { return fail("extractETag only valid after put(), get(), or head()"); }
            t.setVar(name, etag);
            return this;
        }
        public S3Step extractBody(String name) {
            if (!ensureSent()) { return this; }
            if (op != Op.GET) { return fail("extractBody only valid after get()"); }
            t.setVar(name, new String(getRes.body, java.nio.charset.StandardCharsets.UTF_8));
            return this;
        }
        public byte[] getBody() {
            ensureSent();
            return getRes == null ? new byte[0] : getRes.body.clone();
        }
        public List<ObjectInfo> objects() {
            ensureSent();
            return listRes == null ? Collections.emptyList() : new ArrayList<>(listRes.objects);
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        // ── internals ─────────────────────────────────────────────────

        private S3Step fail(String msg) { failures.add(msg); return this; }

        private boolean guard(String method) {
            if (sent) { fail(method + "() called after send"); return true; }
            return false;
        }

        private int statusCode() {
            switch (op) {
                case PUT: return putRes == null ? 0 : putRes.statusCode;
                case GET: return getRes == null ? 0 : getRes.statusCode;
                case HEAD: return headRes == null ? 0 : headRes.statusCode;
                case LIST: return listRes == null ? 0 : listRes.statusCode;
                case DELETE: return deleteRes == null ? 0 : deleteRes.statusCode;
                default: return 0;
            }
        }

        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            startedAt = Instant.now();
            try {
                switch (op) {
                    case PUT: putRes = client.putObject(bucket, key, body, contentType, metadata); break;
                    case GET: getRes = client.getObject(bucket, key); break;
                    case HEAD: headRes = client.headObject(bucket, key); break;
                    case LIST: listRes = client.listObjects(bucket, prefix); break;
                    case DELETE: deleteRes = client.deleteObject(bucket, key); break;
                    default: break;
                }
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
            String target = bucket;
            if (key != null && !key.isEmpty()) { target = bucket + "/" + key; }
            StepRecord rec = new StepRecord();
            rec.protocol = "s3";
            rec.method = op.toString();
            rec.name = "s3 " + op + " " + target;
            rec.url = target;
            rec.statusOrCode = statusCode();
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }
}
