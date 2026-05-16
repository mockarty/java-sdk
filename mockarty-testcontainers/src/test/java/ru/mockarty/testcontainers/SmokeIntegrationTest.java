// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.testcontainers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
// Unused-import diet kept clean: HTTP imports are used by the smoke test.

/**
 * End-to-end smoke test against the real CLI image. Opt-in via the
 * {@code MOCKARTY_SDK_DOCKER_SMOKE} env-var so the regular test pass
 * stays green on CI shards without Docker.
 *
 * <p>Requires:</p>
 * <ul>
 *   <li>Docker daemon reachable (default unix socket or
 *       {@code DOCKER_HOST} env).</li>
 *   <li>The {@code mockarty/cli:latest-mock} image pullable from the
 *       configured registry.</li>
 * </ul>
 */
@ExtendWith(MockartyContainerExtension.class)
@EnabledIfEnvironmentVariable(named = "MOCKARTY_SDK_DOCKER_SMOKE", matches = ".+")
class SmokeIntegrationTest {

    static final MockartyContainer mockarty = new MockartyContainer()
        .withFormat(Format.MOCKARTY);

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @Test
    void containerStartsAndAppliesStub() throws Exception {
        assertNotNull(mockarty.url(), "url must be available once started");
        assertTrue(mockarty.url().startsWith("http://"),
            "url should be http: " + mockarty.url());

        mockarty.apply(Map.of(
            "http", Map.of("request", Map.of("method", "GET", "path", "/ping")),
            "response", Map.of("status", 200, "body", "pong")));

        HttpResponse<String> resp = HTTP.send(
            HttpRequest.newBuilder(URI.create(mockarty.url() + "/ping")).build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("pong", resp.body());

        mockarty.reset();
    }
}
