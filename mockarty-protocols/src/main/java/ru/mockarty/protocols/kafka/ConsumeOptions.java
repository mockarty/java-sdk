package ru.mockarty.protocols.kafka;

/**
 * Per-call options for {@link KafkaClient#consume(ConsumeOptions)}.
 *
 * <p>Mutable POJO — callers set the fields they care about and pass
 * the instance straight into {@code consume}. {@code maxMessages} is
 * clamped to {@code 1} when zero or negative so most CI tests can
 * leave it untouched.
 */
public final class ConsumeOptions {

    /** Target topic. Required. */
    public String topic;
    /** Consumer group id. Required for offset tracking — pick a
     *  short unique-per-test value to avoid cross-test interference. */
    public String groupId;
    /** Cap on messages pulled before returning. Defaults to {@code 1}. */
    public int maxMessages = 1;
    /** First offset to seek to when the group has no committed offset.
     *  Either {@code "earliest"} or {@code "latest"}. */
    public String startOffset = "earliest";
    /** Per-poll timeout in millis. Defaults to {@code 2000}. */
    public long pollTimeoutMs = 2_000;

    public ConsumeOptions() {}
    public ConsumeOptions(String topic, String groupId) {
        this.topic = topic;
        this.groupId = groupId;
    }

    public ConsumeOptions topic(String t) { this.topic = t; return this; }
    public ConsumeOptions groupId(String g) { this.groupId = g; return this; }
    public ConsumeOptions maxMessages(int n) { this.maxMessages = n; return this; }
    public ConsumeOptions startOffset(String s) { this.startOffset = s; return this; }
    public ConsumeOptions pollTimeoutMs(long ms) { this.pollTimeoutMs = ms; return this; }
}
