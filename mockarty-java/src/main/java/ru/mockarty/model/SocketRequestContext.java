// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Socket (WebSocket/Socket.IO) request context for matching socket mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocketRequestContext {

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("event")
    private String event;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    public SocketRequestContext() {
    }

    // Builder-style setters

    public SocketRequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public SocketRequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public SocketRequestContext serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public SocketRequestContext event(String event) {
        this.event = event;
        return this;
    }

    public SocketRequestContext namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public SocketRequestContext sortArray(boolean sortArray) {
        this.sortArray = sortArray;
        return this;
    }

    // Getters

    public List<Condition> getConditions() {
        return conditions;
    }

    public String getServerName() {
        return serverName;
    }

    public String getEvent() {
        return event;
    }

    public String getNamespace() {
        return namespace;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    @Override
    public String toString() {
        return "SocketRequestContext{" +
                "serverName='" + serverName + '\'' +
                ", event='" + event + '\'' +
                '}';
    }
}
