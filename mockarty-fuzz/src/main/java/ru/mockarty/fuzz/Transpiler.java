// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a {@link Target} into Mockarty's canonical fuzz JSON config.
 *
 * <p>Schema parity with {@code internal/fuzzing/config.go FuzzConfig} +
 * {@code FuzzOptions}: top-level fields {@code name, namespace, sourceType,
 * targetBaseUrl, strategy, seedRequests, options} map straight onto the
 * existing server model. Mutators/assertions/reporters live under
 * {@code options} as {@code mutationTypes}, {@code assertions},
 * {@code reporters}. The {@code FuzzingApi.createConfig} client method
 * round-trips the same shape, so users can submit the emitted JSON either
 * through the SDK or via raw HTTP / the CLI without translation.</p>
 *
 * <p>This class has no instance state — all methods are static — and is
 * thread-safe.</p>
 */
public final class Transpiler {

    /**
     * Shared mapper; INDENT_OUTPUT is OFF for compact wire payloads.
     * {@link Target#writeTo} uses a pretty mapper instead for diff-friendly
     * on-disk files.
     */
    private static final ObjectMapper COMPACT = new ObjectMapper();
    private static final ObjectMapper PRETTY = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private Transpiler() {}

    /**
     * Compact (single-line, no whitespace) JSON form. This is what
     * {@link Runner#submit} sends over the wire.
     */
    public static String toJson(Target target) {
        return serialise(target, COMPACT);
    }

    /**
     * Pretty-printed JSON form. Used by {@link Target#writeTo} so the
     * committed file is diff-friendly.
     */
    public static String toPrettyJson(Target target) {
        return serialise(target, PRETTY);
    }

    /**
     * Returns the raw map-of-map structure the JSON serialises to. Handy
     * for golden-file unit tests that want to assert on individual fields
     * without parsing the emitted JSON back.
     */
    public static Map<String, Object> toMap(Target target) {
        return buildPayload(target);
    }

    // ── Internals ────────────────────────────────────────────────────

