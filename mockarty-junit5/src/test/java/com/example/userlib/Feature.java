// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package com.example.userlib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test fixture: user-defined {@code Feature} annotation whose top-level
 * FQCN ({@code com.example.userlib.Feature}) collides with the
 * {@code io.qameta.allure.Feature} suffix.
 *
 * <p>Used by {@code AllureMirrorDefaultOnTest} to assert that the
 * harvester rejects suffix collisions outside the canonical Allure
 * packages.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Feature {
    String value() default "user-defined, NOT allure";
}
