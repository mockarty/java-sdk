// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a folder for organizing mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MockFolder {

    @JsonProperty("id")
    private String id;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("parentId")
    private String parentId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("sortOrder")
    private Integer sortOrder;

    @JsonProperty("createdAt")
    private String createdAt;

    public MockFolder() {
    }

    // Builder-style setters

    public MockFolder id(String id) {
        this.id = id;
        return this;
    }

    public MockFolder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public MockFolder parentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    public MockFolder name(String name) {
        this.name = name;
        return this;
    }

    public MockFolder sortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "MockFolder{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
