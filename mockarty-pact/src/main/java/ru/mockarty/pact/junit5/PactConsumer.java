// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.junit5;

import ru.mockarty.pact.SpecVersion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JUnit5 test class as a Pact consumer test.
 *
 * <p>The {@link MockartyPactExtension} reads this annotation to configure
 * the consumer/provider names and spec version applied to every
 * {@code @Test} method in the class. Tests then inject a
 * {@link ru.mockarty.pact.MockServer} as a parameter; the extension takes
 * care of starting/stopping it around each test and writing the contract
 * to the output directory.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PactConsumer {

    /** Consumer service name (required). */
    String name();

    /** Provider service name (required). */
    String provider();

    /** Pact spec version to emit. Default {@link SpecVersion#V4}. */
    SpecVersion specVersion() default SpecVersion.V4;

    /** Output directory relative to the JVM CWD. Empty = {@code build/pacts}. */
    String outputDir() default "";
}
