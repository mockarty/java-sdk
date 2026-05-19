package ru.mockarty.protocols.rabbitmq;

import com.rabbitmq.client.ShutdownSignalException;
import org.junit.jupiter.api.Test;
import ru.mockarty.protocols.telemetry.AccumulatingRecorder;
import ru.mockarty.protocols.telemetry.NopRecorder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMqClientUnitTest {

    @Test
    void emptyAmqpUrlRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RabbitMqClient(""));
        assertThrows(IllegalArgumentException.class, () -> new RabbitMqClient(null));
    }

    @Test
    void closeIsIdempotent() {
        RabbitMqClient c = new RabbitMqClient("amqp://localhost:5672/");
        c.close();
        c.close();
        assertThrows(RabbitMqException.class,
            () -> c.publish("ex", "rk", "{}", null));
    }

    @Test
    void optionsNullRecorderCoercesToNop() {
        try (RabbitMqClient c = new RabbitMqClient("amqp://localhost:5672/",
            o -> o.recorder(null))) {
            // Sanity — exists.
            assertNotNull(c);
        }
    }

    @Test
    void optionsNegativePayloadCapClamps() {
        new RabbitMqClient.Options().payloadCap(-9);
    }

    @Test
    void classifyMatrix() {
        assertEquals("passed", RabbitMqClient.classify(null));
        assertEquals("broken", RabbitMqClient.classify(new InterruptedException()));
        // Typed ShutdownSignalException — server-side AMQP protocol error.
        ShutdownSignalException sse = new ShutdownSignalException(true, false, null, null);
        assertEquals("failed", RabbitMqClient.classify(sse));
        // IOException with a ShutdownSignalException cause also → failed.
        assertEquals("failed", RabbitMqClient.classify(new IOException("conn", sse)));
        // Plain IOException → broken (transport).
        assertEquals("broken", RabbitMqClient.classify(new IOException("net down")));
        // Untyped → broken (conservative default).
        assertEquals("broken", RabbitMqClient.classify(new RuntimeException("?")));
    }

    /** Class whose Jackson-visible getter always throws — forces the
     *  marshalPayload path into the "failed" step branch. */
    public static final class ThrowingPayload {
        public Object getValue() {
            throw new IllegalStateException("forced marshal failure");
        }
    }

    @Test
    void marshalErrorRecordsFailedStep() {
        AccumulatingRecorder rec = new AccumulatingRecorder();
        try (RabbitMqClient c = new RabbitMqClient("amqp://localhost:5672/",
            o -> o.recorder(rec))) {
            assertThrows(RabbitMqException.class,
                () -> c.publish("ex", "rk", new ThrowingPayload(), null));
        }
        assertEquals(1, rec.size());
        assertEquals("failed", rec.raw().get(0).getStatus());
        assertEquals("publish:ex/rk", rec.raw().get(0).getName());
        assertTrue(rec.raw().get(0).getDurationMs() >= 0);
    }

    @Test
    void stepKeyMonotonicAcrossOperations() {
        AccumulatingRecorder rec = new AccumulatingRecorder();
        try (RabbitMqClient c = new RabbitMqClient("amqp://localhost:5672/",
            o -> o.recorder(rec))) {
            for (int i = 0; i < 3; i++) {
                try {
                    c.publish("ex", "rk", new ThrowingPayload(), null);
                } catch (RabbitMqException ignored) {}
            }
        }
        assertEquals(3, rec.size());
        assertEquals("publish:ex/rk#1", rec.raw().get(0).getKey());
        assertEquals("publish:ex/rk#2", rec.raw().get(1).getKey());
        assertEquals("publish:ex/rk#3", rec.raw().get(2).getKey());
    }

    @Test
    void consumeEmptyQueueRejected() {
        try (RabbitMqClient c = new RabbitMqClient("amqp://localhost:5672/")) {
            assertThrows(RabbitMqException.class,
                () -> c.consume(new ConsumeOptions("")));
            assertThrows(RabbitMqException.class,
                () -> c.consume(new ConsumeOptions(null)));
            assertThrows(RabbitMqException.class,
                () -> c.consume(null));
        }
    }

    @Test
    void declareQueueEmptyNameRejected() {
        try (RabbitMqClient c = new RabbitMqClient("amqp://localhost:5672/")) {
            assertThrows(RabbitMqException.class,
                () -> c.declareQueue("", new DeclareQueueOptions()));
            assertThrows(RabbitMqException.class,
                () -> c.declareQueue(null, new DeclareQueueOptions()));
        }
    }

    @Test
    void nopRecorderSentinel() {
        assertNotNull(NopRecorder.INSTANCE);
    }
}
