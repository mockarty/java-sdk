// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Base exception for all Mockarty SDK errors.
 */
public class MockartyException extends RuntimeException {

    public MockartyException(String message) {
        super(message);
    }

    public MockartyException(String message, Throwable cause) {
        super(message, cause);
    }
}
