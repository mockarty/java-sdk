// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the static, parameter-less method that builds the
 * {@link ru.mockarty.fuzz.Target} for a {@link MockartyFuzz}-annotated
 * test class.
 *
 * <p>Mirrors the {@code @PactBuilder} pattern from the pact module. The
 * extension finds exactly one such method via reflection; multiple
 * matches or a non-static signature is a fail-fast error.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FuzzBuilder {
}
