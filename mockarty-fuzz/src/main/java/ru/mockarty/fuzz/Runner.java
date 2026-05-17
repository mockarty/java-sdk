// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Thin client that runs a {@link Target} either against a running
 * Mockarty admin server (via REST) or against a local {@code mockarty-cli}
 * subprocess. The SDK itself NEVER mutates payloads or invokes
 * detectors — the fuzz engine on the server / inside the CLI does all
 * the work; see {@code internal/fuzzing/engine.go}.
 *
 * <p>Two paths converge on identical {@link Result} shapes:</p>
 * <ul>
 *   <li>{@link #submit(Target)} POSTs the transpiled JSON to the admin's
 *       {@code /api/v1/fuzzing/run} endpoint; {@link #wait(JobId)} polls
 *       {@code /api/v1/fuzzing/results/{id}} until the run is terminal;
 *       {@link #stream(JobId)} subscribes to the SSE event channel.</li>
 *   <li>{@link #localSpawn(Target)} writes the JSON to a temp file and
 *       forks {@code mockarty-cli fuzz run <file> --json} — used when
 *       the developer has no running admin (offline iteration, air-gapped
 *       CI runners, IDE-only loops).</li>
 * </ul>
 */
public final class Runner implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String adminUrl;
    private final String namespace;
    private final String apiToken;
    private final HttpTransport http;
    private final ProcessLauncher processes;
    private final String cliBinary;
    private final Duration pollInterval;
    private final Duration pollTimeout;

    private Runner(Builder b) {
        this.adminUrl = b.adminUrl;
        this.namespace = b.namespace == null ? "" : b.namespace;
        this.apiToken = b.apiToken == null ? "" : b.apiToken;
        this.http = b.http == null ? new JdkHttpTransport() : b.http;
        this.processes = b.processes == null ? ProcessLauncher.jdk() : b.processes;
        this.cliBinary = b.cliBinary == null ? "mockarty-cli" : b.cliBinary;
        this.pollInterval = b.pollInterval == null ? Duration.ofSeconds(2) : b.pollInterval;
        this.pollTimeout = b.pollTimeout == null ? Duration.ofMinutes(30) : b.pollTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience for the common case — admin URL + namespace + API
     * token, default transport, default CLI binary. Equivalent to
     * {@code builder().adminUrl(...).namespace(...).apiToken(...).build()}.
     */
    public static Runner of(String adminUrl, String namespace, String apiToken) {
        return builder().adminUrl(adminUrl).namespace(namespace).apiToken(apiToken).build();
    }

    // ── Submit / Wait / Stream (remote admin path) ───────────────────

    /** Submits the target to the admin's fuzz API and returns the job id. */
    public JobId submit(Target target) throws IOException {
        Objects.requireNonNull(target, "target must not be null");
        requireAdminUrl();
        String body = Transpiler.toJson(target);
        try {
            HttpTransport.HttpResponse resp = http.request(
                    "POST", adminUrl + "/api/v1/fuzzing/run", authHeaders(true), body);
            if (resp.status() < 200 || resp.status() >= 300) {
                throw new IOException("submit failed: HTTP " + resp.status() + " — " + resp.body());
            }
            JsonNode node = MAPPER.readTree(resp.body());
            // The admin returns the FuzzingRun shape; the id field name
            // is "id" in current builds. Be defensive about runId too —
            // it's the field name in older versions.
            JsonNode id = node.path("id");
            if (id.isMissingNode() || id.isNull()) id = node.path("runId");
            if (id.isMissingNode() || id.isNull()) {
                throw new IOException("submit response has no id field: " + resp.body());
            }
            return new JobId(id.asText());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("submit interrupted", e);
        }
    }

    /**
     * Polls the admin until the job reaches a terminal state, then
     * returns the parsed {@link Result}. Throws {@link IOException} on
     * timeout. Polling cadence is configured via {@link Builder#pollInterval}.
     */
    public Result waitFor(JobId jobId) throws IOException {
        Objects.requireNonNull(jobId, "jobId must not be null");
        requireAdminUrl();
        Instant deadline = Instant.now().plus(pollTimeout);
        while (Instant.now().isBefore(deadline)) {
            Result r = pollOnce(jobId);
            if (isTerminal(r.status())) return r;
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("waitFor interrupted", e);
            }
        }
        throw new IOException("waitFor timeout after " + pollTimeout + " for job " + jobId.value());
    }

    /** {@code wait} is a JVM-reserved method on Object — alias for {@link #waitFor}. */
    public Result wait(JobId jobId) throws IOException {
        return waitFor(jobId);
    }

    /**
     * Subscribes to the SSE event stream for {@code jobId} and exposes
     * each event as a lazy {@link Stream}. The stream returns
     * {@link Event.Completed} as its final element when the run finishes.
     * Closing the stream closes the underlying HTTP connection.
     */
    public Stream<Event> stream(JobId jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        requireAdminUrl();
        BlockingQueue<EventOrEnd> queue = new ArrayBlockingQueue<>(256);
        AtomicBoolean stop = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            try {
                http.streamEvents(
                        adminUrl + "/api/v1/fuzzing/run/" + jobId.value() + "/events",
                        authHeaders(false),
                        line -> {
                            Event e = parseSseLine(line);
                            if (e != null) {
                                try { queue.put(EventOrEnd.of(e)); }
                                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                            }
                        },
                        stop::get);
            } catch (Exception e) {
                // Failure during stream — surface it as a synthetic
                // terminal event so the consumer's reduce/for-each still
                // terminates rather than hanging.
                try { queue.put(EventOrEnd.error(e)); } catch (InterruptedException ignored) {}
            } finally {
                try { queue.put(EventOrEnd.end()); } catch (InterruptedException ignored) {}
            }
        }, "mockarty-fuzz-stream-" + jobId.value());
        t.setDaemon(true);
        t.start();

        Iterator<Event> iterator = new Iterator<>() {
            Event next;
            boolean exhausted;

            @Override public boolean hasNext() {
                if (exhausted) return false;
                if (next != null) return true;
                try {
                    // Single blocking take — the producer thread guarantees
                    // an EventOrEnd.end() in its finally block, so we will
                    // never wait forever. Cheaper and clearer than the prior
                    // busy-poll loop.
                    EventOrEnd e = queue.take();
                    if (e.error != null) {
                        exhausted = true;
                        throw new RuntimeException("stream error", e.error);
                    }
                    if (e.end) { exhausted = true; return false; }
                    next = e.event;
                    return true;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    exhausted = true;
                    return false;
                }
            }

            @Override public Event next() {
                if (!hasNext()) throw new NoSuchElementException();
                Event r = next;
                next = null;
                return r;
            }
        };
        Spliterator<Event> spl = Spliterators.spliteratorUnknownSize(
                iterator, Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(spl, false).onClose(() -> stop.set(true));
    }

    /** Stops a running job (DELETE /run/{id}). Idempotent. */
    public void stop(JobId jobId) throws IOException {
        Objects.requireNonNull(jobId, "jobId must not be null");
        requireAdminUrl();
        try {
            http.request("POST", adminUrl + "/api/v1/fuzzing/run/" + jobId.value() + "/stop",
                    authHeaders(false), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("stop interrupted", e);
        }
    }

    // ── LocalSpawn (CLI subprocess path) ─────────────────────────────

    /**
     * Writes the target to a temporary JSON file and invokes
     * {@code mockarty-cli fuzz run <file> --json} (or the binary supplied
     * via {@link Builder#cliBinary}). The CLI's final stdout line MUST be
     * a parseable {@link Result} JSON object — older CLI builds use the
     * exit code only and a {@code --json} flag is required to get the
     * structured result.
     */
    public Result localSpawn(Target target) throws IOException {
        Objects.requireNonNull(target, "target must not be null");
        Path tmp = Files.createTempFile("mockarty-fuzz-", ".json");
        try {
            target.writeTo(tmp);
            List<String> argv = new ArrayList<>();
            argv.add(cliBinary);
            argv.add("fuzz");
            argv.add("run");
            argv.add(tmp.toString());
            argv.add("--json");
            ProcessLauncher.ProcessResult pr;
            try {
                pr = processes.spawn(argv, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("localSpawn interrupted", e);
            }
            if (pr.exitCode() != 0) {
                throw new IOException("mockarty-cli fuzz run exited " + pr.exitCode()
                        + "\nstdout: " + pr.stdout()
                        + "\nstderr: " + pr.stderr());
            }
            // The CLI emits a result envelope as the last non-blank
            // stdout line. We pick the last { ... } object out of stdout
            // so progress lines printed earlier don't confuse parsing.
            String resultLine = extractLastJsonObject(pr.stdout());
            if (resultLine == null) {
                throw new IOException("mockarty-cli stdout has no JSON result envelope:\n" + pr.stdout());
            }
            return parseResult(MAPPER.readTree(resultLine));
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    @Override
    public void close() {
        // The JDK HttpClient has no explicit close in JDK 17; leave as
        // a hook so callers can use try-with-resources and we can wire
        // in cleanup logic later (e.g. shared executor shutdown).
    }

    // ── Builder ──────────────────────────────────────────────────────

    public static final class Builder {
        private String adminUrl;
        private String namespace;
        private String apiToken;
        private HttpTransport http;
        private ProcessLauncher processes;
        private String cliBinary;
        private Duration pollInterval;
        private Duration pollTimeout;

        public Builder adminUrl(String adminUrl) { this.adminUrl = adminUrl; return this; }
        public Builder namespace(String namespace) { this.namespace = namespace; return this; }
        public Builder apiToken(String apiToken) { this.apiToken = apiToken; return this; }
        public Builder http(HttpTransport http) { this.http = http; return this; }
        public Builder processes(ProcessLauncher processes) { this.processes = processes; return this; }
        public Builder cliBinary(String cliBinary) { this.cliBinary = cliBinary; return this; }
        public Builder pollInterval(Duration pollInterval) { this.pollInterval = pollInterval; return this; }
        public Builder pollTimeout(Duration pollTimeout) { this.pollTimeout = pollTimeout; return this; }

        public Runner build() { return new Runner(this); }
    }

    // ── Internals ────────────────────────────────────────────────────

    private void requireAdminUrl() {
        if (adminUrl == null || adminUrl.isBlank()) {
            throw new IllegalStateException(
                    "adminUrl is not set — remote-fuzz paths (submit/wait/stream/stop) require an admin URL. "
                            + "Use localSpawn() for the CLI-only path.");
        }
    }

    private Map<String, String> authHeaders(boolean json) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (!apiToken.isEmpty()) {
            // Mockarty admin accepts both X-API-Token and X-API-Key — we
            // send -Key because that's the post-2026-05 canonical header
            // (see docs audit 2026_05_15).
            headers.put("X-API-Key", apiToken);
        }
        if (!namespace.isEmpty()) {
            headers.put("X-Namespace", namespace);
        }
        if (json) {
            headers.put("Content-Type", "application/json");
        }
        return headers;
    }

    private Result pollOnce(JobId jobId) throws IOException {
        try {
            HttpTransport.HttpResponse resp = http.request(
                    "GET", adminUrl + "/api/v1/fuzzing/results/" + jobId.value(),
                    authHeaders(false), null);
            if (resp.status() == 404) {
                // Result row not yet committed — treat as "queued".
                return new Result(jobId, "queued", 0, 0, 0, 0, 0, 0, 0, 0, List.of());
            }
            if (resp.status() < 200 || resp.status() >= 300) {
                throw new IOException("poll failed: HTTP " + resp.status() + " — " + resp.body());
            }
            return parseResult(MAPPER.readTree(resp.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("poll interrupted", e);
        }
    }

    private static Result parseResult(JsonNode node) {
        // Build a JobId from whichever id field the server populates.
        JsonNode idNode = node.path("id");
        if (idNode.isMissingNode() || idNode.isNull()) idNode = node.path("runId");
        JobId jobId = idNode.isMissingNode() || idNode.isNull()
                ? new JobId("unknown")
                : new JobId(idNode.asText());
        String status = node.path("status").asText("unknown");
        long durationMs = node.path("durationMs").asLong(0);
        long totalReq = node.path("totalRequests").asLong(0);
        int total = node.path("totalFindings").asInt(0);
        int crit = node.path("criticalFindings").asInt(0);
        int high = node.path("highFindings").asInt(0);
        int med = node.path("mediumFindings").asInt(0);
        int low = node.path("lowFindings").asInt(0);
        int info = node.path("infoFindings").asInt(0);
        List<Finding> findings = new ArrayList<>();
        JsonNode fn = node.path("findings");
        if (fn.isArray()) {
            for (JsonNode f : fn) findings.add(parseFinding(f));
        }
        return new Result(jobId, status, durationMs, totalReq, total, crit, high, med, low, info, findings);
    }

    private static Finding parseFinding(JsonNode n) {
        return new Finding(
                n.path("id").asText(""),
                n.path("severity").asText(""),
                n.path("category").asText(""),
                n.path("title").asText(""),
                n.path("requestMethod").asText(""),
                n.path("requestUrl").asText(""),
                n.path("responseStatus").asInt(0),
                n.path("responseTimeMs").asLong(0));
    }

    /**
     * Extracts the last {@code { ... }} object from {@code stdout}. We
     * scan back-to-front to be robust against the CLI printing progress
     * lines on stdout before the result envelope.
     */
    static String extractLastJsonObject(String stdout) {
        if (stdout == null) return null;
        int end = stdout.lastIndexOf('}');
        if (end < 0) return null;
        // Walk back to the matching '{' using brace-depth bookkeeping so
        // we don't get tripped up by nested objects in the result.
        int depth = 0;
        for (int i = end; i >= 0; i--) {
            char c = stdout.charAt(i);
            if (c == '}') depth++;
            else if (c == '{') {
                depth--;
                if (depth == 0) return stdout.substring(i, end + 1);
            }
        }
        return null;
    }

    private static boolean isTerminal(String status) {
        return "completed".equalsIgnoreCase(status)
                || "failed".equalsIgnoreCase(status)
                || "cancelled".equalsIgnoreCase(status)
                || "timeout".equalsIgnoreCase(status);
    }

    /**
     * Parses one SSE line. We honour both the official {@code data: ...}
     * prefix and a bare JSON line (some intermediaries strip the prefix).
     * Comments ({@code :foo}) and empty lines yield null.
     */
    private static Event parseSseLine(String line) {
        if (line == null || line.isBlank() || line.startsWith(":")) return null;
        String payload = line.startsWith("data:")
                ? line.substring("data:".length()).trim()
                : line.trim();
        if (payload.isEmpty()) return null;
        try {
            JsonNode node = MAPPER.readTree(payload);
            String type = node.path("type").asText("");
            return switch (type) {
                case "progress" -> new Event.Progress(
                        node.path("completedRequests").asLong(0),
                        node.path("totalRequests").asLong(0),
                        node.path("requestsPerSecond").asDouble(0),
                        node.path("findings").path("critical").asInt(0),
                        node.path("findings").path("high").asInt(0),
                        node.path("findings").path("medium").asInt(0),
                        node.path("findings").path("low").asInt(0),
                        node.path("findings").path("info").asInt(0));
                case "finding" -> new Event.FindingFound(parseFinding(node.path("finding")));
                case "completed" -> new Event.Completed(parseResult(node.path("result")));
                default -> null;
            };
        } catch (IOException ignored) {
            // Malformed SSE payload — skip rather than blowing up the
            // whole stream. The caller's reduce/for-each will still see
            // the legit events.
            return null;
        }
    }

    /** Internal sum-type for the producer/consumer SSE pipe. */
    private static final class EventOrEnd {
        final Event event;
        final boolean end;
        final Throwable error;
        private EventOrEnd(Event e, boolean end, Throwable err) {
            this.event = e; this.end = end; this.error = err;
        }
        static EventOrEnd of(Event e) { return new EventOrEnd(e, false, null); }
        static EventOrEnd end() { return new EventOrEnd(null, true, null); }
        static EventOrEnd error(Throwable t) { return new EventOrEnd(null, false, t); }
    }

    /** Type token kept so future enhancements (e.g. raw map decode) compile. */
    @SuppressWarnings("unused")
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
}
