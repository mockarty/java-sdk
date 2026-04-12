// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents an async or sync message interaction from a v4 Pact file.
 *
 * <p>Async messages populate {@code contents} + {@code metadata}.
 * Synchronous messages additionally populate {@link #getResponse()} with
 * one or more reply variants.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PactMessageInteraction {

    /** Either {@code "async"} or {@code "sync"}. */
    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("providerStates")
    private List<PactProviderState> providerStates;

    @JsonProperty("contents")
    private Object contents;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /** Expected reply variants for synchronous messages. */
    @JsonProperty("response")
    private List<PactMessageContent> response;

    public PactMessageInteraction() {
    }

    public PactMessageInteraction type(String type) {
        this.type = type;
        return this;
    }

    public PactMessageInteraction description(String description) {
        this.description = description;
        return this;
    }

    public PactMessageInteraction providerStates(List<PactProviderState> providerStates) {
        this.providerStates = providerStates;
        return this;
    }

    public PactMessageInteraction contents(Object contents) {
        this.contents = contents;
        return this;
    }

    public PactMessageInteraction metadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public PactMessageInteraction response(List<PactMessageContent> response) {
        this.response = response;
        return this;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public List<PactProviderState> getProviderStates() {
        return providerStates;
    }

    public Object getContents() {
        return contents;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<PactMessageContent> getResponse() {
        return response;
    }
}
