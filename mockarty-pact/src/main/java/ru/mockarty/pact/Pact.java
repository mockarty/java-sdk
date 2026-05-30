// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import ru.mockarty.pact.plugins.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable Pact contract — consumer name, provider name, interactions,
 * metadata. Created via {@link Consumer#build()}.
 *
 * <p>Knows how to serialise itself to a versioned pact.json on disk (and
 * exposes the JSON as a string for tests / brokers that want bytes).</p>
 */
public final class Pact {

    private final String consumer;
    private final String provider;
    private final SpecVersion specVersion;
    private final List<Interaction> interactions;
    private final List<String> plugins;
    private final Map<String, String> pluginVersions;
    private final List<Plugin> resolvedPlugins;
    private final Map<String, Map<String, Object>> pluginConfigs;
    private final Path outputDir;

    Pact(
            String consumer,
            String provider,
            SpecVersion specVersion,
            List<Interaction> interactions,
            List<String> plugins,
            Map<String, String> pluginVersions,
            List<Plugin> resolvedPlugins,
            Map<String, Map<String, Object>> pluginConfigs,
            Path outputDir) {
        this.consumer = Objects.requireNonNull(consumer, "consumer must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.specVersion = Objects.requireNonNull(specVersion, "specVersion must not be null");
        this.interactions = Collections.unmodifiableList(interactions);
        this.plugins = Collections.unmodifiableList(plugins);
        this.pluginVersions = Collections.unmodifiableMap(
                pluginVersions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(pluginVersions));
        this.resolvedPlugins = Collections.unmodifiableList(
                resolvedPlugins == null ? java.util.List.of() : new java.util.ArrayList<>(resolvedPlugins));
        this.pluginConfigs = Collections.unmodifiableMap(
                pluginConfigs == null ? new LinkedHashMap<>() : new LinkedHashMap<>(pluginConfigs));
        this.outputDir = outputDir;
    }

    public String consumer() { return consumer; }
    public String provider() { return provider; }
    public SpecVersion specVersion() { return specVersion; }
    public List<Interaction> interactions() { return interactions; }
    /** Plugin names declared on the consumer side (V4 only). */
    public List<String> plugins() { return plugins; }
    /** Plugin → version map (the version emitted into metadata.plugins). */
    public Map<String, String> pluginVersions() { return pluginVersions; }
    /** Plugin instances resolved through {@code PluginRegistry} — only the
     *  plugins actually known to this JVM. May be smaller than {@link #plugins()}. */
    public List<Plugin> resolvedPlugins() { return resolvedPlugins; }
    /** Free-form per-plugin config map (opaque to the SDK; consumed by the plugin itself). */
    public Map<String, Map<String, Object>> pluginConfigs() { return pluginConfigs; }
    public Path outputDir() { return outputDir; }

    /** Serialise the pact contract as JSON. */
    public String toJson() {
        return PactWriter.write(this);
    }

    /** Write {@code <consumer>-<provider>.json} into {@link #outputDir()}. */
    public Path writeToFile() throws IOException {
        if (outputDir == null) {
            throw new IllegalStateException(
                    "writeToFile: no outputDir configured — call Consumer.outputDir(...) first");
        }
        Files.createDirectories(outputDir);
        String filename = sanitize(consumer) + "-" + sanitize(provider) + ".json";
        Path target = outputDir.resolve(filename);
        Files.writeString(target, toJson(), StandardCharsets.UTF_8);
        return target;
    }

    private static String sanitize(String s) {
        // Pact-jvm uses snake_case lower for filenames; we replicate the
        // pattern so brokers / file-watchers built around the conventional
        // layout pick the file up without reconfiguration.
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
    }
}
