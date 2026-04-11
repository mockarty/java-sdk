// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when the rate limit is exceeded (HTTP 429).
 */
public class MockartyRateLimitException extends MockartyApiException {

    public MockartyRateLimitException(String errorMessage) {
        super(429, errorMessage);
    }

    public MockartyRateLimitException(String errorMessage, String responseBody) {
        super(429, errorMessage, responseBody);
    }

    public MockartyRateLimitException(String errorMessage, String responseBody, String code, String requestId) {
        super(429, errorMessage, responseBody, code, requestId);
    }
}
