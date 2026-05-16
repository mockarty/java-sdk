// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Top-level consumer-side Pact DSL entry point.
 *
 * <p>Usage:</p>
 * <pre>
 * Pact pact = Consumer.named("OrderService")
 *     .withProvider("PaymentService")
 *     .specVersion(SpecVersion.V4)
 *     .outputDir(Path.of("./pacts"))
 *     .addInteraction(it -&gt; it
 *         .given("payment service is up")
 *         .uponReceiving("a charge request")
 *         .withRequest("POST", "/charge")
 *         .withHeader("Content-Type", "application/json")
 *         .withJsonBody(Map.of("amount", Matchers.like(100)))
 *         .willRespondWith(200)
 *         .withJsonBody(Map.of("id", Matchers.like("abc"))))
 *     .build();
 * </pre>
 */
public final class Consumer {

    private final String consumerName;
    private String providerName;
    private SpecVersion specVersion = SpecVersion.V4;
    private Path outputDir;
    private final List<Interaction> interactions = new ArrayList<>();
    private final Set<String> plugins = new LinkedHashSet<>();

    private Consumer(String consumerName) {
        this.consumerName = consumerName;
    }

    /** Begin building a pact contract for the given consumer name. */
    public static Consumer named(String consumerName) {
        Objects.requireNonNull(consumerName, "consumer name must not be null");
        if (consumerName.isBlank()) {
            throw new IllegalArgumentException("consumer name must not be blank");
        }
        return new Consumer(consumerName);
    }

    /** Declare the provider this consumer talks to. */
    public Consumer withProvider(String providerName) {
        Objects.requireNonNull(providerName, "provider name must not be null");
        if (providerName.isBlank()) {
            throw new IllegalArgumentException("provider name must not be blank");
        }
        this.providerName = providerName;
        return this;
    }

    /** Pin the Pact spec version. Default {@link SpecVersion#V4}. */
    public Consumer specVersion(SpecVersion v) {
        Objects.requireNonNull(v, "specVersion must not be null");
        this.specVersion = v;
        return this;
    }

    /** Directory where {@link Pact#writeToFile()} drops pact.json. */
    public Consumer outputDir(Path dir) {
        this.outputDir = dir;
        return this;
    }

    /**
     * Register a V4 plugin on this contract.
     *
     * <p>Phase-1 limitation: the plugin name is recorded in the pact.json
     * metadata only — the SDK does not yet provide a runtime for plugin
     * behaviour (HTTP/2 framing, async messaging, MQ adapters). The
     * {@link PactWriter} fails loud if a user calls {@code withPlugin}
     * under {@link SpecVersion#V3}.</p>
     */
    public Consumer withPlugin(String name) {
        Objects.requireNonNull(name, "plugin name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("plugin name must not be blank");
        }
        this.plugins.add(name);
        return this;
    }

    /** Add an interaction declared via a fluent builder lambda. */
    public Consumer addInteraction(java.util.function.Consumer<InteractionBuilder> spec) {
        Objects.requireNonNull(spec, "interaction spec must not be null");
        InteractionBuilder b = new InteractionBuilder();
        spec.accept(b);
        interactions.add(b.build());
        return this;
    }

    /** Finalise into an immutable {@link Pact}. */
    public Pact build() {
        if (providerName == null) {
            throw new IllegalStateException("Consumer.build: withProvider(...) is required");
        }
        if (interactions.isEmpty()) {
            throw new IllegalStateException(
                    "Consumer.build: at least one addInteraction(...) call is required");
        }
        if (!plugins.isEmpty() && specVersion == SpecVersion.V3) {
            throw new IllegalStateException(
                    "Plugins (" + plugins + ") are V4-only — cannot serialise under SpecVersion.V3. "
                            + "Switch to specVersion(SpecVersion.V4) or drop the plugin declaration.");
        }
        return new Pact(
                consumerName,
                providerName,
                specVersion,
                new ArrayList<>(interactions),
                new ArrayList<>(plugins),
                outputDir);
    }

    // ── Accessors used by adjacent test fixtures ─────────────────────

    public String consumerName() { return consumerName; }
    public String providerName() { return providerName; }
    public SpecVersion currentSpecVersion() { return specVersion; }
    public Path currentOutputDir() { return outputDir; }
}
