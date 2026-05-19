package ru.mockarty.protocols.telemetry;

/**
 * Narrow seam between protocol clients and step sinks.
 *
 * <p>Implementations MUST be safe for concurrent use — every protocol
 * client in this SDK can fire calls from multiple threads in a
 * single test. {@link NopRecorder} is the zero-cost default;
 * {@link AccumulatingRecorder} buffers steps for later submission
 * to the Mockarty external-run endpoint.
 */
@FunctionalInterface
public interface StepRecorder {

    /**
     * Records one completed step. Called once per protocol-client
     * call after the operation returns (or fails). The
     * implementation should not block — buffering implementations
     * are encouraged to enqueue and flush on a background thread.
     */
    void record(Step step);
}
