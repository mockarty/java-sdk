// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * A single request/response body inside a message interaction.
 * Async messages keep only {@code contents} + {@code metadata}; synchronous
 * messages use {@link PactMessageInteraction#getResponse()} to carry one
 * or more reply variants of this type.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PactMessageContent {

    @JsonProperty("contents")
    private Object contents;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public PactMessageContent() {
    }

    public PactMessageContent contents(Object contents) {
        this.contents = contents;
        return this;
    }

    public PactMessageContent metadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Object getContents() {
        return contents;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
