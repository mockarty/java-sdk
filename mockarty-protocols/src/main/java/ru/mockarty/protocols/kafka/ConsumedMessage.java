package ru.mockarty.protocols.kafka;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/** Immutable record of one consumed Kafka message. */
public final class ConsumedMessage {

    private final String topic;
    private final String key;
    private final byte[] value;
    private final int partition;
    private final long offset;
    private final Instant timestamp;
    private final Map<String, String> headers;

    public ConsumedMessage(String topic, String key, byte[] value, int partition,
                           long offset, Instant timestamp, Map<String, String> headers) {
        this.topic = topic;
        this.key = key;
        this.value = value == null ? new byte[0] : value.clone();
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
        this.headers = headers == null ? Collections.emptyMap() : Map.copyOf(headers);
    }

    public String topic() { return topic; }
    public String key() { return key; }
    public byte[] value() { return value.clone(); }
    public int partition() { return partition; }
    public long offset() { return offset; }
    public Instant timestamp() { return timestamp; }
    public Map<String, String> headers() { return headers; }
}
