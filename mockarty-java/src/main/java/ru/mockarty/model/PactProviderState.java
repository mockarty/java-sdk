// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Provider state required by a message interaction in a v4 Pact file.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PactProviderState {

    @JsonProperty("name")
    private String name;

    @JsonProperty("params")
    private Map<String, Object> params;

    public PactProviderState() {
    }

    public PactProviderState(String name) {
        this.name = name;
    }

    public PactProviderState name(String name) {
        this.name = name;
        return this;
    }

    public PactProviderState params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
