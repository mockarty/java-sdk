// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * A single seed input for the fuzz corpus.
 *
 * <p>The seed has a stable name (used by the engine for finding-grouping
 * and reproducer naming) plus a payload — either a UTF-8 string (treated
 * as the request body verbatim) or a raw byte array (base64-encoded into
 * the JSON config under {@code bytesBase64} so the engine can reconstruct
 * a non-UTF8 payload exactly).</p>
 *
 * <p>Use {@link #of(String, String)} for textual payloads,
 * {@link #bytes(String, byte[])} for binary, and
 * {@link #fromFile(String, Path)} to pull the payload from disk at build
 * time (resolved eagerly so the JSON config is self-contained).</p>
 */
public final class Seed {

    private final String name;
    private final String payload;     // UTF-8 string payload, null if binary
    private final byte[] rawBytes;    // non-null iff payload is null

    private Seed(String name, String payload, byte[] rawBytes) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("seed name must not be null or blank");
        }
        // Exactly one of payload / rawBytes must be set. The empty-string
        // payload IS a valid seed (zero-length body); empty-bytes is too.
        if (payload == null && rawBytes == null) {
            throw new IllegalArgumentException("seed payload must not be null");
        }
        if (payload != null && rawBytes != null) {
            throw new IllegalArgumentException(
                    "seed has both string and bytes payload set — pick one");
        }
        this.name = name;
        this.payload = payload;
        this.rawBytes = rawBytes == null ? null : rawBytes.clone();
    }

    /**
     * Creates a UTF-8 string seed. The payload becomes the request body
     * verbatim (engine does no encoding transformation).
     */
    public static Seed of(String name, String payload) {
        Objects.requireNonNull(payload, "payload must not be null — use bytes() for raw payloads");
        return new Seed(name, payload, null);
    }

    /**
     * Creates a binary seed; the bytes are base64-encoded into the JSON
     * config and the engine decodes them back. Use this for protobuf,
     * binary uploads, or arbitrary blobs.
     */
    public static Seed bytes(String name, byte[] payload) {
        Objects.requireNonNull(payload, "payload bytes must not be null");
        return new Seed(name, null, payload);
    }

    /**
     * Reads the file at {@code path} eagerly and turns it into a seed.
     * Binary files become byte seeds; we always treat file content as
     * binary because we have no way of knowing the original encoding.
     */
    public static Seed fromFile(String name, Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        byte[] data = Files.readAllBytes(path);
        return new Seed(name, null, data);
    }

    /** Seed identifier (stable across runs, surfaced in findings). */
    public String name() {
        return name;
    }

    /** True if this seed holds a UTF-8 string payload (not raw bytes). */
    public boolean isText() {
        return payload != null;
    }

    /**
     * Returns the textual payload. Only legal when {@link #isText()} is
     * true — call {@link #bytes()} instead for binary seeds.
     */
    public String text() {
        if (payload == null) {
            throw new IllegalStateException(
                    "seed '" + name + "' is binary — use bytes() instead of text()");
        }
        return payload;
    }

    /**
     * Returns a defensive copy of the binary payload. For text seeds this
     * returns the UTF-8 encoding of {@link #text()}, so callers always
     * get the raw bytes the engine will mutate.
     */
    public byte[] bytes() {
        if (rawBytes != null) return rawBytes.clone();
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns a base64 encoding of the binary payload — used by the
     * transpiler when emitting non-UTF8 seeds into the JSON config.
     */
    String base64() {
        return Base64.getEncoder().encodeToString(rawBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seed s)) return false;
        return name.equals(s.name)
                && Objects.equals(payload, s.payload)
                && Arrays.equals(rawBytes, s.rawBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, payload, Arrays.hashCode(rawBytes));
    }

    @Override
    public String toString() {
        return "Seed{name='" + name + "', " + (isText()
                ? "text=" + payload.length() + " chars"
                : "bytes=" + rawBytes.length) + "}";
    }
}
