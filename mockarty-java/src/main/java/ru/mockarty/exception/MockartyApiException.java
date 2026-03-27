// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when the Mockarty API returns an error response.
 */
public class MockartyApiException extends MockartyException {

    private final int statusCode;
    private final String errorMessage;
    private final String responseBody;

    public MockartyApiException(int statusCode, String errorMessage) {
        this(statusCode, errorMessage, null);
    }

    public MockartyApiException(int statusCode, String errorMessage, String responseBody) {
        super("Mockarty API error [" + statusCode + "]: " + errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.responseBody = responseBody;
    }

    /**
     * Returns the HTTP status code from the API response.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the error message from the API response.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the raw response body, if available.
     */
    public String getResponseBody() {
        return responseBody;
    }
}
