package ru.mockarty.protocols.telemetry;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Protocol-agnostic captured step.
 *
 * <p>Each protocol client (SOAP, GraphQL, SSE, WebSocket) reports one
 * {@code Step} per operation through a {@link StepRecorder}. The
 * {@link AccumulatingRecorder} buffers them so the test driver can
 * forward the full timeline to the Mockarty external-run endpoint
 * when the suite finishes.
 *
 * <p>Construct via the {@link Builder} for readability:
 * <pre>{@code
 * Step step = Step.builder()
 *     .key(Telemetry.newStepKey("graphql:GetUser", seq))
 *     .name("graphql:GetUser")
 *     .status("passed")
 *     .startedAt(start)
 *     .finishedAt(end)
 *     .parameter("http_status", "200")
 *     .build();
 * }</pre>
 */
public final class Step {

    private final String key;
    private final String name;
    private final String status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final long durationMs;
    private final Map<String, String> parameters;
    private final String message;
    private final String stackTrace;
    private final String parentKey;

    private Step(Builder b) {
        this.key = b.key;
        this.name = b.name;
        this.status = (b.status == null || b.status.isEmpty()) ? "passed" : b.status;
        this.startedAt = b.startedAt;
        this.finishedAt = b.finishedAt;
        long dur = b.durationMs;
        if (dur <= 0 && b.startedAt != null && b.finishedAt != null) {
            dur = Math.max(0, b.finishedAt.toEpochMilli() - b.startedAt.toEpochMilli());
        }
        this.durationMs = dur;
        this.parameters = b.parameters == null ? Map.of() : Map.copyOf(b.parameters);
        this.message = b.message == null ? "" : b.message;
        this.stackTrace = b.stackTrace == null ? "" : b.stackTrace;
        this.parentKey = b.parentKey == null ? "" : b.parentKey;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public long getDurationMs() { return durationMs; }
    public Map<String, String> getParameters() { return parameters; }
    public String getMessage() { return message; }
    public String getStackTrace() { return stackTrace; }
    public String getParentKey() { return parentKey; }

    /**
     * Serialize to the wire shape ExternalRunsApi.report expects. The
     * keys mirror the Go/Python SDK output so cross-language test
     * suites collapse on the same step rows server-side.
     */
    public Map<String, Object> toPayload() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stepKey", key);
        out.put("name", name);
        out.put("status", status);
        if (startedAt != null) out.put("startedAt", startedAt.toString());
        if (finishedAt != null) out.put("finishedAt", finishedAt.toString());
        if (durationMs > 0) out.put("durationMs", durationMs);
        if (!parameters.isEmpty()) out.put("parameters", new HashMap<>(parameters));
        if (!message.isEmpty()) out.put("message", message);
        if (!stackTrace.isEmpty()) out.put("stackTrace", stackTrace);
        if (!parentKey.isEmpty()) out.put("parentKey", parentKey);
        return out;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link Step}. */
    public static final class Builder {
        private String key = "";
        private String name = "";
        private String status = "passed";
        private Instant startedAt;
        private Instant finishedAt;
        private long durationMs;
        private Map<String, String> parameters;
        private String message = "";
        private String stackTrace = "";
        private String parentKey = "";

        public Builder key(String key) { this.key = key; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder startedAt(Instant t) { this.startedAt = t; return this; }
        public Builder finishedAt(Instant t) { this.finishedAt = t; return this; }
        public Builder durationMs(long ms) { this.durationMs = ms; return this; }
        public Builder message(String m) { this.message = m; return this; }
        public Builder stackTrace(String s) { this.stackTrace = s; return this; }
        public Builder parentKey(String k) { this.parentKey = k; return this; }

        public Builder parameter(String k, String v) {
            if (parameters == null) parameters = new LinkedHashMap<>();
            parameters.put(k, v);
            return this;
        }

        public Builder parameters(Map<String, String> p) {
            if (p == null || p.isEmpty()) return this;
            if (parameters == null) parameters = new LinkedHashMap<>();
            parameters.putAll(p);
            return this;
        }

        public Step build() { return new Step(this); }
    }
}
