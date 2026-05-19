package ru.mockarty.protocols.rabbitmq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;
import ru.mockarty.protocols.telemetry.NopRecorder;
import ru.mockarty.protocols.telemetry.Step;
import ru.mockarty.protocols.telemetry.StepRecorder;
import ru.mockarty.protocols.telemetry.Telemetry;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * RabbitMQ (AMQP 0-9-1) test client. Lazy connection — opens the
 * underlying {@code Connection}+{@code Channel} on first {@code publish}
 * / {@code consume} / {@code declareQueue} so test setup can wire the
 * client before the broker comes up.
 *
 * <p>The connection + channel are guarded by a single lock; the client
 * is goroutine-safe at the call level but not designed for high-fanout
 * pipelining (which test code never needs).
 */
public final class RabbitMqClient implements AutoCloseable {

    private final String amqpUrl;
    private final StepRecorder recorder;
    private final int payloadCap;
    private final AtomicLong counter = new AtomicLong(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object lock = new Object();
    private final ObjectMapper json = new ObjectMapper();
    private Connection connection;
    private Channel channel;

    public RabbitMqClient(String amqpUrl) {
        this(amqpUrl, opts -> {});
    }

    public RabbitMqClient(String amqpUrl, Consumer<Options> configure) {
        if (amqpUrl == null || amqpUrl.isEmpty()) {
            throw new IllegalArgumentException("mockarty rabbitmq: empty amqp url");
        }
        Options opts = new Options();
        configure.accept(opts);
        this.amqpUrl = amqpUrl;
        this.recorder = opts.recorder == null ? NopRecorder.INSTANCE : opts.recorder;
        this.payloadCap = Math.max(0, opts.payloadCap);
    }

    public void publish(String exchange, String routingKey, Object payload, Map<String, String> headers) {
        if (closed.get()) {
            throw new RabbitMqException("client is closed");
        }
        String name = "publish:" + nullToEmpty(exchange) + "/" + nullToEmpty(routingKey);
        long seq = counter.incrementAndGet();
        Instant started = Instant.now();
        byte[] body;
        try {
            body = marshalPayload(payload);
        } catch (Exception e) {
            RabbitMqException rex = new RabbitMqException("marshal payload: " + e.getMessage(), e);
            recordStep(name, seq, started, Instant.now(), "failed", rex, null);
            throw rex;
        }
        try {
            Channel ch = channel();
            Map<String, Object> hdrMap = new LinkedHashMap<>();
            if (headers != null) {
                hdrMap.putAll(headers);
            }
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .headers(hdrMap)
                .timestamp(new Date())
                .build();
            ch.basicPublish(nullToEmpty(exchange), nullToEmpty(routingKey), props, body);
            Map<String, String> params = new LinkedHashMap<>();
            params.put("payload", Telemetry.capPreview(body, payloadCap));
            params.put("payload_size", Integer.toString(body.length));
            recordStep(name, seq, started, Instant.now(), "passed", null, params);
        } catch (Exception e) {
            String status = classify(e);
            Map<String, String> params = new LinkedHashMap<>();
            params.put("payload", Telemetry.capPreview(body, payloadCap));
            params.put("payload_size", Integer.toString(body.length));
            recordStep(name, seq, started, Instant.now(), status, e, params);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RabbitMqException("publish " + name + ": " + e.getMessage(), e);
        }
    }

    public List<ConsumedMessage> consume(ConsumeOptions opts) {
        if (closed.get()) {
            throw new RabbitMqException("client is closed");
        }
        if (opts == null || opts.queue == null || opts.queue.isEmpty()) {
            throw new RabbitMqException("empty queue");
        }
        int max = opts.maxMessages <= 0 ? 1 : opts.maxMessages;
        long seq = counter.incrementAndGet();
        String name = "consume:" + opts.queue;
        Instant started = Instant.now();
        java.util.List<ConsumedMessage> out = new java.util.ArrayList<>(max);
        String status = "passed";
        Throwable err = null;
        try {
            Channel ch = channel();
            for (int i = 0; i < max; i++) {
                GetResponse resp = ch.basicGet(opts.queue, opts.autoAck);
                if (resp == null) {
                    break;
                }
                out.add(toConsumed(resp));
                if (!opts.autoAck) {
                    ch.basicAck(resp.getEnvelope().getDeliveryTag(), false);
                }
            }
        } catch (Exception e) {
            err = e;
            status = classify(e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("count", Integer.toString(out.size()));
        recordStep(name, seq, started, Instant.now(), status, err, params);
        if (err != null) {
            throw new RabbitMqException("consume " + opts.queue + ": " + err.getMessage(), err);
        }
        return out;
    }

    public void declareQueue(String queue, DeclareQueueOptions opts) {
        if (closed.get()) {
            throw new RabbitMqException("client is closed");
        }
        if (queue == null || queue.isEmpty()) {
            throw new RabbitMqException("empty queue name");
        }
        if (opts == null) opts = new DeclareQueueOptions();
        long seq = counter.incrementAndGet();
        String name = "declare-queue:" + queue;
        Instant started = Instant.now();
        try {
            Channel ch = channel();
            Map<String, Object> args = opts.args == null ? Map.of() : new LinkedHashMap<>(opts.args);
            ch.queueDeclare(queue, opts.durable, opts.exclusive, opts.autoDelete, args);
            recordStep(name, seq, started, Instant.now(), "passed", null, Map.of());
        } catch (Exception e) {
            String status = classify(e);
            recordStep(name, seq, started, Instant.now(), status, e, Map.of());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RabbitMqException("declare-queue " + queue + ": " + e.getMessage(), e);
        }
    }

    private Channel channel() throws IOException, TimeoutException,
        NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        synchronized (lock) {
            if (channel != null && channel.isOpen()) {
                return channel;
            }
            if (connection == null || !connection.isOpen()) {
                ConnectionFactory cf = new ConnectionFactory();
                cf.setUri(amqpUrl);
                connection = cf.newConnection();
            }
            channel = connection.createChannel();
            return channel;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        synchronized (lock) {
            if (channel != null) {
                try { channel.close(); } catch (Exception ignored) {}
                channel = null;
            }
            if (connection != null) {
                try { connection.close(); } catch (Exception ignored) {}
                connection = null;
            }
        }
    }

    private void recordStep(String name, long seq, Instant start, Instant end,
                            String status, Throwable err, Map<String, String> params) {
        Step.Builder b = Step.builder()
            .key(Telemetry.newStepKey(name, seq))
            .name(name)
            .status(status)
            .startedAt(start)
            .finishedAt(end)
            .durationMs(Math.max(0, end.toEpochMilli() - start.toEpochMilli()))
            .parameters(params == null ? Map.of() : params);
        if (err != null) {
            b.message(err.getMessage() == null ? err.getClass().getSimpleName() : err.getMessage());
        }
        recorder.record(b.build());
    }

    private byte[] marshalPayload(Object value) throws Exception {
        if (value == null) return new byte[0];
        if (value instanceof byte[]) return (byte[]) value;
        if (value instanceof CharSequence) return value.toString().getBytes(StandardCharsets.UTF_8);
        if (value instanceof JsonNode) return value.toString().getBytes(StandardCharsets.UTF_8);
        return json.writeValueAsBytes(value);
    }

    private static ConsumedMessage toConsumed(GetResponse r) {
        Map<String, String> hdrs = new LinkedHashMap<>();
        if (r.getProps() != null && r.getProps().getHeaders() != null) {
            for (Map.Entry<String, Object> e : r.getProps().getHeaders().entrySet()) {
                hdrs.put(e.getKey(), e.getValue() == null ? "" : e.getValue().toString());
            }
        }
        Instant ts = null;
        if (r.getProps() != null && r.getProps().getTimestamp() != null) {
            ts = r.getProps().getTimestamp().toInstant();
        }
        String contentType = r.getProps() != null ? r.getProps().getContentType() : null;
        return new ConsumedMessage(
            r.getEnvelope().getExchange(),
            r.getEnvelope().getRoutingKey(),
            contentType,
            r.getBody(),
            r.getEnvelope().getDeliveryTag(),
            ts,
            hdrs
        );
    }

    /**
     * Classify an AMQP driver error. Typed
     * {@link ShutdownSignalException} (and {@link IOException} wrapping
     * one) is a server-side protocol exception → {@code failed}.
     * Everything else (interrupt, plain transport IO) → {@code broken}.
     */
    static String classify(Throwable err) {
        if (err == null) return "passed";
        if (err instanceof InterruptedException) return "broken";
        if (err instanceof ShutdownSignalException) return "failed";
        if (err instanceof IOException && err.getCause() instanceof ShutdownSignalException) {
            return "failed";
        }
        return "broken";
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    /** Fluent options bag for {@link RabbitMqClient}. */
    public static final class Options {
        private StepRecorder recorder = NopRecorder.INSTANCE;
        private int payloadCap = 1024;

        public Options recorder(StepRecorder r) {
            this.recorder = r == null ? NopRecorder.INSTANCE : r;
            return this;
        }

        public Options payloadCap(int n) {
            this.payloadCap = Math.max(0, n);
            return this;
        }
    }
}
