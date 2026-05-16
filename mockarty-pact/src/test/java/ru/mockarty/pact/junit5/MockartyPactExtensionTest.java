// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.junit5;

import ru.mockarty.pact.Consumer;
import ru.mockarty.pact.MockServer;
import ru.mockarty.pact.Matchers;
import ru.mockarty.pact.SpecVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Validates the JUnit5 extension is auto-picked-up via the
 * {@code META-INF/services/org.junit.jupiter.api.extension.Extension}
 * SPI — no {@code @ExtendWith} declaration is present on this class.
 *
 * <p>For the SPI to take effect under Gradle, the test JVM is started
 * with {@code junit.jupiter.extensions.autodetection.enabled=true} via
 * a system property (set in build.gradle.kts). Without it the SPI is
 * ignored.</p>
 */
@PactConsumer(name = "OrderService", provider = "PaymentService", specVersion = SpecVersion.V4)
class MockartyPactExtensionTest {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();

    @PactBuilder
    static Consumer pact() {
        return Consumer.named("OrderService")
                .addInteraction(it -> it
                        .given("payment service is up")
                        .uponReceiving("a charge request")
                        .withRequest("POST", "/charge")
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Map.of("amount", Matchers.like(100)))
                        .willRespondWith(200)
                        .withJsonBody(Map.of("id", Matchers.like("abc"))));
    }

    @Test
    @DisplayName("Extension auto-detected and injects MockServer")
    void extensionInjectsMockServer(MockServer server) throws Exception {
        assertNotNull(server, "MockServer must be injected by the extension");
        HttpResponse<String> resp = HTTP.send(
                HttpRequest.newBuilder(URI.create(server.uri() + "/charge"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"amount\":100}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
    }
}
