package ru.mockarty.protocols.telemetry;

import java.nio.charset.StandardCharsets;

/**
 * Static helpers shared by every protocol client in this SDK.
 *
 * <p>Kept as a tiny utility class so the public surface is one
 * import — {@code import ru.mockarty.protocols.telemetry.Telemetry;}
 * — for tests that want to build {@link Step} keys without dragging
 * the protocol clients themselves into scope.
 */
public final class Telemetry {

    private Telemetry() {
    }

    /**
     * Build a stable per-call step key. Format matches the Go SDK
     * ({@code <name>#<seq>}) so cross-language test suites collapse
     * on the same key server-side via the (namespace, run, step_key)
     * unique index.
     */
    public static String newStepKey(String name, long seq) {
        return name + "#" + seq;
    }

    /**
     * Truncate a payload preview for a step's {@code parameters} map.
     * The cap is measured in <b>UTF-8 bytes</b> (not Java chars) so the
     * preview shape matches the Go and Python SDKs byte-for-byte. When
     * the cap would land mid-codepoint we walk backwards to the last
     * valid UTF-8 lead byte, then report the original-vs-truncated byte
     * count in the marker — same format every language ships.
     *
     * <p>Returns empty string when {@code cap == 0}; negative caps clamp
     * to zero.
     */
    public static String capPreview(String body, int cap) {
        if (body == null) {
            return "";
        }
        return capPreview(body.getBytes(StandardCharsets.UTF_8), cap);
    }

    /**
     * Byte-aware overload — pass raw UTF-8 bytes directly. Same UTF-8
     * boundary semantics as the {@link #capPreview(String, int) String}
     * variant; mirrors the Go {@code telemetry.CapPreview([]byte, int)}
     * signature so cross-language test suites produce identical previews.
     */
    public static String capPreview(byte[] body, int cap) {
        if (body == null || cap <= 0) {
            return "";
        }
        if (body.length <= cap) {
            return new String(body, StandardCharsets.UTF_8);
        }
        // Slide back while we're on a UTF-8 continuation byte (0x80..0xBF)
        // so we never slice through the middle of a multi-byte codepoint.
        int safe = cap;
        while (safe > 0 && (body[safe] & 0xC0) == 0x80) {
            safe--;
        }
        String head = new String(body, 0, safe, StandardCharsets.UTF_8);
        return head + "…(truncated " + (body.length - safe) + "B)";
    }
}