    private static String serialise(Target target, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(buildPayload(target));
        } catch (JsonProcessingException e) {
            // Defensive — the Map<String,Object> we hand Jackson contains
            // only types it natively understands; failures here would be
            // a programmer error inside this module.
            throw new IllegalStateException("failed to serialise fuzz target", e);
        }
    }

    private static Map<String, Object> buildPayload(Target target) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", target.name());
        if (!target.description().isEmpty()) {
            root.put("description", target.description());
        }
        if (!target.namespace().isEmpty()) {
            root.put("namespace", target.namespace());
        }

        // Canonical FuzzConfig source-type for SDK-emitted configs. The
        // engine uses this to distinguish from openapi/curl/etc imports
        // when picking generators (config.go SourceManual constant).
        root.put("sourceType", "manual");

        // Strategy default = "all" matches FuzzConfig.Strategy when the
        // SDK leaves it implicit. Users who want narrower coverage flip
        // mutators and let the engine combine those into a custom strategy.
        root.put("strategy", "all");

        root.put("protocol", target.protocol().wire());
        root.put("method", target.method() == null ? "" : target.method());
        if (!target.baseUrl().isEmpty()) {
            root.put("targetBaseUrl", target.baseUrl());
        }
        if (!target.path().isEmpty()) {
            root.put("path", target.path());
        }
        if (!target.headers().isEmpty()) {
            root.put("headers", target.headers());
        }

        // Protocol-specific extras under the same option keys the engine
        // already understands (FuzzOptions.GRPCAddress, GraphQLPath etc).
        if (target.protocol() == Protocol.GRPC) {
            if (target.grpcService() != null) root.put("grpcService", target.grpcService());
            if (target.grpcMethod() != null) root.put("grpcMethod", target.grpcMethod());
        }
        if (target.protocol() == Protocol.GRAPHQL && target.graphQLPath() != null) {
            root.put("graphqlPath", target.graphQLPath());
        }

        // Seeds: each one becomes a FuzzSeedRequest-shaped object. Binary
        // seeds carry a base64 sibling key so the engine can reconstruct
        // the exact byte sequence without ambiguity.
        List<Map<String, Object>> seedJson = new ArrayList<>(target.seeds().size());
        for (Seed s : target.seeds()) {
            Map<String, Object> seed = new LinkedHashMap<>();
            seed.put("id", s.name());
            seed.put("method", target.method() == null ? "" : target.method());
            seed.put("path", target.path());
            if (s.isText()) {
                seed.put("body", s.text());
            } else {
                seed.put("bytesBase64", s.base64());
            }
            seedJson.add(seed);
        }
        root.put("seedRequests", seedJson);

        // payloadCategories: forwarded verbatim to the engine's category
        // selector (config.go FuzzConfig.PayloadCategories field).
        if (!target.payloadCategories().isEmpty()) {
            root.put("payloadCategories", target.payloadCategories());
        }

        root.put("options", buildOptions(target));
        return root;
    }

    private static Map<String, Object> buildOptions(Target target) {
        Map<String, Object> opts = new LinkedHashMap<>();
        if (target.duration() != null) {
            opts.put("maxDuration", formatDuration(target.duration()));
        }
        if (target.maxRequests() > 0) {
            opts.put("maxRequests", target.maxRequests());
        }
        if (target.concurrency() > 0) {
            opts.put("concurrency", target.concurrency());
        }
        if (target.stopOnFinding()) {
            opts.put("stopOnCritical", true);
        }

        // Mutators serialise as a list of {name, config} objects so the
        // engine can dispatch through its registry (dynamic-over-hardcode).
        if (!target.mutators().isEmpty()) {
            List<Map<String, Object>> mutators = new ArrayList<>(target.mutators().size());
            List<String> mutationTypes = new ArrayList<>(target.mutators().size());
            for (Mutator m : target.mutators()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", m.name());
                if (!m.config().isEmpty()) {
                    entry.put("config", m.config());
                }
                mutators.add(entry);
                mutationTypes.add(m.name());
            }
            // FuzzOptions.MutationTypes is the legacy flat list — kept
            // alongside the richer `mutators` array so older runner
            // builds that haven't picked up the rich shape still work.
            opts.put("mutationTypes", mutationTypes);
            opts.put("mutators", mutators);
        }

        // Assertions dispatch on the sealed-type, NOT on a string switch.
        if (!target.assertions().isEmpty()) {
            List<Map<String, Object>> assertions = new ArrayList<>(target.assertions().size());
            for (Assertion a : target.assertions()) {
                assertions.add(assertionToMap(a));
            }
            opts.put("assertions", assertions);
        }

        if (!target.reporters().isEmpty()) {
            List<String> reporters = new ArrayList<>(target.reporters().size());
            for (Reporter r : target.reporters()) reporters.add(r.wire());
            opts.put("reporters", reporters);
        }

        return opts;
    }

    private static Map<String, Object> assertionToMap(Assertion a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", a.type());
        // Pattern matching on the sealed type — new variants force a
        // compile error here (the switch is exhaustive thanks to the
        // permits clause). That's the dynamic-over-hardcode pattern in
        // action: no string lookup, no fallthrough default.
        switch (a) {
            case Assertion.Status s -> {
                m.put("min", s.min());
                m.put("max", s.max());
            }
            case Assertion.NoCrash ignored -> {
                // no parameters
            }
            case Assertion.ResponseTimeUnder r -> m.put("limitMs", r.limit().toMillis());
            case Assertion.NoErrorInBody e -> m.put("errorTokens", e.errorTokens());
        }
        return m;
    }

    /**
     * Renders {@code Duration} as a Go-compatible duration string
     * (e.g. "5m", "30s", "1h30m") because the engine parses the
     * {@code maxDuration} field with {@code time.ParseDuration} on the
     * Go side. Sub-second precision is dropped — the engine doesn't
     * accept it for {@code maxDuration} anyway.
     */
    static String formatDuration(Duration d) {
        long totalSeconds = d.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString();
    }
}
