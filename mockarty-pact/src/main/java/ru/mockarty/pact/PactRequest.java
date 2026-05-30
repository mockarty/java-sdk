// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable view of an HTTP request the consumer expects to make.
 *
 * <p>{@code headers} and {@code query} use insertion-ordered maps because
 * the Pact reference spec is deterministic about the order matchers are
 * emitted into {@code matchingRules}, and bug-hunting diffs are easier on
 * a stable order. Matchers can appear as values inside any of the maps.</p>
 */
public final class PactRequest {

    private final String method;
    private final String path;
    private final Object pathMatcher;
    private final Map<String, Object> headers;
    private final Map<String, List<Object>> query;
    private final PactBody body;

    PactRequest(
            String method,
            String path,
            Object pathMatcher,
            Map<String, Object> headers,
            Map<String, List<Object>> query,
            PactBody body) {
        this.method = Objects.requireNonNull(method, "method must not be null").toUpperCase();
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.pathMatcher = pathMatcher;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.query = Collections.unmodifiableMap(new LinkedHashMap<>(query));
        this.body = body == null ? PactBody.empty() : body;
    }

    public String method() { return method; }
    public String path() { return path; }
    /** Optional path matcher (e.g. a regex). null if path is matched verbatim. */
    public Object pathMatcher() { return pathMatcher; }
    public Map<String, Object> headers() { return headers; }
    public Map<String, List<Object>> query() { return query; }
    public PactBody body() { return body; }
}
