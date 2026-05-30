// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz;

/**
 * Reporter format the fuzz runner should emit alongside the findings
 * stream.
 *
 * <p>Mirrors the {@code --reporter} flag on {@code mockarty-cli fuzz run}
 * and the {@code reporter} field on the server-side fuzz config. Multiple
 * reporters can be attached via repeated
 * {@link Target.Builder#reporter} calls.</p>
 */
public enum Reporter {
    /** No external report — findings only via the API/CLI stdout. */
    NONE("none"),
    /** Allure 2 XML — drops into Allure CLI / Mockarty's Allure mirror. */
    ALLURE("allure"),
    /** JUnit-compatible XML — for Jenkins / GitHub Actions report tabs. */
    JUNIT("junit"),
    /** SARIF 2.1.0 — GitHub Advanced Security / Defender for DevOps. */
    SARIF("sarif"),
    /** Raw JSONL stream — one finding per line, for piping into tooling. */
    JSON("json");

    private final String wire;

    Reporter(String wire) {
        this.wire = wire;
    }

    /** Wire identifier emitted in the transpiled JSON config. */
    public String wire() {
        return wire;
    }
}
