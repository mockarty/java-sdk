package ru.mockarty.protocols.sse;

/** Unchecked exception for SSE transport failures. */
public final class SseException extends RuntimeException {
    public SseException(String message) { super(message); }
    public SseException(String message, Throwable cause) { super(message, cause); }
}
