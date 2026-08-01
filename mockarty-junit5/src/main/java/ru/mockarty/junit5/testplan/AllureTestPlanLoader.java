// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.testplan;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Reads the Allure test plan named by {@code ALLURE_TESTPLAN_PATH}.
 *
 * <p>Behaviour — deliberately stricter than the reference adapters, which
 * silently fall back to a full run:</p>
 *
 * <table border="1">
 *   <caption>Test-plan states</caption>
 *   <tr><th>State</th><th>Result</th></tr>
 *   <tr><td>path unset / blank</td><td>{@code null} — normal, unfiltered run</td></tr>
 *   <tr><td>{@code MOCKARTY_TESTPLAN_MODE=off}</td><td>{@code null} — unfiltered, even with a path set</td></tr>
 *   <tr><td>plan with N entries</td><td>only matching tests run</td></tr>
 *   <tr><td>{@code "tests": []}</td><td>a plan that selects nothing — the filter refuses the run</td></tr>
 *   <tr><td>missing / unreadable / malformed</td><td>{@link MockartyTestPlanException}</td></tr>
 *   <tr><td>{@code tests} absent, or an entry without id and selector</td><td>{@link MockartyTestPlanException}</td></tr>
 * </table>
 *
 * <p>The environment variable names are identical in the Go and Python SDKs.
 * For JVM ergonomics each one also has a system-property mirror
 * ({@code allure.testplan.path}, {@code mockarty.testplan.mode}); the
 * environment wins when both are set.</p>
 */
public final class AllureTestPlanLoader {

    /** Path to the plan file — the canonical Allure TestOps contract. */
    public static final String ENV_TESTPLAN_PATH = "ALLURE_TESTPLAN_PATH";

    /** Escape hatch: {@code off} ignores the plan entirely. */
    public static final String ENV_TESTPLAN_MODE = "MOCKARTY_TESTPLAN_MODE";

    /** System-property mirror of {@link #ENV_TESTPLAN_PATH}. */
    public static final String PROP_TESTPLAN_PATH = "allure.testplan.path";

    /** System-property mirror of {@link #ENV_TESTPLAN_MODE}. */
    public static final String PROP_TESTPLAN_MODE = "mockarty.testplan.mode";

    /** Enforce the plan (the default). */
    public static final String MODE_ENFORCE = "enforce";

    /** Ignore the plan; run everything. */
    public static final String MODE_OFF = "off";

    private static final String VERSION_FIELD = "version";
    private static final String TESTS_FIELD = "tests";

    private static final ObjectMapper MAPPER =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private AllureTestPlanLoader() {}

    /**
     * Load the plan from the process environment and system properties.
     *
     * @return the plan, or {@code null} when none is configured.
     * @throws MockartyTestPlanException when a plan is configured but cannot
     *                                   be read or parsed.
     */
    public static AllureTestPlan fromEnvironment() {
        return load(System::getenv, System::getProperty);
    }

    /**
     * Load the plan using explicit accessors — the testable entry point.
     *
     * @param env  environment-variable accessor.
     * @param prop system-property accessor.
     * @return the plan, or {@code null} when none is configured.
     * @throws MockartyTestPlanException when a plan is configured but cannot
     *                                   be read or parsed.
     */
    public static AllureTestPlan load(Function<String, String> env, Function<String, String> prop) {
        String path = firstNonBlank(env.apply(ENV_TESTPLAN_PATH), prop.apply(PROP_TESTPLAN_PATH));
        if (path == null) {
            return null;
        }
        if (MODE_OFF.equals(resolveMode(env, prop))) {
            return null;
        }
        return loadFrom(path);
    }

    /**
     * Resolve the mode. Unknown values fall back to {@link #MODE_ENFORCE} on
     * purpose: a typo in the opt-out must not silently re-enable the full run.
     *
     * @param env  environment-variable accessor.
     * @param prop system-property accessor.
     * @return {@link #MODE_ENFORCE} or {@link #MODE_OFF}.
     */
    public static String resolveMode(Function<String, String> env, Function<String, String> prop) {
        String raw = firstNonBlank(env.apply(ENV_TESTPLAN_MODE), prop.apply(PROP_TESTPLAN_MODE));
        if (raw == null) {
            return MODE_ENFORCE;
        }
        switch (raw.trim().toLowerCase()) {
            case "off":
            case "false":
            case "0":
            case "no":
            case "disabled":
                return MODE_OFF;
            default:
                return MODE_ENFORCE;
        }
    }

