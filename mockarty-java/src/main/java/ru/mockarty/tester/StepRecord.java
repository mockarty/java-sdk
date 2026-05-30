// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** One executed step. Mirrors the Go {@code StepRecord} struct. */
public final class StepRecord {

    public String protocol = "";
    public String method = "";
    public String name = "";
    public String url = "";
    public int statusOrCode;
    public Instant startedAt = Instant.EPOCH;
    public Instant endedAt = Instant.EPOCH;
    public final List<String> failures = new ArrayList<>();

    /** Shallow copy convenient when callers want to mutate independently. */
    public StepRecord copy() {
        StepRecord r = new StepRecord();
        r.protocol = this.protocol;
        r.method = this.method;
        r.name = this.name;
        r.url = this.url;
        r.statusOrCode = this.statusOrCode;
        r.startedAt = this.startedAt;
        r.endedAt = this.endedAt;
        r.failures.addAll(this.failures);
        return r;
    }
}

/** Internal contract — every chain step exposes commit(). */
interface Committable {
    void commit();
}
