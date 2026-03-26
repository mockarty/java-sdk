// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.ContentResponse;
import ru.mockarty.model.Mock;

import java.util.Map;

/**
 * Proxy mode examples showing request forwarding to real backends
 * and the Proxy API for programmatic HTTP/SOAP/gRPC proxying.
 *
 * <p>Proxy mode is useful for:</p>
 * <ul>
 *   <li>Adding artificial delay to real service responses</li>
 *   <li>Logging all traffic through Mockarty</li>
 *   <li>Testing timeouts and resilience</li>
 *   <li>Gradually replacing real services with mocks</li>
 *   <li>Protocol-specific proxying (HTTP, SOAP, gRPC)</li>
 * </ul>
 */
public class ProxyExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createSimpleProxy(client);
            createProxyWithDelay(client);
            createProxyWithCallbackLogging(client);
            createConditionalProxy(client);
            createGrpcProxy(client);
            proxyApiHttp(client);
            proxyApiSoap(client);
            proxyApiGrpc(client);
        }
    }

    /**
     * Simple proxy: forwards all requests to a real backend.
     * No delay, no modification -- pure pass-through.
     */
    static void createSimpleProxy(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/real-service/:path", "GET")
                .id("proxy-simple-passthrough")
                .proxyTo("https://api.real-service.com")
                .build();

        client.mocks().create(mock);
        System.out.println("Created simple proxy");
    }

    /**
     * Proxy with artificial delay.
     * Forwards to the real service but adds a delay AFTER receiving the response.
     * Useful for testing how your application handles slow services.
     */
    static void createProxyWithDelay(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/slow-service/:path", "GET")
                .id("proxy-slow-delay")
                .proxyTo("https://api.fast-service.com")
                .respond(new ContentResponse().delay(3000))  // 3-second delay after response
                .build();

        client.mocks().create(mock);
        System.out.println("Created proxy with 3s delay");
    }

    /**
     * Proxy with a callback for logging / auditing.
     * Forwards to real backend and notifies an audit system.
     */
    static void createProxyWithCallbackLogging(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/audited-service/:path", "POST")
                .id("proxy-with-audit")
                .proxyTo("https://api.production-service.com")
                .callback(
                        "https://audit.example.com/log",
                        "POST",
                        Map.of(
                                "event", "proxy_request",
                                "target", "production-service",
                                "timestamp", "$.fake.DateISO"
                        )
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created proxy with audit callback");
    }

    /**
     * Conditional proxy: some requests go to the real service, others get mocked.
     * Uses priority to route specific conditions to mocks while proxying the rest.
     */
    static void createConditionalProxy(MockartyClient client) {
        // High-priority: mock specific test users
        Mock mockTestUser = MockBuilder.http("/api/users/:id", "GET")
                .id("proxy-conditional-test-user")
                .priority(100)
                .condition("$.pathParam.id", AssertAction.EQUALS, "test-user-001")
                .respond(200, Map.of(
                        "id", "test-user-001",
                        "name", "Test User",
                        "email", "test@example.com",
                        "isMocked", true
                ))
                .build();

        // Low-priority: proxy all other requests to the real service
        Mock proxyRealUsers = MockBuilder.http("/api/users/:id", "GET")
                .id("proxy-conditional-real-users")
                .priority(1)
                .proxyTo("https://api.production.com")
                .build();

        client.mocks().create(mockTestUser);
        client.mocks().create(proxyRealUsers);
        System.out.println("Created conditional proxy setup");
    }

    /**
     * gRPC proxy: forwards gRPC calls to a real backend.
     */
    static void createGrpcProxy(MockartyClient client) {
        Mock mock = MockBuilder.grpc("product.ProductService", "GetProduct")
                .id("proxy-grpc-product")
                .proxyTo("grpc://product-service.internal:50051")
                .build();

        client.mocks().create(mock);
        System.out.println("Created gRPC proxy");
    }

    // ---- Proxy API (programmatic proxying) ----

    /**
     * Proxy an HTTP request programmatically via the Proxy API.
     * Useful for one-off forwarding without creating a mock.
     */
    static void proxyApiHttp(MockartyClient client) {
        System.out.println("\n=== Proxy API: HTTP ===");

        Map<String, Object> response = client.proxy().http(Map.of(
                "url", "https://httpbin.org/get",
                "method", "GET",
                "headers", Map.of(
                        "Accept", "application/json",
                        "X-Custom-Header", "proxy-test"
                ),
                "timeout", 5000
        ));

        System.out.println("HTTP proxy response:");
        System.out.println("  Status: " + response.get("statusCode"));
        System.out.println("  Body: " + response.get("body"));

        // POST with body
        Map<String, Object> postResponse = client.proxy().http(Map.of(
                "url", "https://httpbin.org/post",
                "method", "POST",
                "headers", Map.of("Content-Type", "application/json"),
                "body", Map.of(
                        "name", "test-user",
                        "email", "test@example.com"
                ),
                "timeout", 5000
        ));

        System.out.println("HTTP POST proxy response:");
        System.out.println("  Status: " + postResponse.get("statusCode"));
    }

    /**
     * Proxy a SOAP request programmatically via the Proxy API.
     */
    static void proxyApiSoap(MockartyClient client) {
        System.out.println("\n=== Proxy API: SOAP ===");

        String soapEnvelope = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <GetWeather xmlns="http://www.example.com/weather">
                      <City>London</City>
                    </GetWeather>
                  </soap:Body>
                </soap:Envelope>
                """;

        Map<String, Object> response = client.proxy().soap(Map.of(
                "url", "http://weather-service.internal/ws",
                "soapAction", "http://www.example.com/weather/GetWeather",
                "body", soapEnvelope,
                "headers", Map.of(
                        "Content-Type", "text/xml; charset=utf-8"
                ),
                "timeout", 10000
        ));

        System.out.println("SOAP proxy response:");
        System.out.println("  Status: " + response.get("statusCode"));
        System.out.println("  Body: " + response.get("body"));
    }

    /**
     * Proxy a gRPC request programmatically via the Proxy API.
     */
    static void proxyApiGrpc(MockartyClient client) {
        System.out.println("\n=== Proxy API: gRPC ===");

        Map<String, Object> response = client.proxy().grpc(Map.of(
                "target", "grpc://product-service.internal:50051",
                "service", "product.ProductService",
                "method", "GetProduct",
                "payload", Map.of(
                        "productId", "prod-123"
                ),
                "metadata", Map.of(
                        "authorization", "Bearer grpc-token"
                ),
                "timeout", 5000
        ));

        System.out.println("gRPC proxy response:");
        System.out.println("  Status: " + response.get("statusCode"));
        System.out.println("  Payload: " + response.get("payload"));
    }
}
