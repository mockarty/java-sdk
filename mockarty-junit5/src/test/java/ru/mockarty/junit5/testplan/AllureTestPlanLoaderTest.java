// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.testplan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing, validation and environment resolution for the Allure test plan.
 *
 * <p>Every rejection asserted here exists because the alternative — the
 * reference adapters' silent fallback — turns a selective run into a full
 * run with a green tick.</p>
 */
class AllureTestPlanLoaderTest {

    private static Function<String, String> env(Map<String, String> values) {
        return values::get;
    }

    private static final Function<String, String> NO_PROPS = k -> null;

    private static AllureTestPlan parse(String json) {
        return AllureTestPlanLoader.parse(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), "plan.json");
    }

    @Test
    @DisplayName("a numeric id is normalised to a string so it matches @AllureId(\"777\")")
    void numericIdIsNormalised() {
        AllureTestPlan plan = parse(
                "{\"version\":\"1.0\",\"tests\":[{\"id\":11111,\"selector\":\"my.company.SimpleTest.simpleTestOne\"}]}");
        assertEquals("1.0", plan.getVersion());
        assertEquals(1, plan.getEntries().size());
        assertEquals("11111", plan.getEntries().get(0).getId());
        assertTrue(plan.matches(Collections.singleton("11111"), Collections.emptySet()));
        assertTrue(plan.matches(Collections.emptySet(),
                Collections.singleton("my.company.SimpleTest.simpleTestOne")));
        assertFalse(plan.matches(Collections.singleton("9"), Collections.singleton("other")));
    }

    @Test
    @DisplayName("id-only and selector-only entries are both valid")
    void idOnlyAndSelectorOnly() {
        AllureTestPlan plan = parse("{\"version\":\"1.0\",\"tests\":[{\"id\":\"7\"},{\"selector\":\"x#y\"}]}");
        assertEquals(asList(new AllureTestPlan.Entry("7", null), new AllureTestPlan.Entry(null, "x#y")),
                plan.getEntries());
    }

    @Test
    @DisplayName("\"tests\": [] is a well-formed but empty plan")
    void emptyTestsIsValidButEmpty() {
        AllureTestPlan plan = parse("{\"version\":\"1.0\",\"tests\":[]}");
        assertTrue(plan.isEmpty());
        assertFalse(plan.matches(Collections.singleton("x"), Collections.singleton("y")));
    }

    @Test
    @DisplayName("an unknown schema version is still honoured when tests[] is usable")
    void unknownVersionStillParses() {
        // Refusing it would turn a forward-compatible plan into a hard stop,
        // while honouring it can never widen the selection.
        AllureTestPlan plan = parse("{\"version\":\"2.0\",\"tests\":[{\"id\":\"1\"}]}");
        assertEquals("2.0", plan.getVersion());
        assertTrue(plan.matches(Collections.singleton("1"), Collections.emptySet()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "{ this is not json",
            "[]",
            "\"a string\"",
            "{\"version\":\"1.0\"}",
            "{\"version\":\"1.0\",\"tests\":null}",
            "{\"version\":\"1.0\",\"tests\":{}}",
            "{\"version\":\"1.0\",\"tests\":[\"nope\"]}",
            "{\"version\":\"1.0\",\"tests\":[{\"name\":\"x\"}]}",
            "{\"version\":\"1.0\",\"tests\":[{\"id\":null,\"selector\":null}]}",
    })
    @DisplayName("a broken plan is an error, never a silent full run")
    void brokenDocumentsAreRejected(String json) {
        assertThrows(MockartyTestPlanException.class, () -> parse(json));
    }

    @Test
    @DisplayName("no path configured means no filtering")
    void noPathMeansNoPlan() {
        assertNull(AllureTestPlanLoader.load(env(new HashMap<>()), NO_PROPS));
        Map<String, String> blank = new HashMap<>();
        blank.put(AllureTestPlanLoader.ENV_TESTPLAN_PATH, "   ");
        assertNull(AllureTestPlanLoader.load(env(blank), NO_PROPS));
    }

    @Test
    @DisplayName("MOCKARTY_TESTPLAN_MODE=off disables consumption; a typo does not")
    void modeOff(@TempDir Path dir) throws IOException {
        Path plan = write(dir, "{\"version\":\"1.0\",\"tests\":[{\"selector\":\"a\"}]}");
        Map<String, String> values = new HashMap<>();
        values.put(AllureTestPlanLoader.ENV_TESTPLAN_PATH, plan.toString());
        values.put(AllureTestPlanLoader.ENV_TESTPLAN_MODE, "off");
        assertNull(AllureTestPlanLoader.load(env(values), NO_PROPS));

        // A typo in the opt-out must NOT silently re-enable the full run.
        values.put(AllureTestPlanLoader.ENV_TESTPLAN_MODE, "enfroce");
        assertNotNull(AllureTestPlanLoader.load(env(values), NO_PROPS));
    }

    @Test
    @DisplayName("the system-property mirrors work, and the environment wins")
    void systemPropertyMirrors(@TempDir Path dir) throws IOException {
        Path plan = write(dir, "{\"version\":\"1.0\",\"tests\":[{\"selector\":\"a\"}]}");
        Function<String, String> props = k ->
                AllureTestPlanLoader.PROP_TESTPLAN_PATH.equals(k) ? plan.toString() : null;
        assertNotNull(AllureTestPlanLoader.load(env(new HashMap<>()), props));

        Map<String, String> values = new HashMap<>();
        values.put(AllureTestPlanLoader.ENV_TESTPLAN_MODE, "off");
        assertNull(AllureTestPlanLoader.load(env(values), props),
                "the environment must win over the system property");
    }

    @Test
    @DisplayName("a missing plan file is an error")
    void missingFileIsAnError(@TempDir Path dir) {
        MockartyTestPlanException e = assertThrows(MockartyTestPlanException.class,
                () -> AllureTestPlanLoader.loadFrom(dir.resolve("absent.json").toString()));
        assertTrue(e.getMessage().contains("missing"), e.getMessage());
    }

    @Test
    @DisplayName("a directory is not a readable plan")
    void directoryIsAnError(@TempDir Path dir) {
        assertThrows(MockartyTestPlanException.class,
                () -> AllureTestPlanLoader.loadFrom(dir.toString()));
    }

    @Test
    @DisplayName("a well-formed plan on disk round-trips")
    void loadFromFile(@TempDir Path dir) throws IOException {
        Path file = write(dir, "{\"version\":\"1.0\",\"tests\":[{\"id\":42}]}");
        AllureTestPlan plan = AllureTestPlanLoader.loadFrom(file.toString());
        assertEquals(file.toString(), plan.getPath());
        assertTrue(plan.matches(Collections.singleton("42"), Collections.emptySet()));
    }

    private static Path write(Path dir, String body) throws IOException {
        Path file = dir.resolve("testplan.json");
        Files.write(file, body.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
