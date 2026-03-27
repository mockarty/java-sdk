// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a response template file stored on the server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateFile {

    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;

    @JsonProperty("size")
    private Long size;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public TemplateFile() {
    }

    // Builder-style setters

    public TemplateFile name(String name) {
        this.name = name;
        return this;
    }

    public TemplateFile path(String path) {
        this.path = path;
        return this;
    }

    public TemplateFile size(Long size) {
        this.size = size;
        return this;
    }

    public TemplateFile contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public TemplateFile namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public TemplateFile createdAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public TemplateFile updatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    // Getters

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "TemplateFile{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", size=" + size +
                '}';
    }
}
