// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** URL normalization for the Socket.IO client (no network). */
public class SocketIONormalizeTest {

    @Test
    void normalizeUrl() {
        assertEquals("ws://h:8080/socket.io/?EIO=4&transport=websocket",
                SocketIOFacet.SocketIOConn.normalizeUrl("http://h:8080"));
        assertEquals("wss://h/socket.io/?EIO=4&transport=websocket",
                SocketIOFacet.SocketIOConn.normalizeUrl("https://h"));
        assertEquals("ws://h/socket.io/?EIO=4&transport=websocket",
                SocketIOFacet.SocketIOConn.normalizeUrl("ws://h/socket.io/"));
        assertEquals("ws://h/socket.io/?EIO=4&transport=websocket",
                SocketIOFacet.SocketIOConn.normalizeUrl("ws://h/socket.io/?EIO=4&transport=websocket"));
        assertEquals("ws://h/socket.io/?token=x&EIO=4&transport=websocket",
                SocketIOFacet.SocketIOConn.normalizeUrl("http://h/socket.io/?token=x"));
    }
}
