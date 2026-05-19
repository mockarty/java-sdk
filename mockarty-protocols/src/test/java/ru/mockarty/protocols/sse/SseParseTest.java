package ru.mockarty.protocols.sse;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SseParseTest {

    private static List<SseEvent> parse(String text, int max) throws Exception {
        List<SseEvent> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new StringReader(text))) {
            SseClient.parse(r, out, max, Instant.now().plusSeconds(60));
        }
        return out;
    }

    @Test
    void singleEvent() throws Exception {
        var events = parse("data: hello\n\n", 10);
        assertEquals(1, events.size());
        assertEquals("hello", events.get(0).getData());
    }

    @Test
    void multipleEvents() throws Exception {
        var events = parse("data: one\n\ndata: two\n\ndata: three\n\n", 10);
        assertEquals(3, events.size());
        assertEquals("one", events.get(0).getData());
        assertEquals("three", events.get(2).getData());
    }

    @Test
    void namedEventWithId() throws Exception {
        var events = parse("event: orderCreated\nid: 42\ndata: payload\n\n", 10);
        assertEquals("orderCreated", events.get(0).getEvent());
        assertEquals("42", events.get(0).getId());
        assertEquals("payload", events.get(0).getData());
    }

    @Test
    void multilineDataConcatenates() throws Exception {
        var events = parse("data: line1\ndata: line2\n\n", 10);
        assertEquals("line1\nline2", events.get(0).getData());
    }

    @Test
    void commentLinesSkipped() throws Exception {
        var events = parse(": keep-alive\ndata: hi\n\n", 10);
        assertEquals("hi", events.get(0).getData());
    }

    @Test
    void retryFieldIsInt() throws Exception {
        var events = parse("retry: 5000\ndata: x\n\n", 10);
        assertEquals(Integer.valueOf(5000), events.get(0).getRetry());
    }

    @Test
    void retryInvalidIsNull() throws Exception {
        var events = parse("retry: ten\ndata: x\n\n", 10);
        assertNull(events.get(0).getRetry());
    }

    @Test
    void unknownFieldIgnored() throws Exception {
        var events = parse("weird: ignored\ndata: kept\n\n", 10);
        assertEquals("kept", events.get(0).getData());
    }

    @Test
    void blankEventNotDispatched() throws Exception {
        var events = parse("event: heartbeat\n\ndata: real\n\n", 10);
        assertEquals(1, events.size());
        assertEquals("real", events.get(0).getData());
    }

    @Test
    void maxEventsRespected() throws Exception {
        var events = parse("data: a\n\ndata: b\n\ndata: c\n\n", 2);
        assertEquals(2, events.size());
    }

    @Test
    void spaceAfterColonStrippedOnce() throws Exception {
        var events = parse("data: hello\n\n", 10);
        assertEquals("hello", events.get(0).getData());
        var events2 = parse("data:no-space\n\n", 10);
        assertEquals("no-space", events2.get(0).getData());
    }

    @Test
    void emptyUrlRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SseClient(""));
    }

    @Test
    void zeroMaxEventsRejected() {
        SseClient c = new SseClient("http://x");
        assertThrows(IllegalArgumentException.class, () -> c.collect(0, null));
    }
}
