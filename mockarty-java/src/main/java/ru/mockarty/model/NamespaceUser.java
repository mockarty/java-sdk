// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a user's membership in a namespace with their role.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamespaceUser {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("role")
    private String role;

    @JsonProperty("addedAt")
    private String addedAt;

    public NamespaceUser() {
    }

    // Builder-style setters

    public NamespaceUser userId(String userId) {
        this.userId = userId;
        return this;
    }

    public NamespaceUser username(String username) {
        this.username = username;
        return this;
    }

    public NamespaceUser role(String role) {
        this.role = role;
        return this;
    }

    public NamespaceUser addedAt(String addedAt) {
        this.addedAt = addedAt;
        return this;
    }

    // Getters

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getAddedAt() {
        return addedAt;
    }

    @Override
    public String toString() {
        return "NamespaceUser{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
