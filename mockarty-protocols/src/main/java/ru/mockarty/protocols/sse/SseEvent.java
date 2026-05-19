package ru.mockarty.protocols.sse;

import java.util.List;

/** One parsed Server-Sent Event. */
public final class SseEvent {

    private final String data;
    private final String id;
    private final String event;
    private final Integer retry;
    private final List<String> rawLines;

    public SseEvent(String data, String id, String event, Integer retry, List<String> rawLines) {
        this.data = data == null ? "" : data;
        this.id = id;
        this.event = event;
        this.retry = retry;
        this.rawLines = rawLines == null ? List.of() : List.copyOf(rawLines);
    }

    public String getData() { return data; }
    public String getId() { return id; }
    public String getEvent() { return event; }
    public Integer getRetry() { return retry; }
    public List<String> getRawLines() { return rawLines; }
}
