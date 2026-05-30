// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Override the Allure {@code suite} / {@code parentSuite} / {@code subSuite}
 * labels for a test class. Without this annotation the {@code suite} label
 * defaults to the test class's fully-qualified name (matches
 * allure-junit5).
 *
 * <pre>{@code
 * @MockartySuite(value = "API smoke", parentSuite = "QA Daily")
 * class LoginTest {
 *     // ...
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MockartySuite {

    /** Primary suite label. */
    String value() default "";

    /** Parent suite label (groups multiple suites). */
    String parentSuite() default "";

    /** Sub-suite label (narrower than the primary suite). */
    String subSuite() default "";
}
