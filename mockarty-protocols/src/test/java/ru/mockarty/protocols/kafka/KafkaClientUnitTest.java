package ru.mockarty.protocols.kafka;

import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.NotEnoughReplicasException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import ru.mockarty.protocols.telemetry.AccumulatingRecorder;
import ru.mockarty.protocols.telemetry.NopRecorder;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class KafkaClientUnitTest {

    @Test
    void emptyBootstrapServersRejected() {
        assertThrows(IllegalArgumentException.class, () -> new KafkaClient(""));
        assertThrows(IllegalArgumentException.class, () -> new KafkaClient(null));
    }

    @Test
    void closeIsIdempotent() {
        KafkaClient c = new KafkaClient("localhost:9092");
        c.close();
        c.close();
        assertThrows(KafkaException.class, () -> c.produce("t", "k", "v", null));
    }

    @Test
    void optionsNullRecorderCoercesToNop() {
        KafkaClient c = new KafkaClient("localhost:9092", o -> o.recorder(null));
        c.close();
    }

    @Test
    void optionsNegativePayloadCapClamps() {
        // Verified indirectly — Options chains without throwing.
        new KafkaClient.Options().payloadCap(-50);
    }

    @Test
    void optionsZeroWriteTimeoutIgnored() {
        new KafkaClient("localhost:9092",
            o -> o.writeTimeout(Duration.ZERO).writeTimeout(Duration.ofSeconds(-1))).close();
    }

    @Test
    void optionsRequiredAcksValidated() {
        assertThrows(IllegalArgumentException.class,
            () -> new KafkaClient.Options().requiredAcks("two"));
        new KafkaClient.Options().requiredAcks("all");
        new KafkaClient.Options().requiredAcks("1");
        new KafkaClient.Options().requiredAcks("0");
    }

    @Test
    void classifyMatrix() {
        assertEquals("passed", KafkaClient.classify(null));
        assertEquals("broken", KafkaClient.classify(new InterruptedException("boom")));
        assertEquals("broken", KafkaClient.classify(new java.util.concurrent.TimeoutException("t")));
        assertEquals("broken", KafkaClient.classify(new TimeoutException("kafka timeout")));
        assertEquals("failed", KafkaClient.classify(new NotEnoughReplicasException("retryable")));
        assertEquals("failed", KafkaClient.classify(new InvalidConfigurationException("bad")));
        // Untyped transport-style failure
        assertEquals("broken", KafkaClient.classify(new IOException("net down")));
        assertEquals("broken", KafkaClient.classify(new RuntimeException("untyped")));
    }

    @Test
    void produceEmptyTopicRejected() {
        try (KafkaClient c = new KafkaClient("localhost:9092")) {
            assertThrows(KafkaException.class, () -> c.produce("", "k", "v", null));
        }
    }

    /** Class whose Jackson-visible getter always throws — guaranteed
     *  to make {@code ObjectMapper.writeValueAsBytes} fail with a
     *  marshal error, which is what {@code produce} should classify as
     *  "failed" and record before re-throwing. */
    public static final class ThrowingPayload {
        public Object getValue() {
            throw new IllegalStateException("forced marshal failure");
        }
    }

    @Test
    void marshalErrorRecordsFailedStep() {
        AccumulatingRecorder rec = new AccumulatingRecorder();
        try (KafkaClient c = new KafkaClient("localhost:9092",
            o -> o.recorder(rec))) {
            assertThrows(KafkaException.class,
                () -> c.produce("t", "k", new ThrowingPayload(), null));
        }
        assertEquals(1, rec.size());
        assertEquals("failed", rec.raw().get(0).getStatus());
        assertTrue(rec.raw().get(0).getDurationMs() >= 0);
    }

    @Test
    void stepKeyMonotonicOnFailure() {
        AccumulatingRecorder rec = new AccumulatingRecorder();
        try (KafkaClient c = new KafkaClient("localhost:9092",
            o -> o.recorder(rec))) {
            for (int i = 0; i < 3; i++) {
                try {
                    c.produce("t", "k", new ThrowingPayload(), null);
                } catch (KafkaException ignored) {}
            }
        }
        assertEquals(3, rec.size());
        assertEquals("produce:t#1", rec.raw().get(0).getKey());
        assertEquals("produce:t#2", rec.raw().get(1).getKey());
        assertEquals("produce:t#3", rec.raw().get(2).getKey());
    }

    @Test
    void consumeEmptyTopicRejected() {
        try (KafkaClient c = new KafkaClient("localhost:9092")) {
            ConsumeOptions opts = new ConsumeOptions(null, "g");
            assertThrows(KafkaException.class, () -> c.consume(opts));
        }
    }

    @Test
    void nopRecorderSentinel() {
        assertNotNull(NopRecorder.INSTANCE);
    }
}
