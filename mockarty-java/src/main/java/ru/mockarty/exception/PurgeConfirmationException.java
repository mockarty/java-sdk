// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Thrown by {@code TrashApi.bulkPurge} / {@code adminBulkPurge} when the
 * confirmation phrase is missing or does not match
 * {@code TrashApi.PURGE_CONFIRMATION_PHRASE} exactly.
 *
 * <p>The SDK performs this check client-side so we never dispatch a request
 * the server will reject with an ambiguous 400.</p>
 */
public class PurgeConfirmationException extends MockartyException {

    public PurgeConfirmationException(String message) {
        super(message);
    }
}
