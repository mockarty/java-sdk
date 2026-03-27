// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Kubernetes cluster infrastructure profile for chaos engineering.
 *
 * <p>Maps to the server-side InfraProfile struct in internal/chaos/models.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChaosProfile {

    @JsonProperty("id")
    private String id;

    @JsonProperty("namespaceId")
    private String namespaceId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("kubeconfigPath")
    private String kubeconfigPath;

    @JsonProperty("kubeconfigData")
    private String kubeconfigData;

    @JsonProperty("context")
    private String context;

    @JsonProperty("inCluster")
    private Boolean inCluster;

    @JsonProperty("defaultNamespace")
    private String defaultNamespace;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public ChaosProfile() {
    }

    // Builder-style setters

    public ChaosProfile id(String id) {
        this.id = id;
        return this;
    }

    public ChaosProfile namespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
        return this;
    }

    public ChaosProfile name(String name) {
        this.name = name;
        return this;
    }

    public ChaosProfile kubeconfigPath(String kubeconfigPath) {
        this.kubeconfigPath = kubeconfigPath;
        return this;
    }

    public ChaosProfile kubeconfigData(String kubeconfigData) {
        this.kubeconfigData = kubeconfigData;
        return this;
    }

    public ChaosProfile context(String context) {
        this.context = context;
        return this;
    }

    public ChaosProfile inCluster(Boolean inCluster) {
        this.inCluster = inCluster;
        return this;
    }

    public ChaosProfile defaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public String getName() {
        return name;
    }

    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    public String getKubeconfigData() {
        return kubeconfigData;
    }

    public String getContext() {
        return context;
    }

    public Boolean getInCluster() {
        return inCluster;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "ChaosProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", context='" + context + '\'' +
                '}';
    }
}
