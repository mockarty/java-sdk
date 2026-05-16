// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

/**
 * Thin testcontainers wrapper around the {@code mockarty/cli:latest-mock}
 * image. Drop-in replacement for {@code WireMockContainer} /
 * {@code MockServerContainer} in user integration tests.
 *
 * <p>The package is the Java side of Mockarty's Wave 4 SDK strategy
 * (see {@code docs/research/SDK_FRAMEWORK_PLAN.md} rev 3 §6.4). Like
 * the other Wave 3/4 modules it is intentionally thin:
 *
 * <ul>
 *   <li>It does NOT embed a mock engine — the container itself runs the
 *       real {@code mockarty-cli mock serve} process baked into the
 *       image.</li>
 *   <li>It DOES manage the Docker lifecycle (pull, start, wait for
 *       {@code /health}, stop, cleanup) via testcontainers-java.</li>
 *   <li>It DOES expose both WireMock-compat ({@code /__admin}) and
 *       Mockarty-native ({@code /api/v1}) admin URLs so existing
 *       WireMock and Mockarty test bodies can both target the same
 *       running container.</li>
 * </ul>
 *
 * <h2>Quick start</h2>
 * <pre>
 * try (MockartyContainer c = new MockartyContainer()) {
 *     c.start();
 *     c.apply(Map.of(
 *         "http", Map.of("request", Map.of("method", "GET", "path", "/ping")),
 *         "response", Map.of("status", 200, "body", "pong")));
 *     HttpResponse&lt;String&gt; r = HttpClient.newHttpClient().send(
 *         HttpRequest.newBuilder(URI.create(c.url() + "/ping")).build(),
 *         BodyHandlers.ofString());
 *     assertEquals("pong", r.body());
 * }
 * </pre>
 *
 * <p>Docker is a hard prerequisite. Tests that import this package
 * MUST skip (not fail) when no docker daemon is reachable so the wider
 * SDK test suite stays green on CI shards without docker.</p>
 */
package ru.mockarty.testcontainers;
