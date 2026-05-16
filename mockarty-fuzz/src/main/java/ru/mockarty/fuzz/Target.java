// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A fully-described fuzz target — the language-side representation that
 * the {@link Transpiler} converts into Mockarty's canonical fuzz JSON
 * config (schema-compatible with {@code internal/fuzzing/config.go
 * FuzzConfig}).
 *
 * <p>Build instances with the fluent {@link Builder} returned by
 * {@link #named(String)}:</p>
 * <pre>{@code
 * Target t = Target.named("login-stress")
 *     .description("Hit the login endpoint with bad inputs")
 *     .httpEndpoint("POST", "/api/v1/login")
 *     .seeds(
 *         Seed.of("valid",     "{\"username\":\"admin\",\"password\":\"x\"}"),
 *         Seed.of("missing-pw","{\"username\":\"admin\"}"))
 *     .mutator(Mutator.JSON)
 *     .duration(Duration.ofMinutes(5))
 *     .stopOnFinding(true)
 *     .reporter(Reporter.ALLURE)
 *     .assertion(Assertion.statusInRange(200, 299))
 *     .build();
 * }</pre>
 *
 * <p>{@link Target} is immutable; the builder can be re-used to derive
 * variants. The transpiler / {@link Runner} both treat the object as
 * read-only.</p>
 */
public final class Target {

    private final String name;
    private final String description;
    private final String namespace;
    private final Protocol protocol;
    private final String method;
    private final String baseUrl;
    private final String path;
    private final Map<String, String> headers;
    private final String grpcService;
    private final String grpcMethod;
    private final String graphQLPath;
    private final List<Seed> seeds;
    private final List<Mutator> mutators;
    private final List<Assertion> assertions;
    private final List<Reporter> reporters;
    private final Duration duration;
    private final int maxRequests;
    private final int concurrency;
    private final boolean stopOnFinding;
    private final List<String> payloadCategories;

    private Target(Builder b) {
        this.name = b.name;
        this.description = b.description;
        this.namespace = b.namespace;
        this.protocol = b.protocol;
        this.method = b.method;
        this.baseUrl = b.baseUrl;
        this.path = b.path;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(b.headers));
        this.grpcService = b.grpcService;
        this.grpcMethod = b.grpcMethod;
        this.graphQLPath = b.graphQLPath;
        this.seeds = List.copyOf(b.seeds);
        this.mutators = List.copyOf(b.mutators);
        this.assertions = List.copyOf(b.assertions);
        this.reporters = List.copyOf(b.reporters);
        this.duration = b.duration;
        this.maxRequests = b.maxRequests;
        this.concurrency = b.concurrency;
        this.stopOnFinding = b.stopOnFinding;
        this.payloadCategories = List.copyOf(b.payloadCategories);
    }

    /** Starts a new builder; the name is required and propagates into the JSON. */
    public static Builder named(String name) {
        return new Builder(name);
    }

    // ── Accessors (used by Transpiler + Runner; package-private only
    // where they shouldn't be reachable from user code) ───────────────

    public String name() { return name; }
    public String description() { return description; }
    public String namespace() { return namespace; }
    public Protocol protocol() { return protocol; }
    public String method() { return method; }
    public String baseUrl() { return baseUrl; }
    public String path() { return path; }
    public Map<String, String> headers() { return headers; }
    public String grpcService() { return grpcService; }
    public String grpcMethod() { return grpcMethod; }
    public String graphQLPath() { return graphQLPath; }
    public List<Seed> seeds() { return seeds; }
    public List<Mutator> mutators() { return mutators; }
    public List<Assertion> assertions() { return assertions; }
    public List<Reporter> reporters() { return reporters; }
    public Duration duration() { return duration; }
    public int maxRequests() { return maxRequests; }
    public int concurrency() { return concurrency; }
    public boolean stopOnFinding() { return stopOnFinding; }
    public List<String> payloadCategories() { return payloadCategories; }

    /**
     * Transpile to canonical JSON. Equivalent to
     * {@code Transpiler.toJson(this)} — exposed on Target for
     * discoverability.
     */
    public String toJson() {
        return Transpiler.toJson(this);
    }

    /**
     * Write the canonical JSON config to {@code path}. Convenience for
     * the on-disk → CI workflow described in the package readme.
     */
    public void writeTo(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toJson(), StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "Target{name='" + name + "', protocol=" + protocol
                + ", seeds=" + seeds.size() + ", mutators=" + mutators.size() + "}";
    }

    /**
     * Fluent builder for {@link Target}. Methods returning the builder
     * itself can be chained in any order; {@link #build()} performs final
     * validation. The builder is mutable but not thread-safe — callers
     * that build targets concurrently should each own a separate builder
     * (the {@link Target} they return IS safe to share).
     */
    public static final class Builder {

        private final String name;
        private String description = "";
        private String namespace = "";
        private Protocol protocol;
        private String method;
        private String baseUrl = "";
        private String path = "";
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String grpcService;
        private String grpcMethod;
        private String graphQLPath;
        private final List<Seed> seeds = new ArrayList<>();
        private final List<Mutator> mutators = new ArrayList<>();
        private final List<Assertion> assertions = new ArrayList<>();
        private final List<Reporter> reporters = new ArrayList<>();
        private Duration duration;
        private int maxRequests = 0;          // 0 = engine default
        private int concurrency = 0;          // 0 = engine default
        private boolean stopOnFinding = false;
        private final List<String> payloadCategories = new ArrayList<>();

        private Builder(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("target name must not be null or blank");
            }
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description == null ? "" : description;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace == null ? "" : namespace;
            return this;
        }

        /** Configures an HTTP endpoint; sets {@link Protocol#HTTP}. */
        public Builder httpEndpoint(String method, String path) {
            return httpEndpoint(method, "", path);
        }

        /** Configures an HTTP endpoint with an explicit base URL. */
        public Builder httpEndpoint(String method, String baseUrl, String path) {
            this.protocol = Protocol.HTTP;
            this.method = requireMethod(method);
            this.baseUrl = baseUrl == null ? "" : baseUrl;
            this.path = path == null ? "" : path;
            return this;
        }

        /**
         * Configures a GraphQL endpoint; sets {@link Protocol#GRAPHQL}.
         * The engine POSTs mutated query/variable payloads to
         * {@code baseUrl + graphQLPath}.
         */
        public Builder graphQLEndpoint(String baseUrl, String graphQLPath) {
            this.protocol = Protocol.GRAPHQL;
            this.method = "POST";
            this.baseUrl = baseUrl == null ? "" : baseUrl;
            this.graphQLPath = graphQLPath == null ? "/graphql" : graphQLPath;
            this.path = this.graphQLPath;
            return this;
        }

        /**
         * Configures a gRPC endpoint; sets {@link Protocol#GRPC}. The
         * engine dials {@code grpcAddress} (set via {@link #baseUrl})
         * with reflection and invokes {@code service/method}.
         */
        public Builder grpcEndpoint(String address, String service, String method) {
            this.protocol = Protocol.GRPC;
            this.baseUrl = address == null ? "" : address;
            this.grpcService = service;
            this.grpcMethod = method;
            this.method = method;
            return this;
        }

        public Builder header(String key, String value) {
            Objects.requireNonNull(key, "header key must not be null");
            Objects.requireNonNull(value, "header value must not be null");
            this.headers.put(key, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) this.headers.putAll(headers);
            return this;
        }

        /** Adds a single seed. Equivalent to {@link #seeds(Seed...)} with one arg. */
        public Builder seed(Seed seed) {
            Objects.requireNonNull(seed, "seed must not be null");
            this.seeds.add(seed);
            return this;
        }

        public Builder seeds(Seed... seeds) {
            Objects.requireNonNull(seeds, "seeds varargs must not be null");
            for (Seed s : seeds) seed(s);
            return this;
        }

        public Builder seeds(Iterable<Seed> seeds) {
            Objects.requireNonNull(seeds, "seeds iterable must not be null");
            for (Seed s : seeds) seed(s);
            return this;
        }

        public Builder mutator(Mutator mutator) {
            Objects.requireNonNull(mutator, "mutator must not be null");
            this.mutators.add(mutator);
            return this;
        }

        public Builder mutators(Mutator... mutators) {
            Objects.requireNonNull(mutators, "mutators varargs must not be null");
            for (Mutator m : mutators) mutator(m);
            return this;
        }

        public Builder assertion(Assertion assertion) {
            Objects.requireNonNull(assertion, "assertion must not be null");
            this.assertions.add(assertion);
            return this;
        }

        public Builder reporter(Reporter reporter) {
            Objects.requireNonNull(reporter, "reporter must not be null");
            this.reporters.add(reporter);
            return this;
        }

        public Builder duration(Duration duration) {
            Objects.requireNonNull(duration, "duration must not be null");
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("duration must be positive");
            }
            this.duration = duration;
            return this;
        }

        public Builder maxRequests(int maxRequests) {
            if (maxRequests < 0) {
                throw new IllegalArgumentException("maxRequests must be >= 0 (0 = engine default)");
            }
            this.maxRequests = maxRequests;
            return this;
        }

        public Builder concurrency(int concurrency) {
            if (concurrency < 0) {
                throw new IllegalArgumentException("concurrency must be >= 0 (0 = engine default)");
            }
            this.concurrency = concurrency;
            return this;
        }

        public Builder stopOnFinding(boolean stopOnFinding) {
            this.stopOnFinding = stopOnFinding;
            return this;
        }

        public Builder payloadCategory(String category) {
            Objects.requireNonNull(category, "category must not be null");
            this.payloadCategories.add(category);
            return this;
        }

        public Builder payloadCategories(String... categories) {
            Objects.requireNonNull(categories, "categories must not be null");
            for (String c : categories) payloadCategory(c);
            return this;
        }

        /**
         * Finalises the target. Validates required fields and freezes the
         * collected state into an immutable {@link Target}.
         */
        public Target build() {
            if (protocol == null) {
                throw new IllegalStateException(
                        "target '" + name + "' has no endpoint — call httpEndpoint(), graphQLEndpoint(), or grpcEndpoint()");
            }
            if (seeds.isEmpty() && payloadCategories.isEmpty()) {
                throw new IllegalStateException(
                        "target '" + name + "' has no seeds and no payload categories — engine has nothing to mutate");
            }
            if (mutators.isEmpty() && payloadCategories.isEmpty()) {
                throw new IllegalStateException(
                        "target '" + name + "' has no mutators and no payload categories — pick at least one");
            }
            return new Target(this);
        }

        private static String requireMethod(String method) {
            if (method == null || method.isBlank()) {
                throw new IllegalArgumentException("HTTP method must not be null or blank");
            }
            return method.toUpperCase();
        }
    }
}
