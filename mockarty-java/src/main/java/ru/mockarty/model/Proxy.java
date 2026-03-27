// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Proxy configuration for forwarding requests to a real backend service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Proxy {

    @JsonProperty("target")
    private String target;

    public Proxy() {
    }

    public Proxy(String target) {
        this.target = target;
    }

    public static Proxy to(String target) {
        return new Proxy(target);
    }

    public Proxy target(String target) {
        this.target = target;
        return this;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "Proxy{target='" + target + "'}";
    }
}
