// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Exception thrown when the Mockarty API returns an error response.
 *
 * <p>The Mockarty server emits a uniform JSON error envelope:</p>
 * <pre>{@code
 * {"error": "human message", "code": "not_found", "request_id": "..."}
 * }</pre>
 *
 * <p>The {@code code} field is the stable, machine-readable identifier for
 * the error category and should be preferred over HTTP status codes when
 * branching on errors.</p>
 */
public class MockartyApiException extends MockartyException {

    private final int statusCode;
    private final String errorMessage;
    private final String responseBody;
    private final String code;
    private final String requestId;

    public MockartyApiException(int statusCode, String errorMessage) {
        this(statusCode, errorMessage, null, null, null);
    }

    public MockartyApiException(int statusCode, String errorMessage, String responseBody) {
        this(statusCode, errorMessage, responseBody, null, null);
    }

    public MockartyApiException(
            int statusCode,
            String errorMessage,
            String responseBody,
            String code,
            String requestId) {
        super(formatMessage(statusCode, errorMessage, code, requestId));
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.responseBody = responseBody;
        this.code = code;
        this.requestId = requestId;
    }

    private static String formatMessage(int statusCode, String errorMessage, String code, String requestId) {
        StringBuilder sb = new StringBuilder("Mockarty API error [").append(statusCode);
        if (code != null && !code.isEmpty()) {
            sb.append(' ').append(code);
        }
        sb.append("]: ").append(errorMessage);
        if (requestId != null && !requestId.isEmpty()) {
            sb.append(" (request_id=").append(requestId).append(')');
        }
        return sb.toString();
    }

    /**
     * Returns the HTTP status code from the API response.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the sanitized human-readable error message from the API response.
     * Never contains SQL, stack traces, or internal paths.
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

    /**
     * Returns the stable machine-readable error code (e.g. {@code "not_found"}).
     * May be {@code null} when talking to an older server without the code field.
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the server-side correlation ID. Include it in bug reports.
     * May be {@code null} if the server did not emit one.
     */
    public String getRequestId() {
        return requestId;
    }
}
