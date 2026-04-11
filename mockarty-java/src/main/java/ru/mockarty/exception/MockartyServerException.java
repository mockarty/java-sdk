// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when the server returns a 5xx error (HTTP 500 or any
 * non-specific server failure).
 */
public class MockartyServerException extends MockartyApiException {

    public MockartyServerException(int statusCode, String errorMessage) {
        super(statusCode, errorMessage);
    }

    public MockartyServerException(int statusCode, String errorMessage, String responseBody) {
        super(statusCode, errorMessage, responseBody);
    }

    public MockartyServerException(
            int statusCode, String errorMessage, String responseBody, String code, String requestId) {
        super(statusCode, errorMessage, responseBody, code, requestId);
    }
}
