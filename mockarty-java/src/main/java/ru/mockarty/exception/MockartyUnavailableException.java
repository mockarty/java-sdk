// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when a server dependency is unavailable (HTTP 503).
 */
public class MockartyUnavailableException extends MockartyApiException {

    public MockartyUnavailableException(String errorMessage) {
        super(503, errorMessage);
    }

    public MockartyUnavailableException(String errorMessage, String responseBody) {
        super(503, errorMessage, responseBody);
    }

    public MockartyUnavailableException(String errorMessage, String responseBody, String code, String requestId) {
        super(503, errorMessage, responseBody, code, requestId);
    }
}
