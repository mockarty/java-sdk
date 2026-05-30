// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Seam over {@link ProcessBuilder} so {@link Runner#localSpawn} can be
 * unit-tested without forking a real {@code mockarty-cli} binary.
 *
 * <p>{@link #spawn} returns the captured stdout/stderr + exit code; the
 * Runner parses the stdout (which carries the result JSON as the final
 * line, per the CLI contract).</p>
 */
public interface ProcessLauncher {

    /**
     * Spawns a process with the supplied argv. Working directory inherits
     * from the JVM. Stdin is closed. stdout/stderr are captured fully —
     * fuzz runs typically produce kilobytes, not megabytes, of CLI output.
     */
    ProcessResult spawn(List<String> argv, Path workingDir) throws IOException, InterruptedException;

    /** Captured output + exit status. */
    record ProcessResult(int exitCode, String stdout, String stderr) {}

    /**
     * Default JDK-backed implementation. Marked {@code static} so tests
     * can swap it without touching the production constructor.
     */
    static ProcessLauncher jdk() {
        return (argv, workingDir) -> {
            ProcessBuilder pb = new ProcessBuilder(argv);
            if (workingDir != null) {
                pb.directory(workingDir.toFile());
            }
            // Merge stderr into stdout so the result JSON line never
            // races a separate stderr drain on slow OSes. We split them
            // back apart for callers because mixing CLI-progress on
            // stderr with the result JSON on stdout would break the
            // "result is the last stdout line" contract.
            Process p = pb.redirectErrorStream(false).start();
            // Close stdin — fuzz CLI never reads it. Without this, the
            // process can deadlock if the parent forgets to close.
            p.getOutputStream().close();
            String out = drain(p.getInputStream());
            String err = drain(p.getErrorStream());
            int exit = p.waitFor();
            return new ProcessResult(exit, out, err);
        };
    }

    private static String drain(java.io.InputStream stream) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
            return String.join(System.lineSeparator(), lines);
        }
    }
}
