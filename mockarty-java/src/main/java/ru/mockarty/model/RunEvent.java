// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Map;

/**
 * A single Server-Sent Event emitted while a Test Plan run executes.
 */
public class RunEvent {

    private final String kind;
    private final Map<String, Object> data;
    private final String raw;

    public RunEvent(String kind, Map<String, Object> data, String raw) {
        this.kind = kind;
        this.data = data;
        this.raw = raw;
    }

    /** Event name (e.g. {@code run.started}, {@code item.finished}). */
    public String getKind() {
        return kind;
    }

    /** Parsed JSON payload, or {@code null} if the frame carried no data. */
    public Map<String, Object> getData() {
        return data;
    }

    /** Raw JSON text from the {@code data:} field before parsing. */
    public String getRaw() {
        return raw;
    }
}
