// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Java Tester Kafka + RabbitMQ facet coverage via in-memory fakes. */
public class TesterBrokersTest {

    // ── Kafka fake ────────────────────────────────────────────────────

    static final class FakeKafka implements KafkaFacet.KafkaBroker {
        final Map<String, List<KafkaFacet.ConsumedMessage>> topics = new HashMap<>();
        Exception produceErr;
        Exception consumeErr;

        @Override
        public void produce(String topic, String key, byte[] payload, Map<String, String> headers) throws Exception {
            if (produceErr != null) { throw produceErr; }
            KafkaFacet.ConsumedMessage m = new KafkaFacet.ConsumedMessage();
            m.topic = topic;
            m.key = key;
            m.value = payload.clone();
            m.headers = new HashMap<>(headers);
            List<KafkaFacet.ConsumedMessage> list = topics.computeIfAbsent(topic, k -> new ArrayList<>());
            m.offset = list.size();
            list.add(m);
        }

        @Override
        public List<KafkaFacet.ConsumedMessage> consume(KafkaFacet.ConsumeOptions opts) throws Exception {
            if (consumeErr != null) { throw consumeErr; }
            List<KafkaFacet.ConsumedMessage> all = topics.getOrDefault(opts.topic, new ArrayList<>());
            int start = (int) Math.max(opts.startOffset, 0);
            if (start > all.size()) { start = 0; }
            int end = Math.min(start + opts.maxMessages, all.size());
            return new ArrayList<>(all.subList(start, end));
        }
    }

    @Test
    void kafkaProduceConsumeRoundTrip() {
        FakeKafka b = new FakeKafka();
        Tester t = new Tester.Builder().build();
        t.kafka(b).produce("orders", "user-42")
                .json(Map.of("id", 1, "status", "created"))
                .expectOK();
        t.kafka(b).consume("orders")
                .max(5)
                .expectCount(1)
                .expectFirstOffsetAtLeast(0)
                .expectMessageContains(0, "created")
                .expectJsonPath(0, "$.id", 1)
                .extract(0, "$.status", "lastStatus");
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
        assertEquals("created", t.vars().get("lastStatus"));
    }

    @Test
    void kafkaProduceErrorPropagates() {
        FakeKafka b = new FakeKafka();
        b.produceErr = new RuntimeException("broker unreachable");
        Tester t = new Tester.Builder().build();
        t.kafka(b).produce("orders", "k").json(Map.of("x", 1)).expectOK();
        assertFalse(t.ok());
    }

    @Test
    void kafkaInterpolationAcrossChains() {
        FakeKafka b = new FakeKafka();
        Tester t = new Tester.Builder().build();
        t.setVar("user", "42");
        t.kafka(b).produce("orders", "k-{{user}}")
                .header("X-User", "{{user}}")
                .json(Map.of("userID", "{{user}}"))
                .expectOK();
        t.kafka(b).consume("orders").max(1)
                .expectCount(1)
                .expectHeader(0, "X-User", "42")
                .expectJsonPath(0, "$.userID", "42");
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
        assertEquals("k-42", b.topics.get("orders").get(0).key);
    }

    @Test
    void kafkaStartOffsetSkipsHistory() {
        FakeKafka b = new FakeKafka();
        Tester t = new Tester.Builder().build();
        for (int i = 0; i < 5; i++) {
            t.kafka(b).produce("topic", "k").json(Map.of("i", i)).expectOK();
        }
        t.kafka(b).consume("topic")
                .startOffset(3)
                .max(10)
                .expectCount(2)
                .expectFirstOffsetAtLeast(3);
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
    }

    @Test
    void kafkaIndexOutOfRange() {
        FakeKafka b = new FakeKafka();
        Tester t = new Tester.Builder().build();
        t.kafka(b).consume("empty").max(1)
                .expectMessageContains(0, "x")
                .expectHeader(0, "x", "y")
                .expectJsonPath(0, "$.a", 1)
                .extract(0, "$.a", "v");
        t.finish();
        assertFalse(t.ok());
        assertTrue(t.errors().size() >= 4, () -> "errs=" + t.errors());
    }

    // ── RabbitMQ fake ─────────────────────────────────────────────────

