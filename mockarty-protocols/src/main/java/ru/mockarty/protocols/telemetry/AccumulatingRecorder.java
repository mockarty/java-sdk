package ru.mockarty.protocols.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thread-safe buffer that accumulates {@link Step}s for later
 * submission to the Mockarty external-run endpoint.
 *
 * <p>Typical use:
 * <pre>{@code
 * AccumulatingRecorder rec = new AccumulatingRecorder();
 * SoapClient soap = new SoapClient(url, opts -> opts.recorder(rec));
 * GraphQLClient gql = new GraphQLClient(url, opts -> opts.recorder(rec));
 * // ... run the test ...
 * client.externalRuns().report(builder ->
 *     builder.caseName("my case").status("passed").steps(rec.payloads()));
 * }</pre>
 *
 * <p>Backed by a synchronized {@link ArrayList}; {@link #payloads()}
 * + {@link #raw()} return copies so callers can mutate the result
 * without poisoning the recorder.
 */
public final class AccumulatingRecorder implements StepRecorder {

    private final List<Step> steps = new ArrayList<>();
    private final Object lock = new Object();

    @Override
    public void record(Step step) {
        if (step == null) return;
        synchronized (lock) {
            steps.add(step);
        }
    }

    /**
     * @return the accumulated steps in wire-payload shape suitable
     *     for {@code ExternalRunsApi.report(steps=…)}.
     */
    public List<java.util.Map<String, Object>> payloads() {
        synchronized (lock) {
            return steps.stream().map(Step::toPayload).collect(Collectors.toList());
        }
    }

    /** @return a defensive copy of the accumulated {@link Step}s. */
    public List<Step> raw() {
        synchronized (lock) {
            return new ArrayList<>(steps);
        }
    }

    /** Drop every accumulated step. Useful when sharing one
     *  recorder across multiple test methods. */
    public void clear() {
        synchronized (lock) {
            steps.clear();
        }
    }

    /** @return number of buffered steps. */
    public int size() {
        synchronized (lock) {
            return steps.size();
        }
    }

    /** @return true when no steps are buffered. */
    public boolean isEmpty() {
        return size() == 0;
    }
}
