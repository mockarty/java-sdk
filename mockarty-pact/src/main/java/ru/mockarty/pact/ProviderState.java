// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pact provider-state declaration.
 *
 * <p>In Pact V3 this serialises as a single string under {@code providerState};
 * in V4 it serialises into the {@code providerStates} array with
 * {@code name} + {@code params}. Parameters are V4-only — if a test passes
 * them under V3 the writer raises a loud error.</p>
 */
public final class ProviderState {

    private final String name;
    private final Map<String, Object> params;

    private ProviderState(String name, Map<String, Object> params) {
        this.name = Objects.requireNonNull(name, "providerState name must not be null");
        this.params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    /** Pure state name (V3 + V4). */
    public static ProviderState of(String name) {
        return new ProviderState(name, Map.of());
    }

    /** State name with parameters (V4 only). */
    public static ProviderState of(String name, Map<String, Object> params) {
        return new ProviderState(name, params);
    }

    public String name() { return name; }
    public Map<String, Object> params() { return params; }
}
