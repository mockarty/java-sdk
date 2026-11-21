// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.exception;

/**
 * Exception thrown when the SDK cannot connect to the Mockarty server.
 */
public class MockartyConnectionException extends MockartyException {

    public MockartyConnectionException(String message) {
        super(message);
    }

    public MockartyConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
