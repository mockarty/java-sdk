package ru.mockarty.protocols.telemetry;

/**
 * Drops every recorded step. Protocol clients use this as the
 * default when no recorder is wired so scripts that don't care
 * about TCM step capture work out of the box without extra setup.
 */
public final class NopRecorder implements StepRecorder {

    /** Singleton instance — the recorder is stateless. */
    public static final NopRecorder INSTANCE = new NopRecorder();

    private NopRecorder() {
    }

    @Override
    public void record(Step step) {
        // intentional no-op
    }
}
