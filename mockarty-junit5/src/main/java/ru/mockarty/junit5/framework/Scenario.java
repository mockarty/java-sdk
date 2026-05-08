// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.Mock;
import ru.mockarty.model.SaveMockResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight scenario builder. Wraps an ad-hoc TCM case binding around
 * a block of imperative test code, and tracks any mocks created via
 * {@link #mock(Mock)} so they're auto-deleted on close.
 *
 * <p>Idiomatic usage:</p>
 * <pre>{@code
 * try (Scenario s = Scenario.open("E2E login").client(client).start()) {
 *     Step.run("seed", () -> s.mock(MockBuilder.http("/auth/login", "POST")
 *             .respond(200, Map.of("token", "abc"))
 *             .build()));
 *     Step.run("call under test", () -> myApp.login(...));
 * }
 * }</pre>
 *
 * <p>Defaults are picked for the 80% case: ad-hoc scenarios auto-create
 * the case if {@link #caseId(String)} isn't pinned, and mocks track for
 * cleanup. Plain {@code Scenario.open("name").start()} works without any
 * additional config when running inside a {@link MockartyTest}-annotated
 * class — the extension wires up a default client.</p>
 */
public final class Scenario implements AutoCloseable {

    private final String name;
    private MockartyClient client;
    private String caseId;
    private String planId;
    private boolean autoCreateCase = true;

    private final List<String> createdMockIds = new ArrayList<>();
    private MockartyContext.CaseFrame frame;
    private boolean started;
    private boolean closed;

    private Scenario(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Scenario.open requires a non-empty name");
        }
        this.name = name;
    }

    /** Begin a scenario with the given human-readable name. */
    public static Scenario open(String name) {
        return new Scenario(name);
    }

    public Scenario client(MockartyClient c) {
        this.client = c;
        return this;
    }

    /** Pin to an existing TCM case id; mutually exclusive with autoCreate. */
    public Scenario caseId(String id) {
        this.caseId = id;
        this.autoCreateCase = false;
        return this;
    }

    /** Owning Test Plan id (for grouping in reports). */
    public Scenario plan(String planId) {
        this.planId = planId;
        return this;
    }

    /** Disable auto-create; useful when the case must already exist. */
    public Scenario noAutoCreate() {
        this.autoCreateCase = false;
        return this;
    }

    /** Open the scenario context — pushes the case frame. Returns ``this``
     * so the caller can chain inside a try-with-resources. */
    public Scenario start() {
        if (started) {
            throw new IllegalStateException("Scenario already started");
        }
        started = true;
        frame = new MockartyContext.CaseFrame();
        frame.caseId = caseId;
        frame.caseName = name;
        frame.planId = planId;
        frame.autoCreate = caseId == null && autoCreateCase;
        MockartyContext.pushCase(frame);
        return this;
    }

    /** Create a mock through the bound client and track it for cleanup. */
    public Mock mock(Mock m) {
        requireClient();
        try {
            SaveMockResponse resp = client.mocks().create(m);
            Mock created = resp == null ? null : resp.getMock();
            if (created != null && created.getId() != null) {
                createdMockIds.add(created.getId());
            }
            return created;
        } catch (MockartyException e) {
            throw new RuntimeException("scenario.mock create failed: " + e.getMessage(), e);
        }
    }

    /** Add free-form metadata onto the case frame (e.g. environment tags). */
    public Scenario metadata(String key, Object value) {
        if (frame != null) {
            frame.metadata.put(key, value);
        }
        return this;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        // Tear down ad-hoc mocks first — best-effort, never throws.
        if (client != null) {
            for (String id : createdMockIds) {
                try {
                    client.mocks().delete(id);
                } catch (Exception ignored) { /* swallow */ }
            }
        }
        if (started) {
            MockartyContext.popCase();
        }
    }

    private void requireClient() {
        if (client == null) {
            throw new IllegalStateException(
                    "Scenario.mock() requires a client — set via Scenario.open(\"...\").client(client)"
            );
        }
    }
}
