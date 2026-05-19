package ru.mockarty.protocols.telemetry;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryTest {

    @Test
    void nopRecorderDoesNotThrow() {
        NopRecorder.INSTANCE.record(Step.builder().key("k").name("n").build());
    }

    @Test
    void accumulatingRecorderBuffersSteps() {
        AccumulatingRecorder rec = new AccumulatingRecorder();
        assertTrue(rec.isEmpty());
        rec.record(Step.builder().key("k1").name("n1").build());
        rec.record(Step.builder().key("k2").name("n2").status("failed").build());
        assertEquals(2, rec.size());
        var payloads = rec.payloads();
        assertEquals("k1", payloads.get(0).get("stepKey"));
        assertEquals("passed", payloads.get(0).get("status"));
        assertEquals("failed", payloads.get(1).get("status"));
    }

    @Test
    void accumulatingRecorderClearEmptiesBuffer() {
        AccumulatingRecorder rec = new AccumulatingRecorder();
        rec.record(Step.builder().key("k").name("n").build());
        rec.clear();
        assertEquals(0, rec.size());
        assertTrue(rec.payloads().isEmpty());
    }

    @Test
    void accumulatingRecorderRawReturnsDefensiveCopy() {
        AccumulatingRecorder rec = new AccumulatingRecorder();
        rec.record(Step.builder().key("k").name("n").build());
        var raw = rec.raw();
        raw.add(Step.builder().key("x").name("x").build());
        assertEquals(1, rec.size());
    }

    @Test
    void accumulatingRecorderIsThreadSafe() throws Exception {
        AccumulatingRecorder rec = new AccumulatingRecorder();
        int threads = 8;
        int iters = 100;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            int idx = i;
            pool.submit(() -> {
                try { start.await(); } catch (InterruptedException ignored) {}
                for (int j = 0; j < iters; j++) {
                    rec.record(Step.builder().key("k-" + idx + "-" + j).name("n").build());
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(threads * iters, rec.size());
    }

    @Test
    void stepStatusDefaultsToPassedWhenEmpty() {
        Step s = Step.builder().key("k").name("n").status("").build();
        assertEquals("passed", s.getStatus());
    }

    @Test
    void stepDurationDerivedFromTimestamps() {
        Instant start = Instant.parse("2026-05-19T10:00:00Z");
        Instant end = start.plusMillis(250);
        Step s = Step.builder().key("k").name("n").startedAt(start).finishedAt(end).build();
        assertEquals(250L, s.getDurationMs());
    }

    @Test
    void stepToPayloadOmitsEmptyFields() {
        Map<String, Object> p = Step.builder().key("k").name("n").build().toPayload();
        assertEquals(Map.of("stepKey", "k", "name", "n", "status", "passed"), p);
    }

    @Test
    void stepToPayloadIncludesExtras() {
        Step s = Step.builder()
            .key("k").name("n")
            .parameter("http_status", "200")
            .message("oops")
            .stackTrace("line1\nline2")
            .parentKey("parent#1")
            .build();
        var p = s.toPayload();
        assertEquals(Map.of("http_status", "200"), p.get("parameters"));
        assertEquals("oops", p.get("message"));
        assertEquals("line1\nline2", p.get("stackTrace"));
        assertEquals("parent#1", p.get("parentKey"));
    }

    @Test
    void newStepKeyShape() {
        assertEquals("topic/op#42", Telemetry.newStepKey("topic/op", 42));
    }

    @Test
    void capPreviewBoundaries() {
        assertEquals("", Telemetry.capPreview("hello", 0));
        assertEquals("hello", Telemetry.capPreview("hello", 10));
        String big = "x".repeat(100);
        String got = Telemetry.capPreview(big, 10);
        assertTrue(got.startsWith("xxxxxxxxxx"));
        assertTrue(got.contains("truncated 90B"));
    }

    @Test
    void capPreviewCyrillicRunBoundary() {
        // "Привет" = 12 UTF-8 bytes, 6 chars. Each char is 2 bytes.
        // cap=5 would slice mid-codepoint with naive String.substring;
        // slide back to last lead byte → "Пр" (4 bytes) + marker.
        String got = Telemetry.capPreview("Привет", 5);
        assertEquals("Пр…(truncated 8B)", got);
    }

    @Test
    void capPreviewByteArrayOverload() {
        byte[] bytes = "Привет".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("Пр…(truncated 8B)", Telemetry.capPreview(bytes, 5));
        assertEquals("", Telemetry.capPreview((byte[]) null, 5));
        assertEquals("", Telemetry.capPreview(bytes, 0));
        assertEquals("", Telemetry.capPreview(bytes, -3));
        assertEquals("Привет", Telemetry.capPreview(bytes, 100));
    }

    @Test
    void capPreviewNegativeCapClampsToZero() {
        assertEquals("", Telemetry.capPreview("hello", -1));
    }

    @Test
    void capPreviewExactlyOnCodepointBoundary() {
        // "АБ" = 4 bytes; cap=2 == codepoint boundary, no walkback needed.
        String got = Telemetry.capPreview("АБВ", 2);
        assertEquals("А…(truncated 4B)", got);
    }
}
