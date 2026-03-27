// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Contract definition for contract testing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Contract {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("consumer")
    private String consumer;

    @JsonProperty("spec")
    private Object spec;

    @JsonProperty("specUrl")
    private String specUrl;

    @JsonProperty("schedule")
    private String schedule;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public Contract() {
    }

    // Builder-style setters

    public Contract id(String id) {
        this.id = id;
        return this;
    }

    public Contract name(String name) {
        this.name = name;
        return this;
    }

    public Contract namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Contract protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public Contract provider(String provider) {
        this.provider = provider;
        return this;
    }

    public Contract consumer(String consumer) {
        this.consumer = consumer;
        return this;
    }

    public Contract spec(Object spec) {
        this.spec = spec;
        return this;
    }

    public Contract specUrl(String specUrl) {
        this.specUrl = specUrl;
        return this;
    }

    public Contract schedule(String schedule) {
        this.schedule = schedule;
        return this;
    }

    public Contract enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Contract tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getProvider() {
        return provider;
    }

    public String getConsumer() {
        return consumer;
    }

    public Object getSpec() {
        return spec;
    }

    public String getSpecUrl() {
        return specUrl;
    }

    public String getSchedule() {
        return schedule;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Contract{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", protocol='" + protocol + '\'' +
                '}';
    }
}
