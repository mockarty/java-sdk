package ru.mockarty.protocols.rabbitmq;

/** Unchecked failure raised by {@link RabbitMqClient}. */
public class RabbitMqException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RabbitMqException(String message) {
        super(message);
    }

    public RabbitMqException(String message, Throwable cause) {
        super(message, cause);
    }
}