    static final class FakeRabbit implements RabbitMQFacet.RabbitMQBroker {
        final Map<String, List<RabbitMQFacet.ConsumedMessage>> queues = new HashMap<>();
        Exception publishErr;
        Exception consumeErr;

        @Override
        public void publish(String exchange, String routingKey, byte[] payload, Map<String, String> headers) throws Exception {
            if (publishErr != null) { throw publishErr; }
            RabbitMQFacet.ConsumedMessage m = new RabbitMQFacet.ConsumedMessage();
            m.exchange = exchange;
            m.routingKey = routingKey;
            m.body = payload.clone();
            m.headers = new HashMap<>(headers);
            m.contentType = "application/json";
            queues.computeIfAbsent(routingKey, k -> new ArrayList<>()).add(m);
        }

        @Override
        public List<RabbitMQFacet.ConsumedMessage> consume(RabbitMQFacet.ConsumeOptions opts) throws Exception {
            if (consumeErr != null) { throw consumeErr; }
            List<RabbitMQFacet.ConsumedMessage> all = queues.getOrDefault(opts.queue, new ArrayList<>());
            int n = Math.min(opts.maxMessages, all.size());
            List<RabbitMQFacet.ConsumedMessage> out = new ArrayList<>(all.subList(0, n));
            // mimic non-AutoAck — remove consumed
            queues.put(opts.queue, new ArrayList<>(all.subList(n, all.size())));
            return out;
        }
    }

    @Test
    void rabbitMQPublishConsumeRoundTrip() {
        FakeRabbit b = new FakeRabbit();
        Tester t = new Tester.Builder().build();
        t.rabbitmq(b).publish("events", "user.updated")
                .json(Map.of("id", 1, "status", "ok"))
                .expectOK();
        t.rabbitmq(b).consume("user.updated").max(5)
                .expectCount(1)
                .expectRoutingKey(0, "user.updated")
                .expectMessageContains(0, "ok")
                .expectJsonPath(0, "$.id", 1)
                .extract(0, "$.status", "lastStatus");
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
        assertEquals("ok", t.vars().get("lastStatus"));
    }

    @Test
    void rabbitMQPublishErrorPropagates() {
        FakeRabbit b = new FakeRabbit();
        b.publishErr = new RuntimeException("connection refused");
        Tester t = new Tester.Builder().build();
        t.rabbitmq(b).publish("ex", "rk").json(Map.of("x", 1)).expectOK();
        assertFalse(t.ok());
    }

    @Test
    void rabbitMQAutoAckAndHeader() {
        FakeRabbit b = new FakeRabbit();
        Tester t = new Tester.Builder().build();
        t.rabbitmq(b).publish("ex", "q")
                .header("trace", "abc")
                .json(Map.of("x", 1))
                .expectOK();
        t.rabbitmq(b).consume("q").autoAck(true).max(1)
                .expectCount(1)
                .expectHeader(0, "trace", "abc");
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
    }

    @Test
    void rabbitMQInterpolation() {
        FakeRabbit b = new FakeRabbit();
        Tester t = new Tester.Builder().build();
        t.setVar("user", "alice");
        t.rabbitmq(b).publish("ex-{{user}}", "rk-{{user}}")
                .header("X-User", "{{user}}")
                .json(Map.of("name", "{{user}}"))
                .expectOK();
        t.rabbitmq(b).consume("rk-alice").max(1)
                .expectCount(1)
                .expectHeader(0, "X-User", "alice")
                .expectJsonPath(0, "$.name", "alice");
        t.finish();
        assertTrue(t.ok(), () -> "errs=" + t.errors());
    }

    @Test
    void rabbitMQIndexOutOfRange() {
        FakeRabbit b = new FakeRabbit();
        Tester t = new Tester.Builder().build();
        t.rabbitmq(b).consume("empty").max(1)
                .expectMessageContains(0, "x")
                .expectHeader(0, "x", "y")
                .expectRoutingKey(0, "z")
                .expectJsonPath(0, "$.x", 1)
                .extract(0, "$.x", "v");
        t.finish();
        assertFalse(t.ok());
        assertTrue(t.errors().size() >= 5, () -> "errs=" + t.errors());
    }
}
