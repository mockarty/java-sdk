// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.allure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mockarty.junit5.allure.AllureModel.Attachment;
import ru.mockarty.junit5.allure.AllureModel.Container;
import ru.mockarty.junit5.allure.AllureModel.Label;
import ru.mockarty.junit5.allure.AllureModel.Link;
import ru.mockarty.junit5.allure.AllureModel.Parameter;
import ru.mockarty.junit5.allure.AllureModel.Stage;
import ru.mockarty.junit5.allure.AllureModel.Status;
import ru.mockarty.junit5.allure.AllureModel.StatusDetails;
import ru.mockarty.junit5.allure.AllureModel.StepResult;
import ru.mockarty.junit5.allure.AllureModel.TestResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Allure-2 result writer tests.
 *
 * <p>Asserts:</p>
 * <ul>
 *   <li>Required fields ({@code uuid}, {@code status}, {@code stage},
 *       {@code start}, {@code stop}) are emitted.</li>
 *   <li>Empty / null optional fields are <b>omitted</b> from the JSON
 *       — not serialised as {@code null}.</li>
 *   <li>Status enum is lowercase wire form.</li>
 *   <li>Nested steps preserve order and depth.</li>
 *   <li>Status priority bubbles correctly across nested step trees.</li>
 *   <li>Attachments survive a round-trip and the binary on disk matches
 *       what we registered.</li>
 *   <li>Concurrent writes from multiple threads do not corrupt files.</li>
 * </ul>
 */
class AllureWriterTest {

    @TempDir
    Path tmp;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void resetCtx() {
        AllureLifecycle.get().clearContext();
    }

    @AfterEach
    void clearCtx() {
        AllureLifecycle.get().clearContext();
    }

    @Test
    @DisplayName("TestResult: required fields present, optional absent fields omitted")
    void testResultOmitsNullFields() throws IOException {
        TestResult t = new TestResult();
        t.uuid = "11111111-1111-1111-1111-111111111111";
        t.name = "login";
        t.fullName = "ru.example.LoginTest.login";
        t.status = Status.PASSED;
        t.stage = Stage.FINISHED;
        t.start = 1_700_000_000_000L;
        t.stop = 1_700_000_000_500L;

        Path file = AllureWriter.writeTestResult(tmp, t);
        assertTrue(Files.exists(file));
        JsonNode node = mapper.readTree(file.toFile());

        assertEquals("11111111-1111-1111-1111-111111111111", node.path("uuid").asText());
        assertEquals("login", node.path("name").asText());
        assertEquals("passed", node.path("status").asText());
        assertEquals("finished", node.path("stage").asText());
        assertEquals(1_700_000_000_000L, node.path("start").asLong());
        assertEquals(1_700_000_000_500L, node.path("stop").asLong());
        // Absent optional fields must be missing, not null.
        assertFalse(node.has("description"), "absent description must be omitted");
        assertFalse(node.has("statusDetails"), "absent statusDetails must be omitted");
        assertFalse(node.has("labels"), "empty labels must be omitted");
        assertFalse(node.has("steps"), "empty steps must be omitted");
    }

    @Test
    @DisplayName("Status enum serialises to lowercase wire form")
    void statusLowercaseWire() {
        assertEquals("passed", Status.PASSED.toWire());
        assertEquals("failed", Status.FAILED.toWire());
        assertEquals("broken", Status.BROKEN.toWire());
        assertEquals("skipped", Status.SKIPPED.toWire());
        assertEquals("unknown", Status.UNKNOWN.toWire());
    }

    @Test
    @DisplayName("Status.worst bubbles failed > broken > unknown > skipped > passed")
    void statusWorstPriority() {
        assertEquals(Status.FAILED, Status.worst(stepsWithStatuses(
                Status.PASSED, Status.FAILED, Status.BROKEN)));
        assertEquals(Status.BROKEN, Status.worst(stepsWithStatuses(
                Status.PASSED, Status.BROKEN, Status.SKIPPED)));
        assertEquals(Status.UNKNOWN, Status.worst(stepsWithStatuses(
                Status.PASSED, Status.UNKNOWN, Status.SKIPPED)));
        assertEquals(Status.SKIPPED, Status.worst(stepsWithStatuses(
                Status.PASSED, Status.SKIPPED)));
        assertEquals(Status.PASSED, Status.worst(stepsWithStatuses(Status.PASSED)));
    }

    private static List<StepResult> stepsWithStatuses(Status... statuses) {
        return Stream.of(statuses).map(s -> {
            StepResult sr = new StepResult();
            sr.status = s;
            return sr;
        }).collect(Collectors.toList());
    }

