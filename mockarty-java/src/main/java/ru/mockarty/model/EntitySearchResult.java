// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single picker row returned by the unified entity-search endpoint.
 *
 * <p>Mirrors {@code EntitySearchItem} in
 * {@code internal/webui/entity_search_handlers.go}. {@code numericId} is
 * boxed (nullable) because most entity types do not carry a human-friendly
 * numeric identifier — {@code null} simply means "not applicable for this
 * row".</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntitySearchResult {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("numericId")
    private Long numericId;

    public String getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getNamespace() { return namespace; }
    public String getCreatedAt() { return createdAt; }
    public Long getNumericId() { return numericId; }
}
