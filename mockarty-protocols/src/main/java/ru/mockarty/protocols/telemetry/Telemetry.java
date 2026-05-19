package ru.mockarty.protocols.telemetry;

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

    /** Truncate a payload preview for a step's {@code parameters}
     *  map. Returns empty string when {@code cap == 0}; mirrors the
     *  Go/Python SDK behaviour so previews look identical across
     *  languages. */
    public static String capPreview(String body, int cap) {
        if (body == null || cap == 0) {
            return "";
        }
        if (body.length() <= cap) {
            return body;
        }
        return body.substring(0, cap) + "…(truncated " + (body.length() - cap) + "B)";
    }
}