    /**
     * Read and validate a plan from an explicit path.
     *
     * @param path the {@code testplan.json} location.
     * @return the parsed plan (never null).
     * @throws MockartyTestPlanException when the file cannot be read or parsed.
     */
    public static AllureTestPlan loadFrom(String path) {
        Path file;
        try {
            file = Paths.get(path);
        } catch (RuntimeException e) {
            throw new MockartyTestPlanException(
                    ENV_TESTPLAN_PATH + "=" + path + ": not a valid path", e);
        }
        if (!Files.isRegularFile(file)) {
            throw new MockartyTestPlanException(
                    ENV_TESTPLAN_PATH + "=" + path + ": the test plan is missing or is not a regular file. "
                            + "Refusing to run the full suite — a selective run that silently becomes a full "
                            + "run is worse than a failed one.");
        }
        try (InputStream stream = Files.newInputStream(file)) {
            return parse(stream, path);
        } catch (IOException e) {
            throw new MockartyTestPlanException(
                    ENV_TESTPLAN_PATH + "=" + path + ": cannot read the test plan. "
                            + "Refusing to run the full suite.", e);
        }
    }

    /**
     * Parse and validate a plan document.
     *
     * @param stream the JSON bytes.
     * @param path   the origin, used in error messages.
     * @return the parsed plan (never null).
     * @throws MockartyTestPlanException when the document is not a usable plan.
     */
    public static AllureTestPlan parse(InputStream stream, String path) {
        JsonNode root;
        try {
            root = MAPPER.readTree(stream);
        } catch (IOException e) {
            throw new MockartyTestPlanException(
                    ENV_TESTPLAN_PATH + "=" + path + ": not valid JSON. Refusing to run the full suite — "
                            + "fix the plan or unset " + ENV_TESTPLAN_PATH + ".", e);
        }
        if (root == null || !root.isObject()) {
            throw new MockartyTestPlanException(
                    ENV_TESTPLAN_PATH + "=" + path + ": expected a JSON object with a '" + TESTS_FIELD
                            + "' array.");
        }
        JsonNode tests = root.get(TESTS_FIELD);
        if (tests == null || tests.isNull()) {
            throw new MockartyTestPlanException(
                    ENV_TESTPLAN_PATH + "=" + path + ": the plan has no '" + TESTS_FIELD + "' array. "
                            + "An Allure test plan without '" + TESTS_FIELD + "' cannot select anything; "
                            + "refusing to silently run the whole suite.");
        }
        if (!tests.isArray()) {
            throw new MockartyTestPlanException(
                    ENV_TESTPLAN_PATH + "=" + path + ": '" + TESTS_FIELD + "' must be an array.");
        }

        List<AllureTestPlan.Entry> entries = new ArrayList<>(tests.size());
        for (int i = 0; i < tests.size(); i++) {
            JsonNode node = tests.get(i);
            if (node == null || !node.isObject()) {
                throw new MockartyTestPlanException(
                        ENV_TESTPLAN_PATH + "=" + path + ": " + TESTS_FIELD + "[" + i
                                + "] must be an object with 'id' and/or 'selector'.");
            }
            String id = scalar(node.get("id"));
            String selector = scalar(node.get("selector"));
            if (id == null && selector == null) {
                throw new MockartyTestPlanException(
                        ENV_TESTPLAN_PATH + "=" + path + ": " + TESTS_FIELD + "[" + i
                                + "] has neither 'id' nor 'selector' — it can never match a test.");
            }
            entries.add(new AllureTestPlan.Entry(id, selector));
        }
        // An unknown `version` is honoured as long as `tests` is usable:
        // refusing it would turn a forward-compatible plan into a hard stop,
        // while honouring it can never widen the selection.
        return new AllureTestPlan(path, scalar(root.get(VERSION_FIELD)), entries);
    }

    /**
     * Normalise a plan scalar to a non-blank string. Allure writes {@code id}
     * as a JSON number, so numbers are rendered verbatim; booleans and
     * composites are rejected (returned as null) rather than stringified into
     * something that could accidentally match a test.
     */
    private static String scalar(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber()) {
            String text = node.asText().trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }
}
