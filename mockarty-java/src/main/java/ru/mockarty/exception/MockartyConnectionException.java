// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

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
