// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when the request fails server-side validation (HTTP 400).
 */
public class MockartyValidationException extends MockartyApiException {

    public MockartyValidationException(String errorMessage) {
        super(400, errorMessage);
    }

    public MockartyValidationException(String errorMessage, String responseBody) {
        super(400, errorMessage, responseBody);
    }

    public MockartyValidationException(String errorMessage, String responseBody, String code, String requestId) {
        super(400, errorMessage, responseBody, code, requestId);
    }
}
