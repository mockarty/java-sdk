package ru.mockarty.protocols.rabbitmq;

import java.util.LinkedHashMap;
import java.util.Map;

/** Mirrors the AMQP {@code queue.declare} arguments. */
public final class DeclareQueueOptions {
    public boolean durable;
    public boolean autoDelete;
    public boolean exclusive;
    public Map<String, Object> args;

    public DeclareQueueOptions() {}

    public DeclareQueueOptions durable(boolean d) { this.durable = d; return this; }
    public DeclareQueueOptions autoDelete(boolean d) { this.autoDelete = d; return this; }
    public DeclareQueueOptions exclusive(boolean e) { this.exclusive = e; return this; }
    public DeclareQueueOptions arg(String k, Object v) {
        if (args == null) args = new LinkedHashMap<>();
        args.put(k, v);
        return this;
    }
}
