// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5;

import ru.mockarty.MockartyClient;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension for Mockarty test integration.
 *
 * <p>This extension provides:</p>
 * <ul>
 *   <li>Automatic {@link MockartyClient} injection as a test parameter</li>
 *   <li>Automatic {@link MockartyServer} injection for tracked mock creation</li>
 *   <li>Automatic cleanup of created mocks after each test (configurable)</li>
 * </ul>
 *
 * <p>Can be used either with {@link MockartyTest} annotation or with
 * {@code @ExtendWith(MockartyExtension.class)}</p>
 *
 * <p>Usage with annotation:</p>
 * <pre>{@code
 * @MockartyTest(namespace = "test")
 * class MyTest {
 *     @Test
 *     void test(MockartyClient client, MockartyServer server) {
 *         server.createMock(MockBuilder.http("/api/test", "GET").respond(200).build());
 *     }
 * }
 * }</pre>
 *
 * <p>Usage with ExtendWith:</p>
 * <pre>{@code
 * @ExtendWith(MockartyExtension.class)
 * class MyTest {
 *     @Test
 *     void test(MockartyClient client) {
 *         // manual cleanup required
 *     }
 * }
 * }</pre>
 */
public class MockartyExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final Logger log = LoggerFactory.getLogger(MockartyExtension.class);

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(MockartyExtension.class);

    private static final String CLIENT_KEY = "mockarty-client";
    private static final String SERVER_KEY = "mockarty-server";
    private static final String CLEANUP_KEY = "mockarty-cleanup";

    @Override
    public void beforeEach(ExtensionContext context) {
        MockartyTest annotation = findAnnotation(context);

        String baseUrl = resolveValue(
                annotation != null ? annotation.baseUrl() : "",
                "MOCKARTY_BASE_URL",
                "http://localhost:5770"
        );

        String apiKey = resolveValue(
                annotation != null ? annotation.apiKey() : "",
                "MOCKARTY_API_KEY",
                null
        );

        String namespace = annotation != null ? annotation.namespace() : "sandbox";
        boolean cleanupAfterEach = annotation == null || annotation.cleanupAfterEach();

        MockartyClient client = MockartyClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .namespace(namespace)
                .build();

        MockartyServer server = new MockartyServer(client);

        ExtensionContext.Store store = context.getStore(NAMESPACE);
        store.put(CLIENT_KEY, client);
        store.put(SERVER_KEY, server);
        store.put(CLEANUP_KEY, cleanupAfterEach);

        log.debug("MockartyExtension initialized: baseUrl={}, namespace={}, cleanup={}",
                baseUrl, namespace, cleanupAfterEach);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        Boolean cleanup = store.get(CLEANUP_KEY, Boolean.class);
        if (Boolean.TRUE.equals(cleanup)) {
            MockartyServer server = store.get(SERVER_KEY, MockartyServer.class);
            if (server != null) {
                server.cleanup();
            }
        }

        MockartyClient client = store.get(CLIENT_KEY, MockartyClient.class);
        if (client != null) {
            client.close();
        }

        log.debug("MockartyExtension cleaned up after test: {}",
                context.getDisplayName());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type == MockartyClient.class || type == MockartyServer.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);

        if (type == MockartyClient.class) {
            return store.get(CLIENT_KEY, MockartyClient.class);
        }
        if (type == MockartyServer.class) {
            return store.get(SERVER_KEY, MockartyServer.class);
        }

        throw new ParameterResolutionException("Unsupported parameter type: " + type);
    }

    private MockartyTest findAnnotation(ExtensionContext context) {
        return context.getTestClass()
                .map(cls -> cls.getAnnotation(MockartyTest.class))
                .orElse(null);
    }

    private String resolveValue(String annotationValue, String envVar, String defaultValue) {
        // Annotation value takes priority
        if (annotationValue != null && !annotationValue.isEmpty()) {
            return annotationValue;
        }

        // Then system property
        String sysProp = System.getProperty(envVar);
        if (sysProp != null && !sysProp.isEmpty()) {
            return sysProp;
        }

        // Then environment variable
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Finally, default
        return defaultValue;
    }
}
