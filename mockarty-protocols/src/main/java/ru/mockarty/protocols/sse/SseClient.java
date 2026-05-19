package ru.mockarty.protocols.sse;

import ru.mockarty.protocols.telemetry.NopRecorder;
import ru.mockarty.protocols.telemetry.Step;
import ru.mockarty.protocols.telemetry.StepRecorder;
import ru.mockarty.protocols.telemetry.Telemetry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sync Server-Sent Events client. One {@link #collect(int, Duration)}
 * call records one {@link Step} covering total event count + duration.
 *
 * <p>Parses according to the WHATWG SSE dispatch rules — blank line
 * dispatches the accumulated event, lines starting with {@code :} are
 * treated as comments, unknown fields are silently dropped.
 */
public final class SseClient implements AutoCloseable {

    private final URI url;
    private final Map<String, String> headers;
    private final StepRecorder recorder;
    private final int payloadCap;
    private final HttpClient http;
    private final AtomicLong counter = new AtomicLong(0);

    public SseClient(String url) {
        this(url, opts -> {});
    }

    public SseClient(String url, java.util.function.Consumer<Options> configure) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("mockarty sse: empty url");
        }
        Options opts = new Options();
        configure.accept(opts);
        this.url = URI.create(url);
        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/event-stream");
        h.putAll(opts.headers);
        this.headers = Map.copyOf(h);
        this.recorder = opts.recorder == null ? NopRecorder.INSTANCE : opts.recorder;
        this.payloadCap = Math.max(0, opts.payloadCap);
        this.http = opts.client == null
            ? HttpClient.newBuilder().connectTimeout(opts.connectTimeout).build()
            : opts.client;
    }

    /**
     * Pull up to {@code maxEvents} events or until {@code deadline}
     * elapses. Returns the events received so far when either limit
     * fires; never throws on timeout.
     */
    public List<SseEvent> collect(int maxEvents, Duration deadline) {
        if (maxEvents <= 0) {
            throw new IllegalArgumentException("mockarty sse: maxEvents must be >= 1");
        }
        Duration timeout = deadline == null || deadline.isZero() || deadline.isNegative()
            ? Duration.ofSeconds(30) : deadline;
        Instant started = Instant.now();
        Instant deadlineAt = started.plus(timeout);
        HttpRequest.Builder rb = HttpRequest.newBuilder(url)
            .GET()
            .timeout(timeout);
        headers.forEach(rb::header);

        List<SseEvent> events = new ArrayList<>();
        try {
            HttpResponse<java.io.InputStream> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() >= 400) {
                recordStep("sse:collect", started, Instant.now(), "failed",
                    new RuntimeException("HTTP " + resp.statusCode()),
                    Map.of("http_status", String.valueOf(resp.statusCode()), "count", "0"));
                return events;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                parse(reader, events, maxEvents, deadlineAt);
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            recordStep("sse:collect", started, Instant.now(), "broken", ex,
                Map.of("count", String.valueOf(events.size())));
            throw new SseException("transport: " + ex.getMessage(), ex);
        }
        recordStep("sse:collect", started, Instant.now(), "passed", null, Map.of(
            "count", String.valueOf(events.size()),
            "url", Telemetry.capPreview(url.toString(), payloadCap)));
        return events;
    }

    private void recordStep(String name, Instant started, Instant finished,
                            String status, Throwable err, Map<String, String> params) {
        Step.Builder b = Step.builder()
            .key(Telemetry.newStepKey(name, counter.incrementAndGet()))
            .name(name)
            .status(status)
            .startedAt(started)
            .finishedAt(finished)
            .durationMs(Math.max(0, finished.toEpochMilli() - started.toEpochMilli()))
            .parameters(params);
        if (err != null) {
            b.message(err.getMessage() == null ? err.getClass().getSimpleName() : err.getMessage());
        }
        recorder.record(b.build());
    }

    @Override
    public void close() { /* HttpClient is GC-managed in JDK 11+. */ }

    /**
     * WHATWG-spec SSE parser. Reads frame lines from {@code reader}
     * until {@code maxEvents} events have been dispatched OR the
     * deadline elapses. Exposed package-private so unit tests can
     * exercise the parser without a live HTTP server.
     */
    static void parse(BufferedReader reader, List<SseEvent> events, int maxEvents, Instant deadlineAt) throws IOException {
        List<String> dataBuf = new ArrayList<>();
        String eventName = null;
        String id = null;
        Integer retry = null;
        List<String> rawLines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            rawLines.add(line);
            if (line.isEmpty()) {
                if (!dataBuf.isEmpty()) {
                    events.add(new SseEvent(String.join("\n", dataBuf), id, eventName, retry, rawLines));
                    if (events.size() >= maxEvents) return;
                }
                dataBuf = new ArrayList<>();
                eventName = null;
                retry = null;
                rawLines = new ArrayList<>();
                if (Instant.now().isAfter(deadlineAt)) return;
                continue;
            }
            if (line.startsWith(":")) {
                continue; // comment per spec
            }
            int colon = line.indexOf(':');
            String field;
            String value;
            if (colon < 0) {
                field = line;
                value = "";
            } else {
                field = line.substring(0, colon);
                value = line.substring(colon + 1);
                if (value.startsWith(" ")) value = value.substring(1);
            }
            switch (field) {
                case "data":  dataBuf.add(value); break;
                case "event": eventName = value; break;
                case "id":    id = value; break;
                case "retry":
                    try { retry = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                    break;
                default: /* unknown field ignored per spec */ break;
            }
        }
    }

    /** Options for {@link SseClient}. */
    public static final class Options {
        private final Map<String, String> headers = new HashMap<>();
        private StepRecorder recorder = NopRecorder.INSTANCE;
        private int payloadCap = 1024;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private HttpClient client;

        public Options header(String k, String v) { headers.put(k, v); return this; }
        public Options recorder(StepRecorder r) { this.recorder = r == null ? NopRecorder.INSTANCE : r; return this; }
        public Options payloadCap(int n) { this.payloadCap = Math.max(0, n); return this; }
        public Options connectTimeout(Duration d) { if (d != null && !d.isZero() && !d.isNegative()) this.connectTimeout = d; return this; }
        public Options client(HttpClient c) { this.client = c; return this; }
    }
}
