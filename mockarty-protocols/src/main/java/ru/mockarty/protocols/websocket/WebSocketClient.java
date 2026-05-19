package ru.mockarty.protocols.websocket;

import ru.mockarty.protocols.telemetry.NopRecorder;
import ru.mockarty.protocols.telemetry.Step;
import ru.mockarty.protocols.telemetry.StepRecorder;
import ru.mockarty.protocols.telemetry.Telemetry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sync WebSocket test client built on the JDK 11+ {@link WebSocket} API.
 * No external dependency. Each {@link #send(String)} and {@link #recv}
 * records one step so the TCM run shows a per-frame timeline.
 *
 * <p>Implementation detail: the JDK {@code WebSocket} API is fully
 * async. We bridge to sync by enqueueing incoming text frames in a
 * thread-safe queue and letting {@code recv} drain it (with a
 * deadline). Long-running subscriptions are better served by
 * {@code SseClient} where applicable.
 */
public final class WebSocketClient implements AutoCloseable {

    private final URI uri;
    private final Map<String, String> headers;
    private final StepRecorder recorder;
    private final int payloadCap;
    private final Duration openTimeout;
    private final HttpClient http;
    private final AtomicLong counter = new AtomicLong(0);

    // Lazy connection state.
    private volatile WebSocket socket;
    private final Queue<String> inbox = new LinkedList<>();
    private final Object inboxLock = new Object();
    private volatile boolean closedByServer;

    public WebSocketClient(String url) {
        this(url, opts -> {});
    }

    public WebSocketClient(String url, java.util.function.Consumer<Options> configure) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("mockarty websocket: empty url");
        }
        Options opts = new Options();
        configure.accept(opts);
        this.uri = URI.create(url);
        this.headers = Map.copyOf(opts.headers);
        this.recorder = opts.recorder == null ? NopRecorder.INSTANCE : opts.recorder;
        this.payloadCap = Math.max(0, opts.payloadCap);
        this.openTimeout = opts.openTimeout;
        this.http = opts.client == null ? HttpClient.newHttpClient() : opts.client;
    }

    /** Send a text frame. */
    public void send(String payload) {
        String stepName = "ws:send";
        Instant started = Instant.now();
        try {
            ensureOpen();
            socket.sendText(payload == null ? "" : payload, true).get(openTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            recordStep(stepName, started, Instant.now(), "broken", unwrap(ex),
                Map.of("payload", Telemetry.capPreview(payload == null ? "" : payload, payloadCap)));
            throw new WebSocketException("send: " + unwrap(ex).getMessage(), unwrap(ex));
        }
        recordStep(stepName, started, Instant.now(), "passed", null,
            Map.of("payload", Telemetry.capPreview(payload == null ? "" : payload, payloadCap)));
    }

    /**
     * Wait for one text frame. Returns the frame payload; throws
     * {@link WebSocketException} on timeout.
     */
    public String recv(Duration timeout) {
        String stepName = "ws:recv";
        Instant started = Instant.now();
        Duration t = timeout == null || timeout.isZero() || timeout.isNegative()
            ? Duration.ofSeconds(30) : timeout;
        Instant deadline = started.plus(t);
        try {
            ensureOpen();
            while (true) {
                synchronized (inboxLock) {
                    String head = inbox.poll();
                    if (head != null) {
                        recordStep(stepName, started, Instant.now(), "passed", null,
                            Map.of("payload", Telemetry.capPreview(head, payloadCap),
                                   "size", String.valueOf(head.length())));
                        return head;
                    }
                    if (closedByServer) {
                        throw new WebSocketException("server closed connection");
                    }
                    long left = Duration.between(Instant.now(), deadline).toMillis();
                    if (left <= 0) {
                        throw new WebSocketException("recv timed out after " + t);
                    }
                    inboxLock.wait(left);
                }
            }
        } catch (WebSocketException ex) {
            recordStep(stepName, started, Instant.now(), "broken", ex,
                Map.of("timeout", t.toString()));
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            recordStep(stepName, started, Instant.now(), "broken", ex, Map.of("timeout", t.toString()));
            throw new WebSocketException("interrupted", ex);
        }
    }

    private void ensureOpen() {
        if (socket != null) return;
        synchronized (this) {
            if (socket != null) return;
            WebSocket.Builder wsb = http.newWebSocketBuilder().connectTimeout(openTimeout);
            headers.forEach(wsb::header);
            try {
                socket = wsb.buildAsync(uri, new InboxListener()).get(openTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                throw new WebSocketException("open: " + unwrap(ex).getMessage(), unwrap(ex));
            }
        }
    }

    @Override
    public void close() {
        WebSocket s = this.socket;
        if (s == null) return;
        try {
            s.sendClose(WebSocket.NORMAL_CLOSURE, "client closing").get(openTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
        this.socket = null;
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

    private static Throwable unwrap(Throwable t) {
        Throwable c = t;
        while ((c instanceof ExecutionException || c instanceof CompletionException) && c.getCause() != null) {
            c = c.getCause();
        }
        return c;
    }

    /** WebSocket listener that drops text frames into the inbox. */
    private final class InboxListener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                synchronized (inboxLock) {
                    inbox.add(buf.toString());
                    inboxLock.notifyAll();
                }
                buf.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            synchronized (inboxLock) {
                closedByServer = true;
                inboxLock.notifyAll();
            }
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            synchronized (inboxLock) {
                closedByServer = true;
                inboxLock.notifyAll();
            }
        }
    }

    /** Options for {@link WebSocketClient}. */
    public static final class Options {
        private final Map<String, String> headers = new HashMap<>();
        private StepRecorder recorder = NopRecorder.INSTANCE;
        private int payloadCap = 1024;
        private Duration openTimeout = Duration.ofSeconds(10);
        private HttpClient client;

        public Options header(String k, String v) { headers.put(k, v); return this; }
        public Options recorder(StepRecorder r) { this.recorder = r == null ? NopRecorder.INSTANCE : r; return this; }
        public Options payloadCap(int n) { this.payloadCap = Math.max(0, n); return this; }
        public Options openTimeout(Duration d) { if (d != null && !d.isZero() && !d.isNegative()) this.openTimeout = d; return this; }
        public Options client(HttpClient c) { this.client = c; return this; }
    }
}
