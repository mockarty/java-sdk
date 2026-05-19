package ru.mockarty.protocols.rabbitmq;

/** Per-call options for {@link RabbitMqClient#consume(ConsumeOptions)}. */
public final class ConsumeOptions {
    public String queue;
    public int maxMessages = 1;
    public boolean autoAck = false;

    public ConsumeOptions() {}
    public ConsumeOptions(String queue) { this.queue = queue; }

    public ConsumeOptions queue(String q) { this.queue = q; return this; }
    public ConsumeOptions maxMessages(int n) { this.maxMessages = n; return this; }
    public ConsumeOptions autoAck(boolean ack) { this.autoAck = ack; return this; }
}
