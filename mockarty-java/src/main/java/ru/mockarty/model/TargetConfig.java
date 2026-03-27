// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Configuration for targeting specific resources in a chaos experiment.
 *
 * <p>Maps to the server-side TargetConfig struct in internal/chaos/models.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TargetConfig {

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("selector")
    private Map<String, String> selector;

    @JsonProperty("deployment")
    private String deployment;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("podNames")
    private List<String> podNames;

    @JsonProperty("nodeName")
    private String nodeName;

    @JsonProperty("percentage")
    private Integer percentage;

    public TargetConfig() {
    }

    // Builder-style setters

    public TargetConfig mode(String mode) {
        this.mode = mode;
        return this;
    }

    public TargetConfig selector(Map<String, String> selector) {
        this.selector = selector;
        return this;
    }

    public TargetConfig deployment(String deployment) {
        this.deployment = deployment;
        return this;
    }

    public TargetConfig namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public TargetConfig podNames(List<String> podNames) {
        this.podNames = podNames;
        return this;
    }

    public TargetConfig nodeName(String nodeName) {
        this.nodeName = nodeName;
        return this;
    }

    public TargetConfig percentage(Integer percentage) {
        this.percentage = percentage;
        return this;
    }

    // Getters

    public String getMode() {
        return mode;
    }

    public Map<String, String> getSelector() {
        return selector;
    }

    public String getDeployment() {
        return deployment;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<String> getPodNames() {
        return podNames;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Integer getPercentage() {
        return percentage;
    }

    @Override
    public String toString() {
        return "TargetConfig{" +
                "mode='" + mode + '\'' +
                ", deployment='" + deployment + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
