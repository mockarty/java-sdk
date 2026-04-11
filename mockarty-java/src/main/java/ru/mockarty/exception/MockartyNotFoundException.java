// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when a requested resource is not found (HTTP 404).
 */
public class MockartyNotFoundException extends MockartyApiException {

    public MockartyNotFoundException(String errorMessage) {
        super(404, errorMessage);
    }

    public MockartyNotFoundException(String errorMessage, String responseBody) {
        super(404, errorMessage, responseBody);
    }

    public MockartyNotFoundException(String errorMessage, String responseBody, String code, String requestId) {
        super(404, errorMessage, responseBody, code, requestId);
    }
}
