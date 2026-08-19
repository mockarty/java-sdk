// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A party (consumer or provider) named in a Pact contract. The server emits
 * each side as an object ({@code {"name": "WebApp"}}), matching the Go/Python
 * SDKs' {@code PactParty} / consumer-name shape.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PactParty {

    @JsonProperty("name")
    private String name;

    public PactParty() {
    }

    public PactParty(String name) {
        this.name = name;
    }

    public PactParty name(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
