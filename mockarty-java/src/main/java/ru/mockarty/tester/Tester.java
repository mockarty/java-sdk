// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fluent test builder. Mirrors {@code sdk/go-sdk/tester} and
 * {@code sdk/py-sdk/src/mockarty/tester} so chains translate 1:1 across
 * languages.
 *
 * <p>Example:
 * <pre>
 * Tester t = new Tester.Builder()
 *     .baseUrl("http://localhost:8080")
 *     .build();
 * t.http().get("/users/42")
 *     .expectStatus(200)
 *     .expectJsonPath("$.name", "Alice")
 *     .extract("$.token", "token");
 * t.http().post("/orders")
 *     .header("X-Auth", "Bearer {{token}}")
 *     .json(Map.of("userId", 42))
 *     .expectStatus(201);
 * t.finish();
 * if (!t.ok()) throw new AssertionError(t.errors());
 * </pre>
 */
public final class Tester implements AutoCloseable {

    private final String baseUrl;
    private final HttpClient http;
    private final Map<String, String> defaultHeaders;
    private final boolean failFast;
    private final Map<String, String> vars = new HashMap<>();
    private final List<StepRecord> steps = new ArrayList<>();
    private final List<String> errs = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private Committable pending;

    Tester(String baseUrl, HttpClient http, Map<String, String> defaultHeaders, boolean failFast) {
        this.baseUrl = baseUrl == null ? "" : stripTrailingSlash(baseUrl);
        this.http = http;
        this.defaultHeaders = defaultHeaders == null ? Map.of() : Map.copyOf(defaultHeaders);
        this.failFast = failFast;
    }

    public HttpFacet http() {
        return new HttpFacet(this);
    }

    public GraphQLFacet graphql(String endpoint) {
        return new GraphQLFacet(this, endpoint);
    }

    public KafkaFacet kafka(KafkaFacet.KafkaBroker broker) {
        return new KafkaFacet(this, broker);
    }

    public RabbitMQFacet rabbitmq(RabbitMQFacet.RabbitMQBroker broker) {
        return new RabbitMQFacet(this, broker);
    }

    public SOAPFacet soap(String endpoint) {
        return new SOAPFacet(this, endpoint);
    }

    public DBFacet db(DBFacet.SQLConn conn) {
        return new DBFacet(this, conn);
    }

    public S3Facet s3(S3Facet.S3Client client) {
        return new S3Facet(this, client);
    }

    public SMTPFacet smtp(SMTPFacet.SMTPSender sender) {
        return new SMTPFacet(this, sender);
    }

    public SocketIOFacet socketio(String url) {
        return new SocketIOFacet(this, url);
    }

    public Tester finish() {
        flushPending();
        return this;
    }

    public boolean ok() {
        flushPending();
        lock.lock();
        try {
            return errs.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public List<String> errors() {
        flushPending();
        lock.lock();
        try {
            return new ArrayList<>(errs);
        } finally {
            lock.unlock();
        }
    }

    public List<StepRecord> report() {
        flushPending();
        lock.lock();
        try {
            return new ArrayList<>(steps);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, String> vars() {
        lock.lock();
        try {
            return new HashMap<>(vars);
        } finally {
            lock.unlock();
        }
    }

    public void setVar(String name, String value) {
        lock.lock();
        try {
            vars.put(name, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        finish();
    }

    /**
     * Group child steps under a labelled synthetic parent step so the
     * Allure / external-runs report renders the chain as a tree rather
     * than a flat list. Mirrors {@code Tester.Wrap} in the Go SDK and
     * {@code mockarty.tester.wrap()} in the Python SDK.
     *
     * <p>Mechanics: any pending chain commits before {@code body} runs;
     * any chain left pending inside {@code body} flushes when it
     * returns; a synthetic {@code wrap} StepRecord with the supplied
     * name is appended so downstream renderers can group siblings.
     * Panics inside {@code body} re-propagate after the cleanup runs
     * so {@code try/finally} idioms aren't surprised.</p>
     *
     * @param name short human-readable label (e.g. "login flow"). Used
     *             verbatim in the report.
     * @param body the chain to execute. May be {@code null} (no-op).
     * @return this Tester for fluent chaining.
     */
    public Tester wrap(String name, Runnable body) {
        flushPending();
        if (body == null) {
            return this;
        }
        StepRecord marker = new StepRecord();
        marker.protocol = "wrap";
        marker.name = name == null ? "" : name;
        marker.startedAt = Instant.now();
        try {
            body.run();
        } finally {
            flushPending();
            marker.endedAt = Instant.now();
            lock.lock();
            try {
                steps.add(marker);
            } finally {
                lock.unlock();
            }
        }
        return this;
    }

    // ── package-private chain machinery ───────────────────────────────

    String baseUrl() {
        return baseUrl;
    }

    HttpClient http2() {
        return http;
    }

    Map<String, String> defaultHeaders() {
        return defaultHeaders;
    }

    Map<String, String> snapshotVars() {
        lock.lock();
        try {
            return new HashMap<>(vars);
        } finally {
            lock.unlock();
        }
    }

    boolean shouldAbort() {
        if (!failFast) {
            return false;
        }
        lock.lock();
        try {
            return !errs.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    void setPending(Committable c) {
        lock.lock();
        try {
            pending = c;
        } finally {
            lock.unlock();
        }
    }

    void clearPending(Committable c) {
        lock.lock();
        try {
            if (pending == c) {
                pending = null;
            }
        } finally {
            lock.unlock();
        }
    }

    void flushPending() {
        Committable p;
        lock.lock();
        try {
            p = pending;
            pending = null;
        } finally {
            lock.unlock();
        }
        if (p != null) {
            p.commit();
        }
    }

    void recordStep(StepRecord rec) {
        lock.lock();
        try {
            steps.add(rec);
            for (String f : rec.failures) {
                errs.add(rec.name + ": " + f);
            }
        } finally {
            lock.unlock();
        }
    }

    // ── builder ───────────────────────────────────────────────────────

    public static final class Builder {
        private String baseUrl = "";
        private HttpClient http;
        private final Map<String, String> headers = new HashMap<>();
        private boolean failFast;
        private Duration timeout = Duration.ofSeconds(30);

        public Builder baseUrl(String url) {
            this.baseUrl = url;
            return this;
        }

        public Builder httpClient(HttpClient client) {
            this.http = client;
            return this;
        }

        public Builder header(String k, String v) {
            this.headers.put(k, v);
            return this;
        }

        public Builder failFast() {
            this.failFast = true;
            return this;
        }

        public Builder timeout(Duration d) {
            this.timeout = d;
            return this;
        }

        public Tester build() {
            HttpClient c = http != null ? http
                    : HttpClient.newBuilder().connectTimeout(timeout).build();
            return new Tester(baseUrl, c, headers, failFast);
        }
    }

    private static String stripTrailingSlash(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
