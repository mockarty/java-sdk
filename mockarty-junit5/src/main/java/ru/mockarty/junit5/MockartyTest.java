// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

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
}
