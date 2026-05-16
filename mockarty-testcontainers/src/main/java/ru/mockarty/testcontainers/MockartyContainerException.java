// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

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
