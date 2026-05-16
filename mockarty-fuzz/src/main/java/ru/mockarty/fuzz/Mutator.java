// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A named mutation strategy that the server-side fuzz engine should apply
 * to the seed corpus when running the target.
 *
 * <p>The SDK does NOT implement mutators in Java — they live in
 * {@code internal/fuzzing/engine_mutation.go}. The DSL simply forwards the
 * mutator name (plus an optional JS-blob config for user-defined mutators)
 * into the canonical JSON config; the engine looks it up in its own
 * registry. That keeps the SDK a thin layer and lets owners ship new
 * mutators server-side without recutting the SDK.</p>
 *
 * <p>Built-in mutators are exposed as static constants:
 * {@link #JSON}, {@link #XML}, {@link #BYTES}, {@link #STRING},
 * {@link #URL}, {@link #HEADER}, {@link #GRPC}, {@link #GRAPHQL}.</p>
 *
 * <p>Custom mutators are registered via
 * {@link #custom(String, Map)} — the {@code config} map is serialised
 * verbatim into the JSON and forwarded to the engine's user-mutator
 * loader. Per {@code feedback_dynamic_over_hardcode.md}, this avoids a
 * giant switch on a hard-coded string set: the SDK doesn't enumerate the
 * legal mutator names, the engine does.</p>
 */
public final class Mutator {

    /** JSON body mutator: type confusion, boundary swaps, null/missing keys. */
    public static final Mutator JSON = new Mutator("json", Map.of());
    /** XML body mutator: XXE, entity expansion, malformed nesting. */
    public static final Mutator XML = new Mutator("xml", Map.of());
    /** Raw byte mutator: bit flips, byte inserts, segment shuffles. */
    public static final Mutator BYTES = new Mutator("bytes", Map.of());
    /** String mutator: encoding attacks, unicode, format strings, BOM. */
    public static final Mutator STRING = new Mutator("string", Map.of());
    /** URL mutator: path traversal, open-redirect, SSRF probes. */
    public static final Mutator URL = new Mutator("url", Map.of());
    /** HTTP header mutator: smuggling, oversize values, control chars. */
    public static final Mutator HEADER = new Mutator("header", Map.of());
    /** gRPC payload mutator: protobuf field shuffles, oneof confusion. */
    public static final Mutator GRPC = new Mutator("grpc", Map.of());
    /** GraphQL mutator: introspection abuse, deep query nesting, alias spam. */
    public static final Mutator GRAPHQL = new Mutator("graphql", Map.of());

    private final String name;
    private final Map<String, Object> config;

    private Mutator(String name, Map<String, Object> config) {
        this.name = Objects.requireNonNull(name, "mutator name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("mutator name must not be blank");
        }
        // Defensive copy + LinkedHashMap for stable JSON key order — makes
        // golden-file tests reproducible.
        this.config = config == null || config.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(config));
    }

    /**
     * Registers a custom mutator that the server-side engine will resolve
     * by name. {@code config} is a free-form map serialised verbatim into
     * the JSON config — its shape is owned by the engine's user-mutator
     * loader (typically a JS script reference + arguments).
     *
     * @param name   non-blank identifier the engine will look up
     * @param config optional config blob (may be null or empty)
     * @return a Mutator instance ready to be added to a {@link Target}
     */
    public static Mutator custom(String name, Map<String, Object> config) {
        return new Mutator(name, config);
    }

    /** Convenience overload for a custom mutator with no config blob. */
    public static Mutator custom(String name) {
        return new Mutator(name, Map.of());
    }

    /** Returns the wire identifier the engine will resolve. */
    public String name() {
        return name;
    }

    /** Returns the (possibly empty) config map serialised into the JSON. */
    public Map<String, Object> config() {
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mutator other)) return false;
        return name.equals(other.name) && config.equals(other.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, config);
    }

    @Override
    public String toString() {
        return config.isEmpty() ? "Mutator{" + name + "}"
                : "Mutator{" + name + ", config=" + config + "}";
    }
}
