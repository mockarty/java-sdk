// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.testcontainers;

/**
 * Unchecked exception type for {@link MockartyContainer} operation
 * failures (apply/reset/log/lifecycle). Wraps the underlying cause to
 * preserve the stack trace.
 */
public class MockartyContainerException extends RuntimeException {

    public MockartyContainerException(String message) {
        super(message);
    }

    public MockartyContainerException(String message, Throwable cause) {
        super(message, cause);
    }
}
