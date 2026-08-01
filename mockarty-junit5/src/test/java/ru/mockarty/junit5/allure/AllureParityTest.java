// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.allure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mockarty.junit5.allure.AllureModel.Attachment;
import ru.mockarty.junit5.allure.AllureModel.Label;
import ru.mockarty.junit5.allure.AllureModel.Link;
import ru.mockarty.junit5.allure.AllureModel.Parameter;
import ru.mockarty.junit5.allure.AllureModel.Stage;
import ru.mockarty.junit5.allure.AllureModel.Status;
import ru.mockarty.junit5.allure.AllureModel.StatusDetails;
import ru.mockarty.junit5.allure.AllureModel.StepResult;
import ru.mockarty.junit5.allure.AllureModel.TestResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Byte-for-byte parity test.
 *
 * <p>Builds a {@link TestResult} that mirrors what allure-pytest 2.13
 * would emit for the same logical test, writes it through
 * {@link AllureWriter}, then compares the canonicalised JSON to the
 * curated fixture under {@code src/test/resources/allure-parity/}. The
 * comparison normalises map ordering (so the test verifies structural
 * + value equality regardless of Jackson's iteration order changes).
 */
class AllureParityTest {

    @TempDir
    Path tmp;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("AllureWriter output matches curated allure-pytest reference fixture (key+value parity)")
    void emitsByteAccurateAllure2Json() throws IOException {
        // ── Build a TestResult that mirrors the fixture ───────────────
        TestResult t = new TestResult();
        t.uuid = "00000000-0000-4000-8000-000000000001";
        t.historyId = "deadbeefcafebabe";
        t.name = "should fail on invalid email";
        t.fullName = "ru.example.LoginTest.shouldFailOnInvalidEmail";
        t.description = "Pact + Allure parity sample";
        t.status = Status.FAILED;
        t.stage = Stage.FINISHED;
        t.start = 1_700_000_000_000L;
        t.stop = 1_700_000_000_345L;

        StatusDetails td = new StatusDetails();
        td.message = "AssertionError: email rejected";
        td.trace = "at LoginTest.java:42";
        t.statusDetails = td;

        t.labels.add(new Label("framework", "junit5"));
        t.labels.add(new Label("language", "java"));
        t.labels.add(new Label("severity", "critical"));
        t.labels.add(new Label("owner", "auth-team"));
        t.labels.add(new Label("feature", "Login"));
        t.labels.add(new Label("story", "Negative inputs"));
        t.labels.add(new Label("tag", "smoke"));
        t.labels.add(new Label("suite", "ru.example.LoginTest"));
        t.labels.add(new Label("testClass", "ru.example.LoginTest"));
        t.labels.add(new Label("testMethod", "shouldFailOnInvalidEmail"));

        t.links.add(new Link("AUTH-42", "https://jira.example.com/AUTH-42", "issue"));
        t.links.add(new Link("TC-100", "https://tms.example.com/TC-100", "tms"));
        t.parameters.add(new Parameter("email", "not-an-email"));
        t.attachments.add(new Attachment("request", "req-001-attachment.txt", "text/plain"));

        StepResult s1 = new StepResult();
        s1.name = "POST /login";
        s1.status = Status.PASSED;
        s1.stage = Stage.FINISHED;
        s1.start = 1_700_000_000_010L;
        s1.stop = 1_700_000_000_050L;

        StepResult s2 = new StepResult();
        s2.name = "assert 400 response";
        s2.status = Status.FAILED;
        s2.stage = Stage.FINISHED;
        s2.start = 1_700_000_000_060L;
        s2.stop = 1_700_000_000_345L;
        StatusDetails sd2 = new StatusDetails();
        sd2.message = "expected 400 got 200";
        s2.statusDetails = sd2;

        t.steps.add(s1);
        t.steps.add(s2);

        Path written = AllureWriter.writeTestResult(tmp, t);
        JsonNode produced = mapper.readTree(written.toFile());

        JsonNode expected;
        try (InputStream is = getClass().getResourceAsStream(
                "/allure-parity/expected-test-result.json")) {
            assertNotNull(is, "fixture must be on classpath");
            expected = mapper.readTree(is);
        }

        // Canonicalise (sort map keys recursively + compare equality of
        // the normalised trees). This isolates the parity assertion from
        // Jackson's iteration order on hash maps.
        Object normProduced = canonicalize(produced);
        Object normExpected = canonicalize(expected);
        assertEquals(normExpected, normProduced,
                "Allure-2 JSON parity violated.\n"
                        + "Produced: " + mapper.writeValueAsString(normProduced) + "\n"
                        + "Expected: " + mapper.writeValueAsString(normExpected));
    }

    /** Walk a Jackson tree and convert ObjectNode → TreeMap, ArrayNode → List<Object>. */
    private static Object canonicalize(JsonNode n) {
        if (n.isObject()) {
            Map<String, Object> m = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = n.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                m.put(e.getKey(), canonicalize(e.getValue()));
            }
            return m;
        }
        if (n.isArray()) {
            java.util.List<Object> l = new java.util.ArrayList<>(n.size());
            for (JsonNode el : n) {
                l.add(canonicalize(el));
            }
            return l;
        }
        if (n.isBoolean()) return n.asBoolean();
        if (n.isLong() || n.isInt()) return n.asLong();
        if (n.isDouble() || n.isFloat()) return n.asDouble();
        if (n.isNull()) return null;
        return n.asText();
    }

    @Test
    @DisplayName("historyId byte-parity with Python allure_commons: parameter values sorted by name, no separator")
    void historyIdWithParametersByteParity() {
        // Full name + parameters ("email", "not-an-email") → md5 byte-identical
        // to Python allure_commons.utils.get_history_id.
        String fullName = "ru.example.LoginTest.shouldFailOnInvalidEmail";
        String paramSig = "not-an-email"; // one param, value only
        String got = AllureLifecycle.stableHistoryId(fullName, paramSig);

        // Go SDK produces the same value:
        //   stableHistoryId("ru.example.LoginTest.shouldFailOnInvalidEmail", "not-an-email")
        assertEquals("0e67d278f9a0560373d8052d458f31db", got,
                "historyId must match Go + Python byte-for-byte");

        // Multi-param: values sorted by name, no separator.
        String multiParams = "Apple" + "42";
        String multiGot = AllureLifecycle.stableHistoryId(
                "com.example.ParameterizedTest.testMethod", multiParams);
        assertEquals("7a0cb9d0e575a1be4cb8383893c977d3", multiGot,
                "multi-param historyId must match Go + Python");
    }

    @Test
    @DisplayName("historyId parameter-independent without parameters")
    void historyIdNoParametersMatchesPython() {
        String fullName = "com.example.SimpleTest.plainMethod";
        String got = AllureLifecycle.stableHistoryId(fullName, null);
        assertEquals("2820911e4c5a8cd4cb395ff44be16173", got,
                "no-param historyId must match Go + Python");
    }

    @Test
    @DisplayName("Canonical schema: timestamps survive as longs (not ISO strings)")
    void timestampsAsLongs() throws IOException {
        TestResult t = new TestResult();
        t.uuid = "u-1";
        t.status = Status.PASSED;
        t.stage = Stage.FINISHED;
        t.start = 1_500_000_000_000L;
        t.stop = 1_500_000_001_000L;
        Path file = AllureWriter.writeTestResult(tmp, t);
        JsonNode n = mapper.readTree(file.toFile());
        assertTrue(n.path("start").isLong() || n.path("start").isInt(),
                "start must be long-typed in JSON");
        assertEquals(1_500_000_000_000L, n.path("start").asLong());
    }
}
