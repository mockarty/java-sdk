// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.plugins;

import ru.mockarty.pact.MismatchReport;

import java.util.List;
import java.util.Set;

/**
 * Pact V4 plugin SPI — a strategy interface that lets a contract carry
 * payload semantics beyond plain JSON / XML (Protobuf descriptors, gRPC
 * framing, MQ envelopes, etc.).
 *
 * <p>The SDK loads plugins from the JVM classpath via
 * {@link java.util.ServiceLoader} ({@code META-INF/services/
 * ru.mockarty.pact.plugins.Plugin}) and exposes them through
 * {@link PluginRegistry}. The bundled implementations
 * ({@link ProtobufPlugin}, {@link GRPCPlugin}) are degraded-by-default —
 * they validate payload structure when descriptors are available but
 * never start a real network listener or pull in optional gRPC deps.</p>
 *
 * <p>This is intentionally a small, thin contract. The SDK never
 * embeds a full gRPC/Protobuf runtime (the {@code feedback_sdk_thin_layer}
 * guideline): plugins extend matching surface, they don't bring up
 * stand-alone servers.</p>
 */
public interface Plugin {

    /** Canonical plugin name as it appears in {@code metadata.plugins[].name}. */
    String name();

    /** Plugin version recorded in {@code metadata.plugins[].version}. */
    String version();

    /** Lower-cased content types this plugin claims to understand
     *  ({@code application/grpc}, {@code application/x-protobuf}, etc.). */
    Set<String> supportedContentTypes();

    /**
     * Validate an inbound payload against the consumer-declared expectation.
     *
     * @param contentType wire content-type from the request headers.
     * @param expected    the example payload registered in the pact.
     * @param actual      the bytes that just arrived.
     * @return empty list when the payload matches, otherwise a structured
     *         list of {@link MismatchReport} entries describing the failure
     *         (one entry per logical mismatch).
     */
    List<MismatchReport> matchRequest(String contentType, byte[] expected, byte[] actual);

    /**
     * Produce the bytes that the mock server should write back to the
     * caller when this plugin owns the response. Defaults to echoing the
     * declared example — implementations can substitute richer logic
     * (e.g. proto-serialise a Java object).
     */
    default byte[] generateResponse(String contentType, byte[] example) {
        return example == null ? new byte[0] : example;
    }
}
