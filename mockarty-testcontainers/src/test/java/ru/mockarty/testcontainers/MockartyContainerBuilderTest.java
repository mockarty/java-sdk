// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for the {@link MockartyContainer} builder surface.
 * Nothing here actually spins up Docker — all assertions are on local
 * state (env map, exposed ports) which testcontainers-java populates
 * during configuration before {@code start()}.
 */
class MockartyContainerBuilderTest {

    @Test
    void defaultImage() {
        MockartyContainer c = new MockartyContainer();
        assertEquals(
            MockartyContainer.DEFAULT_IMAGE,
            c.getDockerImageName());
    }

    @Test
    void customImageString() {
        MockartyContainer c = new MockartyContainer("ghcr.io/acme/mockarty-cli:1.2.3");
        assertEquals("ghcr.io/acme/mockarty-cli:1.2.3", c.getDockerImageName());
    }

    @Test
    void customImageObject() {
        DockerImageName name = DockerImageName.parse("private/registry/cli:tag");
        MockartyContainer c = new MockartyContainer(name);
        assertEquals("private/registry/cli:tag", c.getDockerImageName());
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
        assertEquals("MOCKARTY_STUB_FORMAT", MockartyContainer.FORMAT_ENV);
        assertNotNull(MockartyContainer.DEFAULT_IMAGE);
    }
}
