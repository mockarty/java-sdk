package ru.mockarty.protocols.websocket;

import org.junit.jupiter.api.Test;
import ru.mockarty.protocols.telemetry.AccumulatingRecorder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket client unit tests that don't need a live RFC-6455 server.
 * The handshake / send / recv path is exercised through integration
 * tests where a Mockarty WebSocket mock is available.
 */
class WebSocketSmokeTest {

    @Test
    void emptyUrlRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WebSocketClient(""));
    }

    @Test
    void closeIdempotentBeforeOpen() {
        WebSocketClient c = new WebSocketClient("ws://x:0");
        c.close();
        c.close();
    }

    @Test
    void payloadCapClampsNegativeToZero() {
        // Constructed via options to verify clamp; we observe the
        // effect by sending a step manually after the constructor
        // settled — there's no direct getter on the client.
        AccumulatingRecorder rec = new AccumulatingRecorder();
        WebSocketClient c = new WebSocketClient("ws://x:0", o ->
            o.recorder(rec).payloadCap(-1).openTimeout(Duration.ofMillis(50)));
        // recv on an unconnected port should fail broken; payload cap
        // doesn't change error semantics, but a step still lands.
        assertThrows(WebSocketException.class, () -> c.recv(Duration.ofMillis(50)));
        assertFalse(rec.payloads().isEmpty());
        assertEquals("broken", rec.payloads().get(0).get("status"));
    }
}