    @Test
    @DisplayName("Nested steps preserve order + depth, attachments + statusDetails serialise")
    void nestedStepsAndDetails() throws IOException {
        TestResult t = new TestResult();
        t.uuid = "22222222-2222-2222-2222-222222222222";
        t.name = "checkout";
        t.status = Status.FAILED;
        t.stage = Stage.FINISHED;
        t.start = 10;
        t.stop = 200;

        StepResult outer = new StepResult();
        outer.name = "outer";
        outer.status = Status.PASSED;
        outer.start = 11;
        outer.stop = 100;
        outer.parameters.add(new Parameter("env", "qa"));

        StepResult inner = new StepResult();
        inner.name = "inner";
        inner.status = Status.FAILED;
        inner.start = 50;
        inner.stop = 90;
        StatusDetails sd = new StatusDetails();
        sd.message = "AssertionError";
        sd.trace = "at line 42";
        inner.statusDetails = sd;
        inner.attachments.add(new Attachment("req", "abc-attachment.txt", "text/plain"));
        outer.steps.add(inner);
        t.steps.add(outer);
        t.labels.add(new Label("severity", "critical"));
        t.links.add(new Link("AUTH-42", "https://jira/AUTH-42", "issue"));
        t.parameters.add(new Parameter("scenario", "negative"));
        t.statusDetails = sd;

        Path file = AllureWriter.writeTestResult(tmp, t);
        JsonNode node = mapper.readTree(file.toFile());

        JsonNode steps = node.path("steps");
        assertEquals(1, steps.size());
        assertEquals("outer", steps.get(0).path("name").asText());
        JsonNode sub = steps.get(0).path("steps");
        assertEquals(1, sub.size());
        assertEquals("inner", sub.get(0).path("name").asText());
        assertEquals("failed", sub.get(0).path("status").asText());

        JsonNode innerSd = sub.get(0).path("statusDetails");
        assertEquals("AssertionError", innerSd.path("message").asText());
        assertEquals("at line 42", innerSd.path("trace").asText());
        // booleans always emitted
        assertEquals(false, innerSd.path("known").asBoolean());
        assertEquals(false, innerSd.path("flaky").asBoolean());

        JsonNode innerAtt = sub.get(0).path("attachments");
        assertEquals(1, innerAtt.size());
        assertEquals("req", innerAtt.get(0).path("name").asText());
        assertEquals("abc-attachment.txt", innerAtt.get(0).path("source").asText());

        assertEquals("severity", node.path("labels").get(0).path("name").asText());
        assertEquals("AUTH-42", node.path("links").get(0).path("name").asText());
        assertEquals("scenario", node.path("parameters").get(0).path("name").asText());
    }

    @Test
    @DisplayName("Container emit: children list and befores/afters serialise")
    void containerEmit() throws IOException {
        Container c = new Container();
        c.uuid = "ctr-1";
        c.name = "ru.example.SomeTest";
        c.start = 1;
        c.stop = 1000;
        c.children.add("test-uuid-1");
        c.children.add("test-uuid-2");
        StepResult before = new StepResult();
        before.name = "setUp";
        before.status = Status.PASSED;
        c.befores.add(before);

        Path file = AllureWriter.writeContainer(tmp, c);
        JsonNode node = mapper.readTree(file.toFile());
        assertEquals("ctr-1", node.path("uuid").asText());
        assertEquals(2, node.path("children").size());
        assertEquals("test-uuid-1", node.path("children").get(0).asText());
        assertEquals(1, node.path("befores").size());
        assertEquals("setUp", node.path("befores").get(0).path("name").asText());
    }

    @Test
    @DisplayName("writeAttachment: bytes round-trip exactly, source filename is unique")
    void attachmentBytesRoundTrip() throws IOException {
        byte[] data = "request-body".getBytes(StandardCharsets.UTF_8);
        String src1 = AllureWriter.writeAttachment(tmp, "req", data, "text/plain");
        String src2 = AllureWriter.writeAttachment(tmp, "req", data, "text/plain");
        assertNotNull(src1);
        assertNotNull(src2);
        assertFalse(src1.equals(src2), "each writeAttachment must produce a unique source filename");
        Path f1 = tmp.resolve(src1);
        assertTrue(Files.exists(f1));
        byte[] back = Files.readAllBytes(f1);
        assertEquals(new String(data, StandardCharsets.UTF_8),
                new String(back, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("inferExtension maps MIME → extension and falls back to name suffix")
    void extensionInference() {
        assertEquals("json", AllureWriter.inferExtension("application/json", null));
        assertEquals("png", AllureWriter.inferExtension("image/png", null));
        assertEquals("html", AllureWriter.inferExtension("text/html;charset=utf-8", null));
        assertEquals("yaml", AllureWriter.inferExtension("application/yaml", null));
        assertEquals("yaml", AllureWriter.inferExtension("text/x-yaml+yaml", null));
        assertEquals("txt", AllureWriter.inferExtension("text/plain", null));
        assertEquals("zip", AllureWriter.inferExtension("application/x-custom", "blob.zip"));
        assertEquals("", AllureWriter.inferExtension(null, null));
    }

    @Test
    @DisplayName("Concurrent emit from 16 threads: every result file is intact")
    void concurrentEmit() throws Exception {
        int threads = 16;
        int perThread = 25;
        java.util.concurrent.ExecutorService ex =
                java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            ex.submit(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        TestResult r = new TestResult();
                        r.uuid = "t" + tid + "-i" + i;
                        r.name = "test-" + tid + "-" + i;
                        r.status = Status.PASSED;
                        r.stage = Stage.FINISHED;
                        r.start = 1; r.stop = 2;
                        AllureWriter.writeTestResult(tmp, r);
                    }
                } catch (Throwable ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        ex.shutdown();
        // every file should be parseable
        try (java.nio.file.DirectoryStream<Path> ds =
                     Files.newDirectoryStream(tmp, "*-result.json")) {
            int count = 0;
            for (Path p : ds) {
                count++;
                JsonNode n = mapper.readTree(p.toFile());
                assertTrue(n.has("uuid"), "intact uuid in " + p.getFileName());
                assertEquals("passed", n.path("status").asText());
            }
            assertEquals(threads * perThread, count, "all result files must land");
        }
    }
}
