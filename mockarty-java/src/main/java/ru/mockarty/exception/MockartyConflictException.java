// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when a resource conflict occurs (HTTP 409).
 */
public class MockartyConflictException extends MockartyApiException {

    public MockartyConflictException(String errorMessage) {
        super(409, errorMessage);
    }

    public MockartyConflictException(String errorMessage, String responseBody) {
        super(409, errorMessage, responseBody);
    }

    public MockartyConflictException(String errorMessage, String responseBody, String code, String requestId) {
        super(409, errorMessage, responseBody, code, requestId);
    }
}
