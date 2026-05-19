package ru.mockarty.protocols.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import ru.mockarty.protocols.telemetry.NopRecorder;
import ru.mockarty.protocols.telemetry.Step;
import ru.mockarty.protocols.telemetry.StepRecorder;
import ru.mockarty.protocols.telemetry.Telemetry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Thin Kafka test client over the official {@code kafka-clients} driver.
 * Produces and consumes JSON-ish payloads with per-call step capture.
 *
 * <p>One producer per client is shared across {@link #produce} calls
 * (the underlying {@code KafkaProducer} is thread-safe). A fresh
 * {@code KafkaConsumer} is opened per {@link #consume} call because
 * the official consumer is NOT thread-safe and keeping a long-lived
 * one across tests would leak group coordinator state.
 */
public final class KafkaClient implements AutoCloseable {

    private final String bootstrapServers;
    private final StepRecorder recorder;
    private final int payloadCap;
    private final String requiredAcks;
    private final Duration writeTimeout;
    private final AtomicLong counter = new AtomicLong(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object producerLock = new Object();
    private final ObjectMapper json = new ObjectMapper();
    private volatile KafkaProducer<String, byte[]> producer;

    public KafkaClient(String bootstrapServers) {
        this(bootstrapServers, opts -> {});
    }

    public KafkaClient(String bootstrapServers, Consumer<Options> configure) {
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            throw new IllegalArgumentException("mockarty kafka: empty bootstrap servers");
        }
        Options opts = new Options();
        configure.accept(opts);
        this.bootstrapServers = bootstrapServers;
        this.recorder = opts.recorder == null ? NopRecorder.INSTANCE : opts.recorder;
        this.payloadCap = Math.max(0, opts.payloadCap);
        this.requiredAcks = opts.requiredAcks;
        this.writeTimeout = opts.writeTimeout;
    }

    /** Produce one message. {@code key} may be null (round-robin
     *  partitioning); {@code headers} may be null (no headers). */
    public RecordMetadata produce(String topic, String key, Object payload, Map<String, String> headers) {
        if (closed.get()) {
            throw new KafkaException("client is closed");
        }
        if (topic == null || topic.isEmpty()) {
            throw new KafkaException("empty topic");
        }
        long seq = counter.incrementAndGet();
        String name = "produce:" + topic;
        Instant started = Instant.now();
        byte[] body;
        try {
            body = marshalPayload(payload);
        } catch (Exception e) {
            KafkaException kex = new KafkaException("marshal payload: " + e.getMessage(), e);
            recordStep(name, seq, started, Instant.now(), "failed", kex, null);
            throw kex;
        }
        ProducerRecord<String, byte[]> rec = new ProducerRecord<>(topic, key, body);
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                rec.headers().add(e.getKey(),
                    e.getValue() == null ? null : e.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }
        try {
            KafkaProducer<String, byte[]> p = ensureProducer();
            RecordMetadata md = p.send(rec).get(writeTimeout.toMillis(), TimeUnit.MILLISECONDS);
            Map<String, String> params = new LinkedHashMap<>();
            params.put("key", key == null ? "" : key);
            params.put("payload", Telemetry.capPreview(body, payloadCap));
            params.put("payload_size", Integer.toString(body.length));
            recordStep(name, seq, started, Instant.now(), "passed", null, params);
            return md;
        } catch (Exception e) {
            Throwable cause = unwrap(e);
            String status = classify(cause);
            Map<String, String> params = new LinkedHashMap<>();
            params.put("key", key == null ? "" : key);
            params.put("payload", Telemetry.capPreview(body, payloadCap));
            params.put("payload_size", Integer.toString(body.length));
            recordStep(name, seq, started, Instant.now(), status, cause, params);
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new KafkaException("produce " + topic + ": " + cause.getMessage(), cause);
        }
    }

    /** Consume up to {@code opts.maxMessages} messages. Records one
     *  step per call (not per message) so the TCM timeline is concise. */
    public List<ConsumedMessage> consume(ConsumeOptions opts) {
        if (closed.get()) {
            throw new KafkaException("client is closed");
        }
        if (opts == null || opts.topic == null || opts.topic.isEmpty()) {
            throw new KafkaException("empty topic");
        }
        if (opts.groupId == null || opts.groupId.isEmpty()) {
            opts.groupId = "mockarty-test-" + UUID.randomUUID();
        }
        int maxMessages = opts.maxMessages <= 0 ? 1 : opts.maxMessages;
        long seq = counter.incrementAndGet();
        String name = "consume:" + opts.topic;
        Instant started = Instant.now();
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, opts.groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "latest".equalsIgnoreCase(opts.startOffset) ? "latest" : "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        java.util.List<ConsumedMessage> out = new java.util.ArrayList<>(maxMessages);
        String status = "passed";
        Throwable err = null;
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(opts.topic));
            long deadline = System.nanoTime() + Duration.ofMillis(Math.max(opts.pollTimeoutMs, 1)).toNanos();
            while (out.size() < maxMessages && System.nanoTime() < deadline) {
                ConsumerRecords<String, byte[]> records = consumer.poll(
                    Duration.ofMillis(Math.min(500, opts.pollTimeoutMs)));
                for (ConsumerRecord<String, byte[]> r : records) {
                    out.add(toConsumed(r));
                    if (out.size() >= maxMessages) break;
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
        params.put("group", opts.groupId);
        recordStep(name, seq, started, Instant.now(), status, err, params);
        if (err != null) {
            throw new KafkaException("consume " + opts.topic + ": " + err.getMessage(), err);
        }
        return out;
    }

    private KafkaProducer<String, byte[]> ensureProducer() {
        KafkaProducer<String, byte[]> p = producer;
        if (p != null) return p;
        synchronized (producerLock) {
            if (producer != null) return producer;
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, requiredAcks);
            props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                Integer.toString((int) Math.max(writeTimeout.toMillis(), 1)));
            props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                Integer.toString((int) Math.max(writeTimeout.toMillis() / 2, 1)));
            producer = new KafkaProducer<>(props);
            return producer;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        synchronized (producerLock) {
            if (producer != null) {
                try {
                    producer.close(Duration.ofSeconds(2));
                } finally {
                    producer = null;
                }
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

    private static ConsumedMessage toConsumed(ConsumerRecord<String, byte[]> r) {
        Map<String, String> hdrs = new LinkedHashMap<>();
        for (Header h : r.headers()) {
            hdrs.put(h.key(), h.value() == null ? "" : new String(h.value(), StandardCharsets.UTF_8));
        }
        return new ConsumedMessage(
            r.topic(),
            r.key(),
            r.value(),
            r.partition(),
            r.offset(),
            Instant.ofEpochMilli(r.timestamp()),
            hdrs
        );
    }

    private static Throwable unwrap(Throwable e) {
        if (e instanceof ExecutionException && e.getCause() != null) {
            return e.getCause();
        }
        return e;
    }

    /**
     * Map a Kafka driver error to a telemetry status. The decision tree
     * mirrors the Go SDK conservatively — broker-unreachable / interrupt
     * land in {@code broken} so flaky CI doesn't paint runs red.
     */
    static String classify(Throwable err) {
        if (err == null) return "passed";
        if (err instanceof InterruptedException) return "broken";
        if (err instanceof InterruptException) return "broken";
        if (err instanceof TimeoutException) return "broken";
        if (err instanceof java.util.concurrent.TimeoutException) return "broken";
        if (err instanceof RetriableException) return "failed";
        if (err instanceof ConfigException) return "failed";
        // Typed kafka APIException subclasses come from
        // org.apache.kafka.common.errors — treat the typed family as
        // assertion failures, everything else (transport, dns) as broken.
        if (err instanceof org.apache.kafka.common.errors.ApiException) return "failed";
        return "broken";
    }

    /** Fluent options bag for {@link KafkaClient}. */
    public static final class Options {
        private StepRecorder recorder = NopRecorder.INSTANCE;
        private String requiredAcks = "all";
        private Duration writeTimeout = Duration.ofSeconds(10);
        private int payloadCap = 1024;

        public Options recorder(StepRecorder r) {
            this.recorder = r == null ? NopRecorder.INSTANCE : r;
            return this;
        }

        /** {@code "all" | "1" | "0"} — translated straight onto
         *  {@code acks} in the underlying producer config. Defaults to {@code "all"}. */
        public Options requiredAcks(String a) {
            if (a == null) return this;
            if (!"all".equals(a) && !"1".equals(a) && !"0".equals(a)) {
                throw new IllegalArgumentException(
                    "mockarty kafka: requiredAcks must be one of \"all\", \"1\", \"0\"");
            }
            this.requiredAcks = a;
            return this;
        }

        public Options writeTimeout(Duration d) {
            if (d != null && !d.isZero() && !d.isNegative()) {
                this.writeTimeout = d;
            }
            return this;
        }

        public Options payloadCap(int n) {
            this.payloadCap = Math.max(0, n);
            return this;
        }
    }
}
