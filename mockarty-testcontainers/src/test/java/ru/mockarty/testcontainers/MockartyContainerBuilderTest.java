// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.testcontainers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pure unit tests for the {@link MockartyContainer} builder surface.
 * Nothing here logically asserts on Docker — every assertion is on local
 * state (env map, exposed ports) that testcontainers-java populates
 * during configuration before {@code start()}.
 *
 * <p>However, the {@code GenericContainer} super-constructor that
 * {@link MockartyContainer} extends probes the Docker daemon at
 * instantiation time. To keep the suite runnable on CI shards without
 * a Docker daemon we gate the whole class on a quick socket probe of
 * the canonical unix socket or {@code DOCKER_HOST} value and abort with
 * a JUnit {@code assumption-failed} (= skipped, not failed) when none
 * answers. This preserves the original developer ergonomics on
 * docker-enabled machines while keeping the JaCoCo line-coverage report
 * tidy on docker-less CI.</p>
 */
class MockartyContainerBuilderTest {

    @BeforeAll
    static void requireDocker() {
        assumeTrue(dockerAvailable(),
            "Docker daemon not reachable — skipping MockartyContainer "
                + "builder tests (GenericContainer probes the daemon "
                + "at construction time)");
    }

    private static boolean dockerAvailable() {
        // Quick subprocess probe — `docker info` exits 0 iff the daemon
        // is actually reachable. We don't trust the unix-socket file's
        // mere existence: Docker Desktop leaves the socket node on disk
        // after the daemon stops, so a Files.exists() check would
        // false-positive and we'd be back to a 5-minute GenericContainer
        // hang. The probe is bounded to 2 seconds.
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "info", "--format", "{{.ServerVersion}}");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            if (!p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Test
    void defaultImage() {
        MockartyContainer c = new MockartyContainer();
        // configuredImageName() returns the locally-cached string
        // (no docker pull). getDockerImageName() would trigger
        // testcontainers' Future.get() and fail on hosts without
        // the image pulled.
        assertEquals(
            MockartyContainer.DEFAULT_IMAGE,
            c.configuredImageName());
    }

    @Test
    void customImageString() {
        MockartyContainer c = new MockartyContainer("ghcr.io/acme/mockarty-cli:1.2.3");
        assertEquals("ghcr.io/acme/mockarty-cli:1.2.3", c.configuredImageName());
    }

    @Test
    void customImageObject() {
        DockerImageName name = DockerImageName.parse("private/registry/cli:tag");
        MockartyContainer c = new MockartyContainer(name);
        assertEquals("private/registry/cli:tag", c.configuredImageName());
    }

    @Test
    void nullImageRejected() {
        assertThrows(NullPointerException.class,
            () -> new MockartyContainer((DockerImageName) null));
    }

    @Test
    void defaultExposedPortsIncludeBoth() {
        MockartyContainer c = new MockartyContainer();
        assertTrue(c.getExposedPorts().contains(MockartyContainer.MOCK_PORT),
            "MOCK_PORT not exposed");
        assertTrue(c.getExposedPorts().contains(MockartyContainer.METRICS_PORT),
            "METRICS_PORT not exposed");
    }

    @Test
    void defaultFormatIsAuto() {
        MockartyContainer c = new MockartyContainer();
        assertEquals(Format.AUTO.slug(),
            c.getEnvMap().get(MockartyContainer.FORMAT_ENV));
    }

    @Test
    void withFormatUpdatesEnv() {
        MockartyContainer c = new MockartyContainer()
            .withFormat(Format.MOCKARTY);
        assertEquals("mockarty",
            c.getEnvMap().get(MockartyContainer.FORMAT_ENV));
    }

    @Test
    void withFormatRejectsNull() {
        assertThrows(NullPointerException.class,
            () -> new MockartyContainer().withFormat(null));
    }

    @Test
    void withFormatChainable() {
        MockartyContainer c = new MockartyContainer();
        assertSame(c, c.withFormat(Format.WIREMOCK));
    }

    @Test
    void withStubFileRejectsEmptyString() {
        assertThrows(IllegalArgumentException.class,
            () -> new MockartyContainer().withStubFile(""));
        assertThrows(IllegalArgumentException.class,
            () -> new MockartyContainer().withStubFile("   "));
    }

    @Test
    void withStubFileRejectsNullPath() {
        assertThrows(NullPointerException.class,
            () -> new MockartyContainer().withStubFile((Path) null));
    }

    @Test
    void withStubFileChainable(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path stub = tmp.resolve("stubs.json");
        Files.writeString(stub, "[]");
        MockartyContainer c = new MockartyContainer();
        assertSame(c, c.withStubFile(stub));
    }

    @Test
    void applyRejectsNull() {
        MockartyContainer c = new MockartyContainer();
        assertThrows(IllegalArgumentException.class, () -> c.apply(null));
    }

    @Test
    void constantsHaveExpectedValues() {
        // Wire-protocol contract with the CLI image — flag drift = silent bug.
        assertEquals(8080, MockartyContainer.MOCK_PORT);
        assertEquals(9090, MockartyContainer.METRICS_PORT);
        assertEquals("/data/stubs", MockartyContainer.STUBS_MOUNT);
        assertEquals("/mocks", MockartyContainer.MAPPINGS_MOUNT);
        assertEquals("/har/traffic.har", MockartyContainer.HAR_MOUNT);
        // Constant was renamed MOCKARTY_STUB_FORMAT → MOCKARTY_MOCK_FORMAT
        // to match the CLI's applyMockServeEnv reader (review #109/H1).
        assertEquals("MOCKARTY_MOCK_FORMAT", MockartyContainer.FORMAT_ENV);
        assertEquals("MOCKARTY_MOCK_DIR", MockartyContainer.MOCK_DIR_ENV);
        assertEquals("MOCKARTY_HAR_REPLAY", MockartyContainer.HAR_REPLAY_ENV);
        assertNotNull(MockartyContainer.DEFAULT_IMAGE);
    }

    @Test
    void withMappingDirectorySetsEnv(@org.junit.jupiter.api.io.TempDir Path tmp) {
        MockartyContainer c = new MockartyContainer().withMappingDirectory(tmp);
        assertEquals(MockartyContainer.MAPPINGS_MOUNT,
            c.getEnvMap().get(MockartyContainer.MOCK_DIR_ENV));
    }

    @Test
    void withMappingDirectoryChainable(@org.junit.jupiter.api.io.TempDir Path tmp) {
        MockartyContainer c = new MockartyContainer();
        assertSame(c, c.withMappingDirectory(tmp));
    }

    @Test
    void withMappingDirectoryRejectsNonDir(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("not-a-dir.json");
        Files.writeString(file, "{}");
        assertThrows(IllegalArgumentException.class,
            () -> new MockartyContainer().withMappingDirectory(file));
    }

    @Test
    void withMappingDirectoryRejectsBlankString() {
        assertThrows(IllegalArgumentException.class,
            () -> new MockartyContainer().withMappingDirectory("  "));
    }

    @Test
    void withMappingDirectoryRejectsNullPath() {
        assertThrows(NullPointerException.class,
            () -> new MockartyContainer().withMappingDirectory((Path) null));
    }

    @Test
    void withHarReplaySetsEnv(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path har = tmp.resolve("traffic.har");
        Files.writeString(har, "{\"log\":{\"entries\":[]}}");
        MockartyContainer c = new MockartyContainer().withHarReplay(har);
        assertEquals(MockartyContainer.HAR_MOUNT,
            c.getEnvMap().get(MockartyContainer.HAR_REPLAY_ENV));
    }

    @Test
    void withHarReplayChainable(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path har = tmp.resolve("traffic.har");
        Files.writeString(har, "{}");
        MockartyContainer c = new MockartyContainer();
        assertSame(c, c.withHarReplay(har));
    }

    @Test
    void withHarReplayRejectsDirectory(@org.junit.jupiter.api.io.TempDir Path tmp) {
        assertThrows(IllegalArgumentException.class,
            () -> new MockartyContainer().withHarReplay(tmp));
    }

    @Test
    void withHarReplayRejectsBlankString() {
        assertThrows(IllegalArgumentException.class,
            () -> new MockartyContainer().withHarReplay("  "));
    }

    @Test
    void withHarReplayRejectsNullPath() {
        assertThrows(NullPointerException.class,
            () -> new MockartyContainer().withHarReplay((Path) null));
    }
}
