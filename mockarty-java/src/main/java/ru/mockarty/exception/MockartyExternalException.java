// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when an external system called by Mockarty fails (HTTP 502).
 */
public class MockartyExternalException extends MockartyApiException {

    public MockartyExternalException(String errorMessage) {
        super(502, errorMessage);
    }

    public MockartyExternalException(String errorMessage, String responseBody) {
        super(502, errorMessage, responseBody);
    }

    public MockartyExternalException(String errorMessage, String responseBody, String code, String requestId) {
        super(502, errorMessage, responseBody, code, requestId);
    }
}
