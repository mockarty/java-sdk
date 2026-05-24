// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka facet. Mirrors {@code sdk/go-sdk/tester/kafka.go} and the
 * Python port. The SDK ships no Kafka client deps; the user adapts
 * any kafka-clients-style driver via the {@link KafkaBroker} interface.
 */
public final class KafkaFacet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Tester t;
    private final KafkaBroker broker;

    KafkaFacet(Tester t, KafkaBroker broker) {
        this.t = t;
        this.broker = broker;
    }

    public KafkaProduceStep produce(String topic, String key) {
        t.flushPending();
        Map<String, String> v = t.snapshotVars();
        KafkaProduceStep s = new KafkaProduceStep(t, broker,
                Interpolate.apply(topic, v),
                Interpolate.apply(key, v));
        t.setPending(s);
        return s;
    }

    public KafkaConsumeStep consume(String topic) {
        t.flushPending();
        ConsumeOptions opts = new ConsumeOptions();
        opts.topic = Interpolate.apply(topic, t.snapshotVars());
        opts.maxMessages = 1;
        KafkaConsumeStep s = new KafkaConsumeStep(t, broker, opts);
        t.setPending(s);
        return s;
    }

    /** Minimal contract the Tester needs from a Kafka client. */
    public interface KafkaBroker {
        void produce(String topic, String key, byte[] payload, Map<String, String> headers) throws Exception;
        List<ConsumedMessage> consume(ConsumeOptions opts) throws Exception;
    }

    public static final class ConsumeOptions {
        public String topic = "";
        public String groupId = "";
        public int maxMessages = 1;
        public long startOffset;
    }

    public static final class ConsumedMessage {
        public String topic = "";
        public String key = "";
        public byte[] value = new byte[0];
        public int partition;
        public long offset;
        public Map<String, String> headers = new HashMap<>();
    }

    public static final class KafkaProduceStep implements Committable {
        private final Tester t;
        private final KafkaBroker broker;
        private final String topic;
        private final String key;
        private final Map<String, String> headers = new HashMap<>();
        private byte[] payload = new byte[0];
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private Throwable err;
        private final List<String> failures = new ArrayList<>();

        KafkaProduceStep(Tester t, KafkaBroker broker, String topic, String key) {
            this.t = t; this.broker = broker; this.topic = topic; this.key = key;
        }

        public KafkaProduceStep header(String k, String v) {
            if (sent) { return fail("header() after send"); }
            headers.put(k, Interpolate.apply(v, t.snapshotVars()));
            return this;
        }

        public KafkaProduceStep json(Object v) {
            if (sent) { return fail("json() after send"); }
            try {
                String raw = MAPPER.writeValueAsString(v);
                payload = Interpolate.apply(raw, t.snapshotVars()).getBytes(StandardCharsets.UTF_8);
            } catch (JsonProcessingException e) {
                abortChain = true;
                return fail("marshal payload: " + e.getMessage());
            }
            return this;
        }

        public KafkaProduceStep bytes(byte[] b) {
            if (sent) { return fail("bytes() after send"); }
            payload = b.clone();
            return this;
        }

        public KafkaProduceStep expectOK() {
            if (!ensureSent()) { return this; }
            if (err != null) { fail("expectOK: " + err.getMessage()); }
            return this;
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        private KafkaProduceStep fail(String msg) { failures.add(msg); return this; }

        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            startedAt = Instant.now();
            try {
                broker.produce(topic, key, payload, Collections.unmodifiableMap(headers));
            } catch (Exception e) {
                err = e;
                endedAt = Instant.now();
                fail("produce: " + e.getMessage());
                abortChain = true;
                return false;
            }
            endedAt = Instant.now();
            return true;
        }

        @Override
        public void commit() {
            if (committed) { return; }
            committed = true;
            if (!sent) { ensureSent(); }
            StepRecord rec = new StepRecord();
            rec.protocol = "kafka";
            rec.method = "produce";
            rec.name = "produce " + topic;
            rec.url = topic;
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }

    public static final class KafkaConsumeStep implements Committable {
        private final Tester t;
        private final KafkaBroker broker;
        private final ConsumeOptions opts;
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private List<ConsumedMessage> msgs = Collections.emptyList();
        private Throwable err;
        private final List<String> failures = new ArrayList<>();

        KafkaConsumeStep(Tester t, KafkaBroker broker, ConsumeOptions opts) {
            this.t = t; this.broker = broker; this.opts = opts;
        }

        public KafkaConsumeStep group(String g) {
            if (sent) { return fail("group() after send"); }
            opts.groupId = g;
            return this;
        }
        public KafkaConsumeStep max(int n) {
            if (sent) { return fail("max() after send"); }
            if (n > 0) { opts.maxMessages = n; }
            return this;
        }
        public KafkaConsumeStep startOffset(long o) {
            if (sent) { return fail("startOffset() after send"); }
            opts.startOffset = o;
            return this;
        }

        public KafkaConsumeStep expectCount(int n) {
            if (!ensureSent()) { return this; }
            if (msgs.size() != n) {
                fail("expectCount: want " + n + ", got " + msgs.size());
            }
            return this;
        }
        public KafkaConsumeStep expectAtLeast(int n) {
            if (!ensureSent()) { return this; }
            if (msgs.size() < n) {
                fail("expectAtLeast: want >=" + n + ", got " + msgs.size());
            }
            return this;
        }
        public KafkaConsumeStep expectFirstOffsetAtLeast(long o) {
            if (!ensureSent()) { return this; }
            if (msgs.isEmpty()) { return fail("expectFirstOffsetAtLeast: no messages"); }
            if (msgs.get(0).offset < o) {
                fail("expectFirstOffsetAtLeast: want >=" + o + ", got " + msgs.get(0).offset);
            }
            return this;
        }
        public KafkaConsumeStep expectMessageContains(int idx, String sub) {
            if (!ensureSent()) { return this; }
            if (idx < 0 || idx >= msgs.size()) {
                return fail("expectMessageContains[" + idx + "]: index out of range (len=" + msgs.size() + ")");
            }
            String body = new String(msgs.get(idx).value, StandardCharsets.UTF_8);
            if (!body.contains(sub)) {
                fail("expectMessageContains[" + idx + "]: \"" + sub + "\" not found");
            }
            return this;
        }
        public KafkaConsumeStep expectHeader(int idx, String k, String v) {
            if (!ensureSent()) { return this; }
            if (idx < 0 || idx >= msgs.size()) {
                return fail("expectHeader[" + idx + "]: index out of range (len=" + msgs.size() + ")");
            }
            String got = msgs.get(idx).headers.getOrDefault(k, "");
            if (!got.equals(v)) {
                fail("expectHeader[" + idx + "] " + k + ": want \"" + v + "\", got \"" + got + "\"");
            }
            return this;
        }
        public KafkaConsumeStep expectJsonPath(int idx, String path, Object want) {
            if (!ensureSent()) { return this; }
            try {
                Object got = evalAt(idx, path);
                if (!JsonPath.equalsLoose(got, want)) {
                    fail("expectJsonPath[" + idx + "] " + path + ": want " + want + ", got " + got);
                }
            } catch (Exception e) {
                fail("expectJsonPath[" + idx + "] " + path + ": " + e.getMessage());
            }
            return this;
        }
        public KafkaConsumeStep extract(int idx, String path, String name) {
            if (!ensureSent()) { return this; }
            try {
                Object got = evalAt(idx, path);
                t.setVar(name, TesterScalar.stringify(got));
            } catch (Exception e) {
                fail("extract[" + idx + "] " + path + ": " + e.getMessage());
            }
            return this;
        }
        public List<ConsumedMessage> messages() {
            ensureSent();
            return new ArrayList<>(msgs);
        }
        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        private KafkaConsumeStep fail(String msg) { failures.add(msg); return this; }

        private Object evalAt(int idx, String path) throws Exception {
            if (idx < 0 || idx >= msgs.size()) {
                throw new RuntimeException("index out of range (len=" + msgs.size() + ")");
            }
            Object doc = MAPPER.readValue(msgs.get(idx).value, Object.class);
            return JsonPath.resolve(doc, path);
        }

        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            startedAt = Instant.now();
            try {
                msgs = broker.consume(opts);
                if (msgs == null) { msgs = Collections.emptyList(); }
            } catch (Exception e) {
                err = e;
                endedAt = Instant.now();
                fail("consume: " + e.getMessage());
                return true;
            }
            endedAt = Instant.now();
            return true;
        }

        @Override
        public void commit() {
            if (committed) { return; }
            committed = true;
            if (!sent) { ensureSent(); }
            StepRecord rec = new StepRecord();
            rec.protocol = "kafka";
            rec.method = "consume";
            rec.name = "consume " + opts.topic;
            rec.url = opts.topic;
            rec.statusOrCode = msgs.size();
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }
}
