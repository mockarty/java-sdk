// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable view of the response the consumer expects to receive.
 */
public final class PactResponse {

    private final int status;
    private final Map<String, Object> headers;
    private final PactBody body;

    PactResponse(int status, Map<String, Object> headers, PactBody body) {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("response status must be 1xx-5xx, got " + status);
        }
        this.status = status;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(headers)));
        this.body = body == null ? PactBody.empty() : body;
    }

    public int status() { return status; }
    public Map<String, Object> headers() { return headers; }
    public PactBody body() { return body; }
}
