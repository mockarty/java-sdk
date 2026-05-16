// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JUnit5 test class as a Mockarty fuzz consumer test.
 *
 * <p>{@link MockartyFuzzExtension} reads this annotation to find the
 * static {@link FuzzBuilder}-tagged method that returns the
 * {@link ru.mockarty.fuzz.Target} for the run. Tests then inject a
 * {@link ru.mockarty.fuzz.Runner} or a {@link ru.mockarty.fuzz.Target}
 * as a parameter; the extension wires the lifecycle around each
 * {@code @Test}.</p>
 *
 * <pre>{@code
 * @MockartyFuzz(adminUrl = "https://mockarty.example.com",
 *               namespace = "default",
 *               apiToken = "tok-abc")
 * class LoginFuzzTest {
 *     @FuzzBuilder
 *     static Target target() { return Target.named("login").httpEndpoint(...).build(); }
 *
 *     @Test
 *     void runsAgainstAdmin(Runner runner, Target target) throws Exception {
 *         JobId job = runner.submit(target);
 *         Result r = runner.waitFor(job);
 *         assertEquals("completed", r.status());
 *     }
 * }
 * }</pre>
 *
 * <p>For offline iteration, leave {@code adminUrl} empty and set
 * {@code localSpawn = true} — the runner will fork {@code mockarty-cli}
 * via {@link ru.mockarty.fuzz.Runner#localSpawn(ru.mockarty.fuzz.Target)}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MockartyFuzz {

    /** Admin URL the runner submits to. Empty = no-admin (CLI-only) mode. */
    String adminUrl() default "";

    /** Namespace claim sent in {@code X-Namespace}. Empty = caller default. */
    String namespace() default "";

    /** API token / key sent in {@code X-API-Key}. Empty = none (anon). */
    String apiToken() default "";

    /**
     * If true, the extension marks the runner as local-CLI mode — calls
     * to {@code runner.submit(target)} will be rewritten into
     * {@code runner.localSpawn(target)}. False (default) = remote mode.
     */
    boolean localSpawn() default false;
}
