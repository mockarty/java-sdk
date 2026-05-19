package ru.mockarty.protocols.rabbitmq;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/** Immutable record of one consumed AMQP message. */
public final class ConsumedMessage {

    private final String exchange;
    private final String routingKey;
    private final String contentType;
    private final byte[] body;
    private final long deliveryTag;
    private final Instant timestamp;
    private final Map<String, String> headers;

    public ConsumedMessage(String exchange, String routingKey, String contentType, byte[] body,
                           long deliveryTag, Instant timestamp, Map<String, String> headers) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.contentType = contentType;
        this.body = body == null ? new byte[0] : body.clone();
        this.deliveryTag = deliveryTag;
        this.timestamp = timestamp;
        this.headers = headers == null ? Collections.emptyMap() : Map.copyOf(headers);
    }

    public String exchange() { return exchange; }
    public String routingKey() { return routingKey; }
    public String contentType() { return contentType; }
    public byte[] body() { return body.clone(); }
    public long deliveryTag() { return deliveryTag; }
    public Instant timestamp() { return timestamp; }
    public Map<String, String> headers() { return headers; }
}
