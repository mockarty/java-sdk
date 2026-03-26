// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Pact contract for consumer-driven contract testing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pact {

    @JsonProperty("id")
    private String id;

    @JsonProperty("consumer")
    private String consumer;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("version")
    private String version;

    @JsonProperty("spec")
    private Object spec;

    @JsonProperty("specUrl")
    private String specUrl;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("createdAt")
    private String createdAt;

    public Pact() {
    }

    // Builder-style setters

    public Pact id(String id) {
        this.id = id;
        return this;
    }

    public Pact consumer(String consumer) {
        this.consumer = consumer;
        return this;
    }

    public Pact provider(String provider) {
        this.provider = provider;
        return this;
    }

    public Pact version(String version) {
        this.version = version;
        return this;
    }

    public Pact spec(Object spec) {
        this.spec = spec;
        return this;
    }

    public Pact specUrl(String specUrl) {
        this.specUrl = specUrl;
        return this;
    }

    public Pact namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getConsumer() {
        return consumer;
    }

    public String getProvider() {
        return provider;
    }

    public String getVersion() {
        return version;
    }

    public Object getSpec() {
        return spec;
    }

    public String getSpecUrl() {
        return specUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Pact{" +
                "id='" + id + '\'' +
                ", consumer='" + consumer + '\'' +
                ", provider='" + provider + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
