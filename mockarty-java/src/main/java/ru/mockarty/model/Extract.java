// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Extract defines data extraction rules from request to stores.
 * Allows extracting values from the incoming request into Mock, Chain, or Global stores.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Extract {

    @JsonProperty("mStore")
    private Map<String, Object> mStore;

    @JsonProperty("cStore")
    private Map<String, Object> cStore;

    @JsonProperty("gStore")
    private Map<String, Object> gStore;

    public Extract() {
    }

    // Builder-style setters

    public Extract mStore(Map<String, Object> mStore) {
        this.mStore = mStore;
        return this;
    }

    public Extract cStore(Map<String, Object> cStore) {
        this.cStore = cStore;
        return this;
    }

    public Extract gStore(Map<String, Object> gStore) {
        this.gStore = gStore;
        return this;
    }

    // Getters

    public Map<String, Object> getMStore() {
        return mStore;
    }

    public Map<String, Object> getCStore() {
        return cStore;
    }

    public Map<String, Object> getGStore() {
        return gStore;
    }

    @Override
    public String toString() {
        return "Extract{" +
                "mStore=" + mStore +
                ", cStore=" + cStore +
                ", gStore=" + gStore +
                '}';
    }
}
