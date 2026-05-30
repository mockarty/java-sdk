// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.plugins;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Process-wide registry of {@link Plugin}s.
 *
 * <p>Two intake paths:</p>
 * <ol>
 *   <li>Classpath SPI discovery via {@link ServiceLoader} — the canonical
 *       way to surface a third-party plugin. Files under
 *       {@code META-INF/services/ru.mockarty.pact.plugins.Plugin} are picked
 *       up automatically the first time something queries the registry.</li>
 *   <li>{@link #register(Plugin)} — programmatic registration for tests,
 *       desktop apps, and embedding scenarios where the classpath layout
 *       doesn't carry a {@code META-INF/services} file (e.g. fat-jar
 *       relocation).</li>
 * </ol>
 *
 * <p>Thread-safe by construction: registration goes through a
 * {@link ConcurrentHashMap} keyed on lower-cased plugin name; the bootstrap
 * scan uses double-checked locking via {@link AtomicBoolean} so concurrent
 * callers don't double-load the SPI providers.</p>
 *
 * <p>This is a singleton-by-convention but exposes a {@link #fresh()}
 * factory so unit tests can run with an isolated registry (the global
 * {@link #global()} instance is mutated by other tests in the same JVM,
 * which is fine for production code but undesirable for race tests that
 * count exact entries).</p>
 */
public final class PluginRegistry {

    private static final PluginRegistry GLOBAL = new PluginRegistry();

    private final Map<String, Plugin> byName = new ConcurrentHashMap<>();
    private final AtomicBoolean bootstrapped = new AtomicBoolean(false);

    private PluginRegistry() {}

    /** Returns the JVM-wide registry. Loads SPI providers on first call. */
    public static PluginRegistry global() {
        GLOBAL.ensureBootstrapped();
        return GLOBAL;
    }

    /** Returns a registry with no plugins pre-loaded. Useful for tests
     *  that want to verify behaviour in isolation. */
    public static PluginRegistry fresh() {
        return new PluginRegistry();
    }

    /** Register (or replace) a plugin under its canonical name. */
    public PluginRegistry register(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        String name = plugin.name();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Plugin.name() must not be null or blank");
        }
        byName.put(name.toLowerCase(Locale.ROOT), plugin);
        return this;
    }

    /** Look up a plugin by its canonical name (case-insensitive). */
    public Optional<Plugin> get(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    /** Immutable snapshot of the current plugin set. */
    public Collection<Plugin> all() {
        return Collections.unmodifiableCollection(new LinkedHashMap<>(byName).values());
    }

    /** Number of currently-registered plugins (post-bootstrap). */
    public int size() {
        return byName.size();
    }

    /** Clear all registered plugins. Reset only — also drops the SPI scan
     *  flag so the next {@link #global()} call re-discovers providers. */
    public PluginRegistry clear() {
        byName.clear();
        bootstrapped.set(false);
        return this;
    }

    // ── SPI discovery ───────────────────────────────────────────────────

    private void ensureBootstrapped() {
        // Cheap double-check: the AtomicBoolean toggles exactly once per
        // registry lifetime, so the synchronized block runs at most once.
        if (bootstrapped.compareAndSet(false, true)) {
            // Always seed the two built-ins so callers that don't ship a
            // META-INF/services file still get the canonical pact-V4
            // plugin set out-of-the-box.
            byName.putIfAbsent(ProtobufPlugin.NAME, new ProtobufPlugin());
            byName.putIfAbsent(GRPCPlugin.NAME, new GRPCPlugin());
            // Then layer any classpath-provided plugins on top — they win
            // over the built-ins, allowing a third-party to override.
            for (Plugin p : ServiceLoader.load(Plugin.class)) {
                if (p == null) continue;
                String n = p.name();
                if (n == null || n.isBlank()) continue;
                byName.put(n.toLowerCase(Locale.ROOT), p);
            }
        }
    }
}
