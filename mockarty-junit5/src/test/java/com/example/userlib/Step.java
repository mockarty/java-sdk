// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package com.example.userlib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test fixture: a user-defined annotation whose top-level FQCN
 * ({@code com.example.userlib.Step}) ends in {@code .Step}.
 *
 * <p>{@link ru.mockarty.junit5.framework.AllureMirror} must NOT
 * confuse this with {@code io.qameta.allure.Step}: the suffix match
 * alone would yield a false positive — only the canonical-package
 * guard keeps the harvest scoped.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Step {
    String value() default "user-defined, NOT allure";
}
