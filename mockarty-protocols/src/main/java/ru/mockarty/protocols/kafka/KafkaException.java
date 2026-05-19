package ru.mockarty.protocols.kafka;

/**
 * Unchecked failure raised by {@link KafkaClient}. Wraps producer /
 * consumer errors plus payload marshalling failures. The underlying
 * {@code org.apache.kafka.*} exception is preserved via
 * {@link #getCause()} so tests can pattern-match by type when needed.
 */
public class KafkaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KafkaException(String message) {
        super(message);
    }

    public KafkaException(String message, Throwable cause) {
        super(message, cause);
    }
}
