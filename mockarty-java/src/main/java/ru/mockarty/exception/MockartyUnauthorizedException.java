// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when authentication fails (HTTP 401).
 */
public class MockartyUnauthorizedException extends MockartyApiException {

    public MockartyUnauthorizedException(String errorMessage) {
        super(401, errorMessage);
    }

    public MockartyUnauthorizedException(String errorMessage, String responseBody) {
        super(401, errorMessage, responseBody);
    }

    public MockartyUnauthorizedException(String errorMessage, String responseBody, String code, String requestId) {
        super(401, errorMessage, responseBody, code, requestId);
    }
}
