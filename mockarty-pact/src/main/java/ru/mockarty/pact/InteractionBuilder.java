// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for a single {@link Interaction}.
 *
 * <p>Order: zero-or-more {@code given(...)}, exactly one
 * {@code uponReceiving(...)}, exactly one {@code withRequest(...)}, then
 * the response side (terminated by {@code willRespondWith(...)} + optional
 * headers / body). The builder validates the order itself — calling
 * {@code willRespondWith} without a request first is a hard error, not a
 * silent producer of nonsense JSON.</p>
 */
public final class InteractionBuilder {

    private final List<ProviderState> providerStates = new ArrayList<>();
    private String description;

    // Request side
    private String reqMethod;
    private String reqPath;
    private Object reqPathMatcher;
    private final Map<String, Object> reqHeaders = new LinkedHashMap<>();
    private final Map<String, List<Object>> reqQuery = new LinkedHashMap<>();
    private PactBody reqBody = PactBody.empty();
    private boolean requestDeclared = false;

    // Response side
    private Integer respStatus;
    private final Map<String, Object> respHeaders = new LinkedHashMap<>();
    private PactBody respBody = PactBody.empty();
    private boolean responseDeclared = false;

    InteractionBuilder() {
        // Package-private — created by Consumer.addInteraction.
    }

    // ── Given ────────────────────────────────────────────────────────

    /** Add a provider-state declaration (string only). */
    public InteractionBuilder given(String state) {
        providerStates.add(ProviderState.of(state));
        return this;
    }

    /** Add a provider-state declaration with parameters (V4 only). */
    public InteractionBuilder given(String state, Map<String, Object> params) {
        providerStates.add(ProviderState.of(state, params));
        return this;
    }

    // ── Description ──────────────────────────────────────────────────

    /** Set the {@code description} field — required. */
    public InteractionBuilder uponReceiving(String description) {
        Objects.requireNonNull(description, "uponReceiving: description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("uponReceiving: description must not be blank");
        }
        this.description = description;
        return this;
    }

    // ── Request ──────────────────────────────────────────────────────

    /** Declare the request method + path (literal). */
    public InteractionBuilder withRequest(String method, String path) {
        Objects.requireNonNull(method, "withRequest: method must not be null");
        Objects.requireNonNull(path, "withRequest: path must not be null");
        this.reqMethod = method;
        this.reqPath = path;
        this.reqPathMatcher = null;
        this.requestDeclared = true;
        return this;
    }

    /** Declare the request method + path matched via a regex (or other matcher). */
    public InteractionBuilder withRequest(String method, String examplePath, Matcher pathMatcher) {
        Objects.requireNonNull(pathMatcher, "withRequest(matcher): pathMatcher must not be null");
        withRequest(method, examplePath);
        this.reqPathMatcher = pathMatcher;
        return this;
    }

    /** Add a single request header. Value may be a literal or a {@link Matcher}. */
    public InteractionBuilder withHeader(String name, Object value) {
        requireRequestStarted("withHeader");
        Objects.requireNonNull(name, "withHeader: name must not be null");
        Objects.requireNonNull(value, "withHeader: value must not be null");
        reqHeaders.put(name, value);
        return this;
    }

    /** Add multiple request headers at once. */
    public InteractionBuilder withHeaders(Map<String, Object> headers) {
        requireRequestStarted("withHeaders");
        Objects.requireNonNull(headers, "withHeaders: headers must not be null");
        reqHeaders.putAll(headers);
        return this;
    }

    /** Add a single query-string entry. Values may be literals or {@link Matcher}s. */
    public InteractionBuilder withQuery(String name, Object... values) {
        requireRequestStarted("withQuery");
        Objects.requireNonNull(name, "withQuery: name must not be null");
        Objects.requireNonNull(values, "withQuery: values must not be null");
        if (values.length == 0) {
            throw new IllegalArgumentException("withQuery: at least one value required");
        }
        reqQuery.put(name, Arrays.asList(values));
        return this;
    }

