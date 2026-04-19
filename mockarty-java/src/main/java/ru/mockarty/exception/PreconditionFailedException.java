// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Thrown when a server returns {@code 412 Precondition Failed} — the
 * supplied {@code If-Match} etag no longer matches the resource.
 *
 * <p>Callers should re-fetch the current state, reconcile their intent,
 * and retry with the refreshed etag.</p>
 *
 * <p>Currently emitted by {@code TestPlanApi.patch(...)} when the plan's
 * {@code updatedAt} has moved since the etag was captured.</p>
 */
public class PreconditionFailedException extends MockartyException {

    private final String currentEtag;

    public PreconditionFailedException(String message) {
        this(message, null);
    }

    public PreconditionFailedException(String message, String currentEtag) {
        super(message);
        this.currentEtag = currentEtag;
    }

    /**
     * Returns the freshest etag the server disclosed in the failure
     * response, or {@code null} if it did not.
     */
    public String getCurrentEtag() {
        return currentEtag;
    }
}
