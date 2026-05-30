// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned when creating or updating a mock.
 *
 * <p>Wire shape (admin node, {@code POST /api/v1/mocks}):
 * <pre>{@code
 * {
 *   "id":      "<mock-id>",
 *   "mock":    {...full mock...},
 *   "isNew":   <true|false>,   // true when an existing mock with this id was replaced
 *   "success": true,
 *   "message": "Mock created successfully"
 * }
 * }</pre>
 *
 * <p>The server's {@code isNew} field is semantically <em>"was overwrite"</em>
 * (it's true when an existing record was replaced). This SDK exposes a
 * cleaner {@link #isOverwritten()} alias so client code reads naturally.
 * The legacy {@code overwritten} field is also accepted on read for
 * forward compatibility with future server cleanups.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveMockResponse {

    /**
     * Server-side flag — true when the create call replaced an existing
     * mock with the same id. The server JSON key is {@code "isNew"}
     * (historic naming — semantics are "was overwrite"). We also accept
     * {@code "overwritten"} via {@link JsonAlias} so the SDK round-trips
     * cleanly if/when the server settles on the cleaner name.
     */
    @JsonProperty("isNew")
    @JsonAlias({"overwritten"})
    private boolean overwritten;

    @JsonProperty("mock")
    private Mock mock;

    /** Server echo of the persisted mock id. Same value as {@code mock.id}. */
    @JsonProperty("id")
    private String id;

    /** Human-readable status message — populated on every save. */
    @JsonProperty("message")
    private String message;

    /** Server-side success flag — true unless the call hit a fail-fast path
     * that still returns 2xx (rare). Clients should rely on HTTP status. */
    @JsonProperty("success")
    private Boolean success;

    public SaveMockResponse() {
    }

    /**
     * True when an existing mock with the same id was replaced; false when
     * a brand-new record was inserted. Always reflects the server's
     * {@code isNew} field (which is, despite the name, the "was overwrite"
     * signal — see class javadoc).
     */
    public boolean isOverwritten() {
        return overwritten;
    }

    public void setOverwritten(boolean overwritten) {
        this.overwritten = overwritten;
    }

    /**
     * Returns the raw server flag verbatim. Identical to
     * {@link #isOverwritten()} — provided for callers that prefer reading
     * the on-the-wire name. Marked {@code @JsonIgnore} so Jackson doesn't
     * try to round-trip it back to the server.
     */
    @JsonIgnore
    public boolean isNew() {
        return overwritten;
    }

    public Mock getMock() {
        return mock;
    }

    public void setMock(Mock mock) {
        this.mock = mock;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "SaveMockResponse{" +
                "overwritten=" + overwritten +
                ", id=" + id +
                ", mock=" + mock +
                '}';
    }
}
