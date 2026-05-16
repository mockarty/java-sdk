// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Runner#localSpawn} — no real {@code mockarty-cli} is
 * invoked, only an injected {@link ProcessLauncher} that captures the
 * argv and returns canned output.
 */
class LocalSpawnTest {

    private static Target sample() {
        return Target.named("ls")
                .httpEndpoint("POST", "/x")
                .seeds(Seed.of("s", "{}"))
                .mutator(Mutator.JSON)
                .build();
    }

    @Test
    void localSpawnInvokesCliWithJsonFlagAndTempFile() throws Exception {
        AtomicReference<List<String>> capturedArgv = new AtomicReference<>();
        ProcessLauncher fake = (argv, dir) -> {
            capturedArgv.set(new ArrayList<>(argv));
            // Verify the temp file the runner wrote is actually a Target
            // JSON we can re-parse — that proves writeTo() is wired in.
            String path = argv.get(argv.size() - 2);   // arg before --json
            String contents = java.nio.file.Files.readString(java.nio.file.Path.of(path));
            assertTrue(contents.contains("\"name\":\"ls\""));
            return new ProcessLauncher.ProcessResult(0,
                    "[info] starting fuzz\n"
                            + "{\"id\":\"local-1\",\"status\":\"completed\",\"totalFindings\":2,"
                            + "\"highFindings\":1,\"mediumFindings\":1,\"findings\":["
                            + "{\"id\":\"f1\",\"severity\":\"high\",\"category\":\"sqli\"},"
                            + "{\"id\":\"f2\",\"severity\":\"medium\",\"category\":\"xss\"}]}",
                    "");
        };
        try (Runner r = Runner.builder()
                .processes(fake)
                .cliBinary("/usr/local/bin/mockarty-cli")
                .build()) {
            Result result = r.localSpawn(sample());
            assertEquals("completed", result.status());
            assertEquals(2, result.totalFindings());
            assertEquals("sqli", result.findings().get(0).category());
        }
        List<String> argv = capturedArgv.get();
        assertNotNull(argv);
        assertEquals("/usr/local/bin/mockarty-cli", argv.get(0));
        assertEquals("fuzz", argv.get(1));
        assertEquals("run", argv.get(2));
        assertEquals("--json", argv.get(argv.size() - 1));
    }

    @Test
    void localSpawnSurfacesNonZeroExit() {
        ProcessLauncher fake = (argv, dir) ->
                new ProcessLauncher.ProcessResult(7, "", "boom");
        try (Runner r = Runner.builder().processes(fake).build()) {
            IOException ex = assertThrows(IOException.class, () -> r.localSpawn(sample()));
            assertTrue(ex.getMessage().contains("exited 7"));
            assertTrue(ex.getMessage().contains("boom"));
        }
    }

    @Test
    void localSpawnMissingJsonEnvelopeFails() {
        ProcessLauncher fake = (argv, dir) ->
                new ProcessLauncher.ProcessResult(0, "no json here, just text", "");
        try (Runner r = Runner.builder().processes(fake).build()) {
            IOException ex = assertThrows(IOException.class, () -> r.localSpawn(sample()));
            assertTrue(ex.getMessage().contains("no JSON result envelope"));
        }
    }

    @Test
    void extractLastJsonObjectHandlesNestedAndPrefix() {
        // Last legal envelope among progress lines.
        String stdout = "progress 1%\nprogress 50%\n"
                + "{\"id\":\"r1\",\"meta\":{\"k\":\"v\"},\"status\":\"completed\"}\n";
        String got = Runner.extractLastJsonObject(stdout);
        assertNotNull(got);
        assertTrue(got.startsWith("{"));
        assertTrue(got.endsWith("}"));
        assertTrue(got.contains("\"status\":\"completed\""));
    }

    @Test
    void extractLastJsonObjectReturnsNullOnNoBrace() {
        assertEquals(null, Runner.extractLastJsonObject("nothing here"));
        assertEquals(null, Runner.extractLastJsonObject(""));
        assertEquals(null, Runner.extractLastJsonObject(null));
    }
}
