// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for mock generation from API specifications.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneratorRequest {

    @JsonProperty("spec")
    private String spec;

    @JsonProperty("url")
    private String url;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("pathPrefix")
    private String pathPrefix;

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("graphqlUrl")
    private String graphqlUrl;

    @JsonProperty("protoContent")
    private String protoContent;

    @JsonProperty("wsdlContent")
    private String wsdlContent;

    @JsonProperty("harContent")
    private String harContent;

    public GeneratorRequest() {
    }

    // Builder-style setters

    public GeneratorRequest spec(String spec) {
        this.spec = spec;
        return this;
    }

    public GeneratorRequest url(String url) {
        this.url = url;
        return this;
    }

    public GeneratorRequest namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public GeneratorRequest pathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
        return this;
    }

    public GeneratorRequest serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public GeneratorRequest graphqlUrl(String graphqlUrl) {
        this.graphqlUrl = graphqlUrl;
        return this;
    }

    public GeneratorRequest protoContent(String protoContent) {
        this.protoContent = protoContent;
        return this;
    }

    public GeneratorRequest wsdlContent(String wsdlContent) {
        this.wsdlContent = wsdlContent;
        return this;
    }

    public GeneratorRequest harContent(String harContent) {
        this.harContent = harContent;
        return this;
    }

    // Getters

    public String getSpec() {
        return spec;
    }

    public String getUrl() {
        return url;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public String getServerName() {
        return serverName;
    }

    public String getGraphqlUrl() {
        return graphqlUrl;
    }

    public String getProtoContent() {
        return protoContent;
    }

    public String getWsdlContent() {
        return wsdlContent;
    }

    public String getHarContent() {
        return harContent;
    }

    @Override
    public String toString() {
        return "GeneratorRequest{" +
                "namespace='" + namespace + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
