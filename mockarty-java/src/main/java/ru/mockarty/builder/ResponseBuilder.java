// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.builder;

import ru.mockarty.model.ContentResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating ContentResponse objects.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * ContentResponse response = ResponseBuilder.create()
 *     .statusCode(200)
 *     .payload(Map.of("status", "ok"))
 *     .header("X-Custom-Header", "value")
 *     .delay(100)
 *     .build();
 * }</pre>
 */
public class ResponseBuilder {

    private int statusCode = 200;
    private Object payload;
    private String payloadTemplatePath;
    private String error;
    private Integer delay;
    private String decode;
    private Map<String, List<String>> headers;

    private ResponseBuilder() {
    }

    /**
     * Creates a new ResponseBuilder.
     */
    public static ResponseBuilder create() {
        return new ResponseBuilder();
    }

    /**
     * Creates a ResponseBuilder with the given status code.
     */
    public static ResponseBuilder status(int statusCode) {
        ResponseBuilder builder = new ResponseBuilder();
        builder.statusCode = statusCode;
        return builder;
    }

    /**
     * Sets the HTTP status code.
     */
    public ResponseBuilder statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Sets the response payload (body).
     */
    public ResponseBuilder payload(Object payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Sets the payload template path for template-based responses.
     */
    public ResponseBuilder payloadTemplatePath(String path) {
        this.payloadTemplatePath = path;
        return this;
    }

    /**
     * Sets the error message for error responses.
     */
    public ResponseBuilder error(String error) {
        this.error = error;
        return this;
    }

    /**
     * Sets the response delay in milliseconds.
     */
    public ResponseBuilder delay(int delayMs) {
        this.delay = delayMs;
        return this;
    }

    /**
     * Sets the decode type (e.g., "base64").
     */
    public ResponseBuilder decode(String decode) {
        this.decode = decode;
        return this;
    }

    /**
     * Adds a response header with a single value.
     */
    public ResponseBuilder header(String name, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Adds a response header with multiple values.
     */
    public ResponseBuilder header(String name, String... values) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(name, new ArrayList<>(Arrays.asList(values)));
        return this;
    }

    /**
     * Sets all response headers.
     */
    public ResponseBuilder headers(Map<String, List<String>> headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Builds the ContentResponse object.
     */
    public ContentResponse build() {
        ContentResponse response = new ContentResponse()
                .statusCode(statusCode);

        if (payload != null) {
            response.payload(payload);
        }
        if (payloadTemplatePath != null) {
            response.payloadTemplatePath(payloadTemplatePath);
        }
        if (error != null) {
            response.error(error);
        }
        if (delay != null) {
            response.delay(delay);
        }
        if (decode != null) {
            response.decode(decode);
        }
        if (headers != null) {
            response.headers(headers);
        }

        return response;
    }
}
