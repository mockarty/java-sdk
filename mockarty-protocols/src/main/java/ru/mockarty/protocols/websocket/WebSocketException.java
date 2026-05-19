package ru.mockarty.protocols.websocket;

/** Unchecked exception for WebSocket transport failures. */
public final class WebSocketException extends RuntimeException {
    public WebSocketException(String message) { super(message); }
    public WebSocketException(String message, Throwable cause) { super(message, cause); }
}