    /** JSON request body (when called before {@link #willRespondWith(int)})
     * or JSON response body (when called after). The runtime decision
     * lets the user re-use a single fluent method on both sides. */
    public InteractionBuilder withJsonBody(Map<String, Object> body) {
        Objects.requireNonNull(body, "withJsonBody: body must not be null");
        if (responseDeclared) {
            // Route to the response side. responseDeclared turns true the
            // moment willRespondWith fires — everything after that targets
            // the response body.
            respHeaders.putIfAbsent("Content-Type", "application/json");
            this.respBody = PactBody.json(body);
        } else {
            requireRequestStarted("withJsonBody");
            reqHeaders.putIfAbsent("Content-Type", "application/json");
            this.reqBody = PactBody.json(body);
        }
        return this;
    }

    /** Plain text request body. */
    public InteractionBuilder withTextBody(String body, String contentType) {
        requireRequestStarted("withTextBody");
        this.reqBody = new PactBody.Text(body, contentType);
        reqHeaders.putIfAbsent("Content-Type", contentType);
        return this;
    }

    /** Binary request body (V4 only). */
    public InteractionBuilder withBinaryBody(byte[] body, String contentType) {
        requireRequestStarted("withBinaryBody");
        this.reqBody = new PactBody.Binary(body, contentType);
        reqHeaders.putIfAbsent("Content-Type", contentType);
        return this;
    }

    // ── Response ─────────────────────────────────────────────────────

    /** Declare the response status code — required exactly once. */
    public InteractionBuilder willRespondWith(int status) {
        if (!requestDeclared) {
            throw new IllegalStateException("willRespondWith: call withRequest(...) first");
        }
        this.respStatus = status;
        this.responseDeclared = true;
        return this;
    }

    /** Add a single response header. */
    public InteractionBuilder withResponseHeader(String name, Object value) {
        requireResponseStarted("withResponseHeader");
        respHeaders.put(name, value);
        return this;
    }

    /** Add multiple response headers at once. */
    public InteractionBuilder withResponseHeaders(Map<String, Object> headers) {
        requireResponseStarted("withResponseHeaders");
        respHeaders.putAll(headers);
        return this;
    }

    /** JSON response body that's a top-level list (rare, but legal). The
     * Map-shaped variant is the more common case and is handled by the
     * {@link #withJsonBody(Map)} overload — the runtime route to request
     * vs response side is the same. */
    public InteractionBuilder withJsonArrayBody(java.util.List<?> body) {
        Objects.requireNonNull(body, "withJsonArrayBody: body must not be null");
        if (responseDeclared) {
            respHeaders.putIfAbsent("Content-Type", "application/json");
            this.respBody = new PactBody.Json(body);
        } else {
            requireRequestStarted("withJsonArrayBody");
            reqHeaders.putIfAbsent("Content-Type", "application/json");
            this.reqBody = new PactBody.Json(body);
        }
        return this;
    }

    /** Plain text response body. */
    public InteractionBuilder withResponseTextBody(String body, String contentType) {
        requireResponseStarted("withResponseTextBody");
        this.respBody = new PactBody.Text(body, contentType);
        respHeaders.putIfAbsent("Content-Type", contentType);
        return this;
    }

    /** Binary response body (V4 only). */
    public InteractionBuilder withResponseBinaryBody(byte[] body, String contentType) {
        requireResponseStarted("withResponseBinaryBody");
        this.respBody = new PactBody.Binary(body, contentType);
        respHeaders.putIfAbsent("Content-Type", contentType);
        return this;
    }

    // ── Build ────────────────────────────────────────────────────────

    Interaction build() {
        if (description == null) {
            throw new IllegalStateException("addInteraction: uponReceiving(description) is required");
        }
        if (!requestDeclared) {
            throw new IllegalStateException("addInteraction: withRequest(method, path) is required");
        }
        if (!responseDeclared) {
            throw new IllegalStateException("addInteraction: willRespondWith(status) is required");
        }
        PactRequest req = new PactRequest(
                reqMethod, reqPath, reqPathMatcher, reqHeaders, reqQuery, reqBody);
        PactResponse resp = new PactResponse(respStatus, respHeaders, respBody);
        return new Interaction(description, new ArrayList<>(providerStates), req, resp);
    }

    private void requireRequestStarted(String op) {
        if (!requestDeclared) {
            throw new IllegalStateException(op + ": call withRequest(...) first");
        }
    }

    private void requireResponseStarted(String op) {
        if (!responseDeclared) {
            throw new IllegalStateException(op + ": call willRespondWith(status) first");
        }
    }
}
