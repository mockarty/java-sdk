// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Input for building a specification FROM mocks — the reverse of
 * {@link GeneratorRequest}, which describes mocks by a specification.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecExportRequest {

    /** Required: {@code openapi} or {@code asyncapi}. */
    @JsonProperty("format")
    private String format;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("folderId")
    private String folderId;

    /** A mock must carry EVERY one of these tags to be described. */
    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("mockIds")
    private List<String> mockIds;

    @JsonProperty("title")
    private String title;

    @JsonProperty("version")
    private String version;

    /**
     * Server URL for {@code servers[]}. Leave it unset when the address is
     * unknown: a document that invents a server is wrong in a way its reader
     * cannot see.
     */
    @JsonProperty("serverUrl")
    private String serverUrl;

    public SpecExportRequest() {
    }

    public SpecExportRequest(String format) {
        this.format = format;
    }

    public SpecExportRequest format(String format) {
        this.format = format;
        return this;
    }

    public SpecExportRequest namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public SpecExportRequest folderId(String folderId) {
        this.folderId = folderId;
        return this;
    }

    public SpecExportRequest tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public SpecExportRequest mockIds(List<String> mockIds) {
        this.mockIds = mockIds;
        return this;
    }

    public SpecExportRequest title(String title) {
        this.title = title;
        return this;
    }

    public SpecExportRequest version(String version) {
        this.version = version;
        return this;
    }

    public SpecExportRequest serverUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getFolderId() {
        return folderId;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getMockIds() {
        return mockIds;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    @Override
    public String toString() {
        return "SpecExportRequest{format='" + format + "', namespace='" + namespace + "'}";
    }
}
