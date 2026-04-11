// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when access is forbidden (HTTP 403).
 */
public class MockartyForbiddenException extends MockartyApiException {

    public MockartyForbiddenException(String errorMessage) {
        super(403, errorMessage);
    }

    public MockartyForbiddenException(String errorMessage, String responseBody) {
        super(403, errorMessage, responseBody);
    }

    public MockartyForbiddenException(String errorMessage, String responseBody, String code, String requestId) {
        super(403, errorMessage, responseBody, code, requestId);
    }
}
