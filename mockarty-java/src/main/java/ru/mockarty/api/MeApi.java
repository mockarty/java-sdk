// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import java.util.Map;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

/**
 * Per-caller endpoints ({@code /api/v1/me/*}).
 *
 * <p>Parity with the Go SDK ({@code client.Me()}) and the Python SDK
 * ({@code client.me}) — the Java SDK previously had no {@code me()} surface,
 * a 3-language parity gap surfaced during a cross-SDK audit.
 */
public class MeApi {

    private final MockartyClient client;

    public MeApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists the test executions awaiting THIS caller's manual action /
     * verification. Mirrors {@code MeAPI.AwaitingManual()} (Go) and
     * {@code me.awaiting_manual()} (Python).
     *
     * <p>{@code GET /api/v1/me/awaiting-manual}
     *
     * @return the raw response payload (awaiting-manual queue)
     * @throws MockartyException on transport / server error
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> awaitingManual() throws MockartyException {
        return client.get("/api/v1/me/awaiting-manual", Map.class);
    }
}
