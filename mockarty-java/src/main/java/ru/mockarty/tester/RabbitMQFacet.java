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
 * RabbitMQ facet. Mirrors {@code sdk/go-sdk/tester/rabbitmq.go} and the
 * Python port. The SDK ships no AMQP client deps; the user adapts any
 * client via {@link RabbitMQBroker}.
 */
public final class RabbitMQFacet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Tester t;
    private final RabbitMQBroker broker;

    RabbitMQFacet(Tester t, RabbitMQBroker broker) {
        this.t = t;
        this.broker = broker;
    }

    public RabbitMQPublishStep publish(String exchange, String routingKey) {
        t.flushPending();
        Map<String, String> v = t.snapshotVars();
        RabbitMQPublishStep s = new RabbitMQPublishStep(t, broker,
                Interpolate.apply(exchange, v),
                Interpolate.apply(routingKey, v));
        t.setPending(s);
        return s;
    }

    public RabbitMQConsumeStep consume(String queue) {
        t.flushPending();
        ConsumeOptions opts = new ConsumeOptions();
        opts.queue = Interpolate.apply(queue, t.snapshotVars());
        opts.maxMessages = 1;
        RabbitMQConsumeStep s = new RabbitMQConsumeStep(t, broker, opts);
        t.setPending(s);
        return s;
    }

    public interface RabbitMQBroker {
        void publish(String exchange, String routingKey, byte[] payload, Map<String, String> headers) throws Exception;
        List<ConsumedMessage> consume(ConsumeOptions opts) throws Exception;
    }

    public static final class ConsumeOptions {
        public String queue = "";
        public int maxMessages = 1;
        public boolean autoAck;
    }

    public static final class ConsumedMessage {
        public String exchange = "";
        public String routingKey = "";
        public byte[] body = new byte[0];
        public String contentType = "";
        public Map<String, String> headers = new HashMap<>();
    }

    public static final class RabbitMQPublishStep implements Committable {
        private final Tester t;
        private final RabbitMQBroker broker;
        private final String exchange;
        private final String routingKey;
        private final Map<String, String> headers = new HashMap<>();
        private byte[] payload = new byte[0];
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private Throwable err;
        private final List<String> failures = new ArrayList<>();

        RabbitMQPublishStep(Tester t, RabbitMQBroker broker, String exchange, String routingKey) {
            this.t = t; this.broker = broker; this.exchange = exchange; this.routingKey = routingKey;
        }

        public RabbitMQPublishStep header(String k, String v) {
            if (sent) { return fail("header() after send"); }
            headers.put(k, Interpolate.apply(v, t.snapshotVars()));
            return this;
        }

        public RabbitMQPublishStep json(Object v) {
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

        public RabbitMQPublishStep bytes(byte[] b) {
            if (sent) { return fail("bytes() after send"); }
            payload = b.clone();
            return this;
        }

        public RabbitMQPublishStep expectOK() {
            if (!ensureSent()) { return this; }
            if (err != null) { fail("expectOK: " + err.getMessage()); }
            return this;
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        private RabbitMQPublishStep fail(String msg) { failures.add(msg); return this; }

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
                broker.publish(exchange, routingKey, payload, Collections.unmodifiableMap(headers));
            } catch (Exception e) {
                err = e;
                endedAt = Instant.now();
                fail("publish: " + e.getMessage());
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
            rec.protocol = "rabbitmq";
            rec.method = "publish";
            rec.name = "publish " + exchange + "/" + routingKey;
            rec.url = exchange + "/" + routingKey;
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }

    public static final class RabbitMQConsumeStep implements Committable {
        private final Tester t;
        private final RabbitMQBroker broker;
        private final ConsumeOptions opts;
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private List<ConsumedMessage> msgs = Collections.emptyList();
        private Throwable err;
        private final List<String> failures = new ArrayList<>();

        RabbitMQConsumeStep(Tester t, RabbitMQBroker broker, ConsumeOptions opts) {
            this.t = t; this.broker = broker; this.opts = opts;
        }

        public RabbitMQConsumeStep max(int n) {
            if (sent) { return fail("max() after send"); }
            if (n > 0) { opts.maxMessages = n; }
            return this;
        }
        public RabbitMQConsumeStep autoAck(boolean b) {
            if (sent) { return fail("autoAck() after send"); }
            opts.autoAck = b;
            return this;
        }

        public RabbitMQConsumeStep expectCount(int n) {
            if (!ensureSent()) { return this; }
            if (msgs.size() != n) {
                fail("expectCount: want " + n + ", got " + msgs.size());
            }
            return this;
        }
        public RabbitMQConsumeStep expectAtLeast(int n) {
            if (!ensureSent()) { return this; }
            if (msgs.size() < n) {
                fail("expectAtLeast: want >=" + n + ", got " + msgs.size());
            }
            return this;
        }
        public RabbitMQConsumeStep expectMessageContains(int idx, String sub) {
            if (!ensureSent()) { return this; }
            if (idx < 0 || idx >= msgs.size()) {
                return fail("expectMessageContains[" + idx + "]: index out of range (len=" + msgs.size() + ")");
            }
            String body = new String(msgs.get(idx).body, StandardCharsets.UTF_8);
            if (!body.contains(sub)) {
                fail("expectMessageContains[" + idx + "]: \"" + sub + "\" not found");
            }
            return this;
        }
        public RabbitMQConsumeStep expectHeader(int idx, String k, String v) {
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
        public RabbitMQConsumeStep expectRoutingKey(int idx, String want) {
            if (!ensureSent()) { return this; }
            if (idx < 0 || idx >= msgs.size()) {
                return fail("expectRoutingKey[" + idx + "]: index out of range (len=" + msgs.size() + ")");
            }
            String got = msgs.get(idx).routingKey;
            if (!got.equals(want)) {
                fail("expectRoutingKey[" + idx + "]: want \"" + want + "\", got \"" + got + "\"");
            }
            return this;
        }
        public RabbitMQConsumeStep expectJsonPath(int idx, String path, Object want) {
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
        public RabbitMQConsumeStep extract(int idx, String path, String name) {
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

        private RabbitMQConsumeStep fail(String msg) { failures.add(msg); return this; }

        private Object evalAt(int idx, String path) throws Exception {
            if (idx < 0 || idx >= msgs.size()) {
                throw new RuntimeException("index out of range (len=" + msgs.size() + ")");
            }
            Object doc = MAPPER.readValue(msgs.get(idx).body, Object.class);
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
            rec.protocol = "rabbitmq";
            rec.method = "consume";
            rec.name = "consume " + opts.queue;
            rec.url = opts.queue;
            rec.statusOrCode = msgs.size();
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }
}
