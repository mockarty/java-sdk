// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The create payload for a delegated credential.
 *
 * <p>Note what is NOT here: no issuer field, and nothing that could later extend
 * a live credential. The issuer is taken from the authenticated principal and
 * the scope is frozen at creation.
 */
public final class DelegatedCredentialCreateRequest {
    private final String name;
    private final String description;
    private final List<String> namespaces;
    private final List<String> allowedActions;
    private final Instant expiresAt;
    private final Map<String, Object> metadata;

    private DelegatedCredentialCreateRequest(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.namespaces = List.copyOf(builder.namespaces);
        this.allowedActions = List.copyOf(builder.allowedActions);
        this.expiresAt = builder.expiresAt;
        this.metadata = builder.metadata == null ? null : Map.copyOf(builder.metadata);
    }

    public static Builder builder() { return new Builder(); }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getNamespaces() { return Collections.unmodifiableList(namespaces); }

    /** Empty means "the server's full vocabulary" (read, write, delete). */
    public List<String> getAllowedActions() { return Collections.unmodifiableList(allowedActions); }

    /** Required. An unbounded delegated credential is an API key. */
    public Instant getExpiresAt() { return expiresAt; }

    public Map<String, Object> getMetadata() { return metadata; }

    /** Fluent builder; {@code expiresAt} and at least one namespace are required. */
    public static final class Builder {
        private String name = "";
        private String description = "";
        private List<String> namespaces = List.of();
        private List<String> allowedActions = List.of();
        private Instant expiresAt;
        private Map<String, Object> metadata;

        public Builder name(String value) { this.name = value == null ? "" : value; return this; }
        public Builder description(String value) { this.description = value == null ? "" : value; return this; }
        public Builder namespaces(List<String> value) { this.namespaces = value == null ? List.of() : value; return this; }
        public Builder allowedActions(List<String> value) { this.allowedActions = value == null ? List.of() : value; return this; }
        public Builder expiresAt(Instant value) { this.expiresAt = value; return this; }
        public Builder metadata(Map<String, Object> value) { this.metadata = value; return this; }

        public DelegatedCredentialCreateRequest build() { return new DelegatedCredentialCreateRequest(this); }
    }
}
