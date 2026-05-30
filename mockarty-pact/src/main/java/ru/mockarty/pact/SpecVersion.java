// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

/**
 * Pact specification version this consumer test should target.
 *
 * <p>V3 (legacy) and V4 (plugin-aware) are both supported as first-class
 * citizens. The selected version drives:</p>
 * <ul>
 *   <li>Pact JSON output shape (metadata.pactSpecification.version).</li>
 *   <li>Matching-rule serialisation layout (flat path keys in V3,
 *       nested object tree in V4).</li>
 *   <li>Provider-state shape ({@code providerState} string in V3,
 *       {@code providerStates} array with {@code name}+{@code params}
 *       in V4).</li>
 *   <li>V4-only constructs (plugins block, generators block, interaction
 *       {@code type} discriminator).</li>
 * </ul>
 *
 * <p>Default is {@link #V4}. The selector is exposed through
 * {@link Consumer#specVersion(SpecVersion)} on the fluent builder.</p>
 */
public enum SpecVersion {
    /** Legacy Pact V3 — widely supported by older brokers and pact-jvm. */
    V3("3.0.0"),
    /** Modern Pact V4 — required for plugins, generators, async/message
     * interactions. New tests should prefer V4 unless their broker is
     * pinned to V3. */
    V4("4.0");

    private final String wire;

    SpecVersion(String wire) {
        this.wire = wire;
    }

    /** Returns the spec version string as it appears in the
     * {@code metadata.pactSpecification.version} field of pact.json. */
    public String wire() {
        return wire;
    }
}
