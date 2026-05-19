package ru.mockarty.protocols.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.protocols.telemetry.NopRecorder;
import ru.mockarty.protocols.telemetry.Step;
import ru.mockarty.protocols.telemetry.StepRecorder;
import ru.mockarty.protocols.telemetry.Telemetry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sync GraphQL test client built on the JDK's {@link HttpClient}.
 *
 * <p>Sends GraphQL documents as {@code POST /graphql} with the
 * standard JSON envelope {@code {"query": "...", "variables": {...}}}.
 * Treats any non-empty {@code errors[]} entry in the response as a
 * failed step even when HTTP returned 200.
 *
 * <p>One client → one URL. Reuse across queries / mutations for
 * connection pooling.
 */
public final class GraphQLClient implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final URI url;
    private final Map<String, String> defaultHeaders;
    private final StepRecorder recorder;
    private final int payloadCap;
    private final Duration timeout;
    private final HttpClient http;
    private final AtomicLong counter = new AtomicLong(0);

    public GraphQLClient(String url) {
        this(url, opts -> {});
    }

    public GraphQLClient(String url, java.util.function.Consumer<Options> configure) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("mockarty graphql: empty url");
        }
        Options opts = new Options();
        configure.accept(opts);
        this.url = URI.create(url);
        this.defaultHeaders = Map.copyOf(opts.headers);
        this.recorder = opts.recorder == null ? NopRecorder.INSTANCE : opts.recorder;
        this.payloadCap = Math.max(0, opts.payloadCap);
        this.timeout = opts.timeout;
        this.http = opts.client == null
            ? HttpClient.newBuilder().connectTimeout(timeout).build()
            : opts.client;
    }

    /** Send a GraphQL request and return the parsed response. */
    public GraphQLResponse execute(String query) {
        return execute(query, null, null);
    }

    public GraphQLResponse execute(String query, Map<String, Object> variables) {
        return execute(query, variables, null);
    }

    public GraphQLResponse execute(String query, Map<String, Object> variables, String operationName) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("mockarty graphql: empty query");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        if (variables != null && !variables.isEmpty()) {
            body.put("variables", variables);
        }
        if (operationName != null && !operationName.isEmpty()) {
            body.put("operationName", operationName);
        }
        String opLabel = operationName != null && !operationName.isEmpty()
            ? operationName
            : extractOperationName(query).orElse("anonymous");
        String stepName = "graphql:" + opLabel;

        byte[] bodyBytes;
        try {
            bodyBytes = MAPPER.writeValueAsBytes(body);
        } catch (Exception e) {
            recordStep(stepName, Instant.now(), Instant.now(), "broken", e,
                Map.of("operation", opLabel));
            throw new GraphQLException("marshal request: " + e.getMessage(), e);
        }

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(url)
            .header("Content-Type", "application/json")
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        defaultHeaders.forEach(reqBuilder::header);

        Instant started = Instant.now();
        HttpResponse<String> resp;
        try {
            resp = http.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            recordStep(stepName, started, Instant.now(), "broken", ex,
                Map.of("operation", opLabel));
            throw new GraphQLException("transport: " + ex.getMessage(), ex);
        }
        Instant finished = Instant.now();

        JsonNode parsed;
        try {
            parsed = resp.body() == null || resp.body().isEmpty()
                ? MAPPER.createObjectNode()
                : MAPPER.readTree(resp.body());
        } catch (Exception ex) {
            recordStep(stepName, started, finished, "broken", ex, Map.of(
                "operation", opLabel,
                "http_status", String.valueOf(resp.statusCode()),
                "response", Telemetry.capPreview(resp.body(), payloadCap)));
            throw new GraphQLException("decode response: " + ex.getMessage(), ex);
        }

        List<Map<String, Object>> errors = new ArrayList<>();
        JsonNode errorsNode = parsed.get("errors");
        if (errorsNode != null && errorsNode.isArray()) {
            for (JsonNode e : errorsNode) {
                errors.add(MAPPER.convertValue(e, Map.class));
            }
        }
        JsonNode dataNode = parsed.get("data");
        Map<String, Object> extensions = parsed.has("extensions") && parsed.get("extensions").isObject()
            ? MAPPER.convertValue(parsed.get("extensions"), Map.class)
            : Collections.emptyMap();

        String status = "passed";
        String message = "";
        if (resp.statusCode() >= 400) {
            status = "failed";
            message = "HTTP " + resp.statusCode();
        } else if (!errors.isEmpty()) {
            status = "failed";
            Object msg = errors.get(0).get("message");
            message = msg == null ? "graphql error" : String.valueOf(msg);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("operation", opLabel);
        params.put("http_status", String.valueOf(resp.statusCode()));
        params.put("request", Telemetry.capPreview(new String(bodyBytes), payloadCap));
        params.put("response", Telemetry.capPreview(resp.body(), payloadCap));
        params.put("error_count", String.valueOf(errors.size()));
        recordStep(stepName, started, finished, status,
            "passed".equals(status) ? null : new RuntimeException(message), params);

        return new GraphQLResponse(resp.statusCode(), dataNode, errors, extensions);
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
        if (err != null) b.message(err.getMessage() == null ? err.getClass().getSimpleName() : err.getMessage());
        recorder.record(b.build());
    }

    @Override
    public void close() {
        // HttpClient has no explicit close in JDK 11+; instance is GC-managed.
    }

    /**
     * Best-effort: pull the operation name out of a GraphQL document.
     * Skips leading whitespace + comments, then expects
     * {@code query|mutation|subscription <Name>}.
     */
    static java.util.Optional<String> extractOperationName(String query) {
        if (query == null || query.isEmpty()) {
            return java.util.Optional.empty();
        }
        for (String raw : query.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            for (String kw : new String[]{"query ", "mutation ", "subscription "}) {
                if (line.startsWith(kw)) {
                    String rest = line.substring(kw.length()).trim();
                    StringBuilder name = new StringBuilder();
                    for (int i = 0; i < rest.length(); i++) {
                        char c = rest.charAt(i);
                        if (Character.isLetterOrDigit(c) || c == '_') {
                            name.append(c);
                        } else {
                            break;
                        }
                    }
                    return name.length() == 0 ? java.util.Optional.empty() : java.util.Optional.of(name.toString());
                }
            }
            return java.util.Optional.empty();
        }
        return java.util.Optional.empty();
    }

    /** Fluent options bag for {@link GraphQLClient}. */
    public static final class Options {
        private final Map<String, String> headers = new HashMap<>();
        private StepRecorder recorder = NopRecorder.INSTANCE;
        private int payloadCap = 1024;
        private Duration timeout = Duration.ofSeconds(30);
        private HttpClient client;

        public Options header(String k, String v) { headers.put(k, v); return this; }
        public Options headers(Map<String, String> h) { if (h != null) headers.putAll(h); return this; }
        public Options recorder(StepRecorder r) { this.recorder = r == null ? NopRecorder.INSTANCE : r; return this; }
        public Options payloadCap(int n) { this.payloadCap = Math.max(0, n); return this; }
        public Options timeout(Duration d) { if (d != null && !d.isZero() && !d.isNegative()) this.timeout = d; return this; }
        public Options client(HttpClient c) { this.client = c; return this; }
    }
}
