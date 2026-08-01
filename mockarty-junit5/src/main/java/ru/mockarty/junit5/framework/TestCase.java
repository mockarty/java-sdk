// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a JUnit 5 test method to a Mockarty TCM case.
 *
 * <p>Defaults are designed for the 80% case:</p>
 * <ul>
 *   <li>{@code value()} — the case id; supply this when you have an existing case.</li>
 *   <li>{@code name()} — required when {@code autoCreate=true}; ignored otherwise.</li>
 *   <li>{@code plan()} — owning Test Plan id; optional, for grouping in reports.</li>
 *   <li>{@code autoCreate()} — when true, a missing case is created on the server before
 *       the test runs. Mutually exclusive with a non-empty {@code value()}.</li>
 * </ul>
 *
 * <p><b>Mockarty extensions</b> (owner directive 2026-05-18 —
 * see {@code docs/research/SDK_MOCKARTY_EXTENSIONS_AUDIT.md}). Allure
 * adapters never set any of these; they ride the
 * {@code ExternalRunRequest} fields the server gained in the same
 * phase:</p>
 * <ul>
 *   <li>{@code description()} — Markdown description for the case row.
 *       Saves the test author from explaining the test in two places
 *       (code comment AND TCM UI).</li>
 *   <li>{@code expectedResult()} — Markdown expected-result clause.
 *       Mockarty's primary differentiator vs Allure: the review
 *       workflow keys off this column.</li>
 *   <li>{@code customFields()} — string pairs encoded as
 *       {@code "type:name:value"} per entry. Persisted to
 *       {@code test_cases.custom_fields_json} (migration 203).</li>
 *   <li>{@code claimOwnership()} — when true, the receiver overwrites
 *       {@code description}/{@code expectedResult}/{@code customFields}
 *       on every upload so the code stays source of truth. Default
 *       false preserves manual UI edits.</li>
 * </ul>
 *
 * <p>Examples:</p>
 * <pre>{@code
 * // Bind to existing case
 * @Test
 * @TestCase("CASE-LOGIN-1")
 * @AttachReport
 * void login() { ... }
 *
 * // Auto-create with full Mockarty metadata
 * @Test
 * @TestCase(
 *     name = "login flow",
 *     autoCreate = true,
 *     plan = "qa-smoke",
 *     description = "## Smoke-test the email+password happy path",
 *     expectedResult = "Lands on /dashboard within 2s",
 *     customFields = {"feature:Auth:Login", "severity:severity:critical"},
 *     claimOwnership = true)
 * void loginAuto() { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestCase {

    /** Existing TCM case id. Empty when {@code autoCreate=true}. */
    String value() default "";

    /** Human-readable case name. Required with {@code autoCreate=true}. */
    String name() default "";

    /** Owning Test Plan id (numeric or uuid). Optional. */
    String plan() default "";

    /** Create the case on the server if it doesn't exist yet. */
    boolean autoCreate() default false;

    /**
     * Markdown description for the TCM case row. Mockarty
     * extension — Allure adapters never set this. Empty string = use
     * the boilerplate fallback on auto-create.
     */
    String description() default "";

    /**
     * Markdown "what should happen" clause. Mockarty's review
     * workflow keys off this column (migration 237). Empty = unset.
     */
    String expectedResult() default "";

    /**
     * Typed custom fields, each entry encoded as
     * {@code "type:name:value"} (e.g. {@code "feature:Auth:Login"}).
     * Annotation parameters can't be complex types, so we keep the
     * wire shape compact; the JUnit extension parses each entry and
     * forwards it as a {@code []CustomField} on the
     * {@code ExternalRunRequest}.
     */
    String[] customFields() default {};

    /**
     * When true, the receiver overwrites the case row's description /
     * expected_result / custom_fields with the annotation values on
     * EVERY upload. Default false preserves manual UI edits.
     */
    boolean claimOwnership() default false;
}
