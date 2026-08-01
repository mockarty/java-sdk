// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the Java WebSocket facet against a dependency-free embedded
 * WS echo server (handshake + two text frames + close). Mirrors the Go/Python
 * WebSocket port.
 */
public class TesterWebSocketTest {

    private ServerSocket server;
    private String base;

    @BeforeEach
    void setUp() throws IOException {
        server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        Thread th = new Thread(this::serveOnce);
        th.setDaemon(true);
        th.start();
        base = "http://127.0.0.1:" + server.getLocalPort();
    }

    @AfterEach
    void tearDown() throws IOException { server.close(); }

    // Accept one connection, complete the WS handshake, read one client frame,
    // send two text frames, then a close frame.
    private void serveOnce() {
        try (Socket s = server.accept()) {
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            String key = readHandshakeKey(in);
            String accept = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1").digest(
                            (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)));
            out.write(("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            readFrame(in);                       // the client's "hello"
            sendText(out, "{\"echo\":1}");
            sendText(out, "{\"echo\":2}");
            out.write(new byte[]{(byte) 0x88, 0x00}); // close
            out.flush();
            Thread.sleep(150);
        } catch (Exception ignored) {
        }
    }

    private static String readHandshakeKey(InputStream in) throws IOException {
        StringBuilder line = new StringBuilder();
        String key = null;
        int prev = 0, prev2 = 0, prev3 = 0, c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                String l = line.toString().trim();
                if (l.toLowerCase().startsWith("sec-websocket-key:")) {
                    key = l.substring(l.indexOf(':') + 1).trim();
                }
                line.setLength(0);
            } else if (c != '\r') {
                line.append((char) c);
            }
            if (c == '\n' && prev == '\r' && prev2 == '\n' && prev3 == '\r') break;
            prev3 = prev2; prev2 = prev; prev = c;
        }
        return key;
    }

    private static void readFrame(InputStream in) throws IOException {
        int b0 = in.read(), b1 = in.read();
        if (b0 < 0 || b1 < 0) return;
        int len = b1 & 0x7F;
        if (len == 126) { in.read(); in.read(); }
        if ((b1 & 0x80) != 0) { in.read(); in.read(); in.read(); in.read(); }
        for (int i = 0; i < len; i++) { in.read(); }
    }

    private static void sendText(OutputStream out, String s) throws IOException {
        byte[] p = s.getBytes(StandardCharsets.UTF_8);
        out.write(0x81);
        out.write(p.length & 0x7F);
        out.write(p);
        out.flush();
    }

    @Test
    void wsConnectsSendsReceivesAndExtracts() {
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.websocket("/ws").connect()
                .listen(Duration.ofSeconds(2))
                .send("hello")
                .expectConnected()
                .expectReceivedCount(2)
                .expectReceivedAtLeast(2)
                .expectMessageContains(0, "echo")
                .expectJsonPath(0, "$.echo", 1)
                .expectJsonPath(1, "$.echo", 2)
                .extract(1, "$.echo", "second")
                .done();
        t.finish();
        assertTrue(t.ok(), () -> "errors: " + t.errors());
        assertEquals("2", t.vars().get("second"));
    }

    @Test
    void wsWrongCountFails() {
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.websocket("/ws").connect()
                .listen(Duration.ofSeconds(2))
                .send("hello")
                .expectReceivedCount(5) // only 2 arrive
                .done();
        t.finish();
        assertFalse(t.ok());
    }
}
