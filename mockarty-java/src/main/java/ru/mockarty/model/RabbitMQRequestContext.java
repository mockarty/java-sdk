// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RabbitMQ request context for matching RabbitMQ mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RabbitMQRequestContext {

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("headers")
    private List<Condition> headers;

    @JsonProperty("queue")
    private String queue;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("routingKey")
    private String routingKey;

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    @JsonProperty("outputURL")
    private String outputURL;

    @JsonProperty("outputExchange")
    private String outputExchange;

    @JsonProperty("outputRoutingKey")
    private String outputRoutingKey;

    @JsonProperty("outputQueue")
    private String outputQueue;

    @JsonProperty("outputProps")
    private Map<String, Object> outputProps;

    public RabbitMQRequestContext() {
    }

    // Builder-style setters

    public RabbitMQRequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public RabbitMQRequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public RabbitMQRequestContext headers(List<Condition> headers) {
        this.headers = headers;
        return this;
    }

    public RabbitMQRequestContext addHeader(Condition condition) {
        if (this.headers == null) {
            this.headers = new ArrayList<>();
        }
        this.headers.add(condition);
        return this;
    }

    public RabbitMQRequestContext queue(String queue) {
        this.queue = queue;
        return this;
    }

    public RabbitMQRequestContext exchange(String exchange) {
        this.exchange = exchange;
        return this;
    }

    public RabbitMQRequestContext routingKey(String routingKey) {
        this.routingKey = routingKey;
        return this;
    }

    public RabbitMQRequestContext serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public RabbitMQRequestContext sortArray(boolean sortArray) {
        this.sortArray = sortArray;
        return this;
    }

    public RabbitMQRequestContext outputURL(String outputURL) {
        this.outputURL = outputURL;
        return this;
    }

    public RabbitMQRequestContext outputExchange(String outputExchange) {
        this.outputExchange = outputExchange;
        return this;
    }

    public RabbitMQRequestContext outputRoutingKey(String outputRoutingKey) {
        this.outputRoutingKey = outputRoutingKey;
        return this;
    }

    public RabbitMQRequestContext outputQueue(String outputQueue) {
        this.outputQueue = outputQueue;
        return this;
    }

    public RabbitMQRequestContext outputProps(Map<String, Object> outputProps) {
        this.outputProps = outputProps;
        return this;
    }

    // Getters

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Condition> getHeaders() {
        return headers;
    }

    public String getQueue() {
        return queue;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getServerName() {
        return serverName;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    public String getOutputURL() {
        return outputURL;
    }

    public String getOutputExchange() {
        return outputExchange;
    }

    public String getOutputRoutingKey() {
        return outputRoutingKey;
    }

    public String getOutputQueue() {
        return outputQueue;
    }

    public Map<String, Object> getOutputProps() {
        return outputProps;
    }

    @Override
    public String toString() {
        return "RabbitMQRequestContext{" +
                "queue='" + queue + '\'' +
                ", exchange='" + exchange + '\'' +
                '}';
    }
}
