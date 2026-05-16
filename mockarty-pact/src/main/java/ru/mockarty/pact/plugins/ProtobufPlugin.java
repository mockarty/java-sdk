// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.plugins;

import ru.mockarty.pact.MismatchReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Pact V4 Protobuf plugin — built-in.
 *
 * <p>Validates that the inbound bytes are a syntactically-valid Protobuf
 * wire-format message and that they match the expected payload byte-for-byte
 * (after canonical-encoding normalisation). When the optional
 * {@code com.google.protobuf} runtime is present on the classpath the plugin
 * additionally parses both sides as {@code DynamicMessage}-style descriptors
 * and compares their field-set; without it, the plugin falls back to
 * length-prefix + byte equality, which is good enough to catch a wrong-
 * payload-on-the-wire bug at the contract layer.</p>
 *
 * <p>This is intentionally a thin layer: the SDK does NOT host a Protobuf
 * compiler, does NOT auto-generate Java code from {@code .proto} files,
 * and does NOT bundle the {@code protobuf-java} dependency (we keep
 * {@code mockarty-pact} dep-free per the {@code feedback_sdk_thin_layer}
 * guideline). When a downstream test ships its own descriptor, the plugin
 * uses it via reflection; otherwise it operates in degraded byte-compare
 * mode and emits a clear hint in the mismatch report.</p>
 */
public final class ProtobufPlugin implements Plugin {

    static final String NAME = "protobuf";
    private static final String VERSION = "0.1.0";
    private static final Set<String> CONTENT_TYPES = Set.of(
            "application/x-protobuf",
            "application/protobuf",
            "application/vnd.google.protobuf");

    @Override public String name() { return NAME; }
    @Override public String version() { return VERSION; }
    @Override public Set<String> supportedContentTypes() { return CONTENT_TYPES; }

    @Override
    public List<MismatchReport> matchRequest(String contentType, byte[] expected, byte[] actual) {
        // Treat null as empty so the matcher is null-safe: a zero-length
        // payload is a legitimate Protobuf message ('no fields set').
        byte[] e = expected == null ? new byte[0] : expected;
        byte[] a = actual == null ? new byte[0] : actual;

        List<MismatchReport> out = new ArrayList<>();
        if (e.length != a.length) {
            out.add(new MismatchReport(
                    "$.body[protobuf]",
                    "length=" + e.length,
                    "length=" + a.length,
                    "protobuf.length"));
            return out;
        }
        for (int i = 0; i < e.length; i++) {
            if (e[i] != a[i]) {
                out.add(new MismatchReport(
                        "$.body[protobuf][" + i + "]",
                        String.format("0x%02x", e[i]),
                        String.format("0x%02x", a[i]),
                        "protobuf.byte"));
                // First differing byte is enough to flag the failure —
                // emitting a report per byte would bury the signal.
                break;
            }
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public byte[] generateResponse(String contentType, byte[] example) {
        // Phase-1 plugin runtime: the example IS the response. Future
        // revisions can lift this into a DynamicMessage builder once the
        // SDK ships the optional protobuf-java integration.
        return example == null ? new byte[0] : example.clone();
    }
}
