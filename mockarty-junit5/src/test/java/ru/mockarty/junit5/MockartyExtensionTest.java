// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.Mock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MockartyExtensionTest {

    private static HttpServer mockServer;
    private static int port;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final AtomicInteger createCount = new AtomicInteger(0);
    private static final AtomicInteger deleteCount = new AtomicInteger(0);

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();

        // Mock create endpoint
        mockServer.createContext("/api/v1/mocks", exchange -> {
            int count = createCount.incrementAndGet();
            String json = "{\"overwritten\":false,\"mock\":{\"id\":\"test-mock-" + count + "\"}}";
            byte[] body = json.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        // Mock delete endpoint
        mockServer.createContext("/api/v1/mocks/", exchange -> {
            deleteCount.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
        });

        // Health endpoint
        mockServer.createContext("/health", exchange -> {
            String json = "{\"status\":\"pass\",\"releaseId\":\"test\"}";
            byte[] body = json.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        mockServer.start();

        // Set environment properties for the extension to pick up
        System.setProperty("MOCKARTY_BASE_URL", "http://localhost:" + port);
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
        System.clearProperty("MOCKARTY_BASE_URL");
    }

    @Nested
    @DisplayName("MockartyServer")
    class MockartyServerTests {

        @Test
        @DisplayName("should track created mocks")
        void trackCreatedMocks() {
            MockartyClient client = MockartyClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
            MockartyServer server = new MockartyServer(client);

            Mock mock = MockBuilder.http("/api/test", "GET")
                    .respond(200)
                    .build();

            server.createMock(mock);

            assertEquals(1, server.createdMockCount());
            assertFalse(server.getCreatedMockIds().isEmpty());

            client.close();
        }

        @Test
        @DisplayName("should cleanup all tracked mocks")
        void cleanupMocks() {
            int deleteBefore = deleteCount.get();

            MockartyClient client = MockartyClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
            MockartyServer server = new MockartyServer(client);

            // Create multiple mocks
            for (int i = 0; i < 3; i++) {
                server.createMock(MockBuilder.http("/api/test-" + i, "GET").respond(200).build());
            }

            assertEquals(3, server.createdMockCount());

            // Cleanup
            server.cleanup();

            assertEquals(0, server.createdMockCount());
            assertEquals(deleteBefore + 3, deleteCount.get());

            client.close();
        }

        @Test
        @DisplayName("should return empty list when no mocks created")
        void noMocksCreated() {
            MockartyClient client = MockartyClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
            MockartyServer server = new MockartyServer(client);

            assertEquals(0, server.createdMockCount());
            assertTrue(server.getCreatedMockIds().isEmpty());

            // Cleanup should be a no-op
            server.cleanup();

            client.close();
        }

        @Test
        @DisplayName("should return unmodifiable list of mock IDs")
        void unmodifiableList() {
            MockartyClient client = MockartyClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
            MockartyServer server = new MockartyServer(client);

            server.createMock(MockBuilder.http("/api/test", "GET").respond(200).build());

            assertThrows(UnsupportedOperationException.class,
                    () -> server.getCreatedMockIds().add("manual-id"));

            client.close();
        }
    }

    @Nested
    @DisplayName("Extension parameter resolution")
    class ParameterResolutionTests {

        @Test
        @DisplayName("should support MockartyClient parameter")
        void supportsClient() {
            MockartyExtension ext = new MockartyExtension();
            assertTrue(supportsType(ext, MockartyClient.class));
        }

        @Test
        @DisplayName("should support MockartyServer parameter")
        void supportsServer() {
            MockartyExtension ext = new MockartyExtension();
            assertTrue(supportsType(ext, MockartyServer.class));
        }

        @Test
        @DisplayName("should not support unsupported types")
        void unsupportedType() {
            MockartyExtension ext = new MockartyExtension();
            assertFalse(supportsType(ext, String.class));
            assertFalse(supportsType(ext, Object.class));
            assertFalse(supportsType(ext, Integer.class));
        }

        private boolean supportsType(MockartyExtension ext, Class<?> type) {
            try {
                // Create a mock ParameterContext using a method with the target type
                var method = TestMethodHolder.class.getMethod("testMethod", type);
                var parameter = method.getParameters()[0];

                ParameterContext paramCtx = new ParameterContext() {
                    @Override
                    public java.lang.reflect.Parameter getParameter() {
                        return parameter;
                    }

                    @Override
                    public int getIndex() {
                        return 0;
                    }

                    @Override
                    public java.util.Optional<Object> getTarget() {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public boolean isAnnotated(Class<? extends java.lang.annotation.Annotation> annotationType) {
                        return false;
                    }

                    @Override
                    public <A extends java.lang.annotation.Annotation> java.util.Optional<A> findAnnotation(Class<A> annotationType) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public <A extends java.lang.annotation.Annotation> java.util.List<A> findRepeatableAnnotations(Class<A> annotationType) {
                        return java.util.List.of();
                    }
                };

                return ext.supportsParameter(paramCtx, null);
            } catch (Exception e) {
                return false;
            }
        }
    }

    // Helper class used for parameter resolution testing
    public static class TestMethodHolder {
        public void testMethod(MockartyClient client) {}
        public void testMethod(MockartyServer server) {}
        public void testMethod(String string) {}
        public void testMethod(Object object) {}
        public void testMethod(Integer integer) {}
    }
}
