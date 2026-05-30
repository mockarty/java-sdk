// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz;

/**
 * Wire-level protocol the fuzz target speaks.
 *
 * <p>Each constant maps 1:1 to Mockarty's server-side fuzz engine
 * dispatcher (see {@code internal/fuzzing/engine.go} — the {@code Strategy}
 * field selection on {@code FuzzConfig} carries the protocol implicitly,
 * but the SDK keeps it explicit so the user picks one builder shape and
 * the transpiler can validate seed correctness up-front).</p>
 *
 * <p>{@link #HTTP} covers REST + SOAP + GraphQL-over-HTTP; the latter two
 * have richer builders ({@link Target.Builder#graphQLEndpoint},
 * {@link Target.Builder#grpcEndpoint}) when their protocol-specific
 * shape is needed.</p>
 */
public enum Protocol {
    /** Plain HTTP/1.1 + HTTP/2 — REST, JSON-RPC, raw payloads. */
    HTTP("http"),
    /** GraphQL over HTTP — query + variables + operation name. */
    GRAPHQL("graphql"),
    /** gRPC unary or streaming — service + method + reflection address. */
    GRPC("grpc");

    private final String wire;

    Protocol(String wire) {
        this.wire = wire;
    }

    /**
     * Returns the canonical lowercase identifier emitted into the
     * transpiled JSON config (consumed by Mockarty's fuzz engine and the
     * mockarty-cli {@code fuzz run} subcommand).
     */
    public String wire() {
        return wire;
    }
}
