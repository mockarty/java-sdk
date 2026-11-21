// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

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
