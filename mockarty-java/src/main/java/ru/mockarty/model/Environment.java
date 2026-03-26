// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents an API tester environment with variables.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Environment {

    @JsonProperty("id")
    private String id;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("name")
    private String name;

    @JsonProperty("variables")
    private Map<String, String> variables;

    @JsonProperty("isActive")
    private Boolean isActive;

    public Environment() {
    }

    // Builder-style setters

    public Environment id(String id) {
        this.id = id;
        return this;
    }

    public Environment namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Environment name(String name) {
        this.name = name;
        return this;
    }

    public Environment variables(Map<String, String> variables) {
        this.variables = variables;
        return this;
    }

    public Environment isActive(Boolean isActive) {
        this.isActive = isActive;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    @Override
    public String toString() {
        return "Environment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
