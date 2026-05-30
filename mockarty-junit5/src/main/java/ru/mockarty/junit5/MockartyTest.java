// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for JUnit 5 tests that use Mockarty.
 * Registers the {@link MockartyExtension} and configures the test environment.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @MockartyTest(namespace = "test-ns", cleanupAfterEach = true)
 * class MyApiTest {
 *
 *     @Test
 *     void shouldCreateMock(MockartyClient client, MockartyServer server) {
 *         Mock mock = MockBuilder.http("/api/test", "GET")
 *             .respond(200, Map.of("status", "ok"))
 *             .build();
 *         server.createMock(mock);
 *
 *         // ... test logic ...
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MockartyExtension.class)
public @interface MockartyTest {

    /**
     * The base URL of the Mockarty server.
     * If empty, reads from MOCKARTY_BASE_URL env var or defaults to http://localhost:5770.
     */
    String baseUrl() default "";

    /**
     * The API key for authentication.
     * If empty, reads from MOCKARTY_API_KEY env var.
     */
    String apiKey() default "";

    /**
     * The namespace to use for test mocks.
     * Defaults to "sandbox".
     */
    String namespace() default "sandbox";

    /**
     * Whether to automatically clean up mocks created during each test.
     * When true, all mocks created via MockartyServer are deleted after each test.
     * Defaults to true.
     */
    boolean cleanupAfterEach() default true;

    /**
     * Whether to harvest {@code io.qameta.allure.*} annotations (e.g.
     * {@code @Step}, {@code @Severity}, {@code @Feature}, {@code @Story},
     * {@code @Owner}, {@code @Description}, {@code @Issue}, {@code @TmsLink},
     * {@code @Link}, {@code @Epic}, {@code @Tag}, {@code @Label}, {@code @Title})
     * from the test class + method and lift them into the Mockarty case
     * frame metadata so existing Allure test code flows through the SDK
     * without refactoring.
     *
     * <p>Detection is reflection-based — we match on the annotation's
     * fully-qualified class name (FQCN endsWith), so the user's project
     * does NOT need to depend on {@code io.qameta.allure:allure-java-commons}.
     * Results are cached per {@link java.lang.reflect.Method} so the
     * runtime cost is microsecond-class after the first hit.</p>
     *
     * <p>Defaults to {@code true} — Owner decision (2026-05-16, see
     * {@code SDK_FRAMEWORK_PLAN.md} §3.3): seamless Allure compatibility
     * out of the box.</p>
     *
     * <p>Set to {@code false} to skip the scan if you don't use Allure
     * annotations and want to shave a few microseconds from every test.</p>
     */
    boolean mirrorAllure() default true;
}
