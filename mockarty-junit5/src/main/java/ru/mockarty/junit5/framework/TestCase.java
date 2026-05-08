// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

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
 * <p>Examples:</p>
 * <pre>{@code
 * // Bind to existing case
 * @Test
 * @TestCase("CASE-LOGIN-1")
 * @AttachReport
 * void login() { ... }
 *
 * // Auto-create on first run, then pin in subsequent runs
 * @Test
 * @TestCase(name = "login flow", autoCreate = true, plan = "qa-smoke")
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
}
