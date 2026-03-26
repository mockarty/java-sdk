// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kafka request context for matching Kafka mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaRequestContext {

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("headers")
    private List<Condition> headers;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("consumerGroup")
    private String consumerGroup;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    @JsonProperty("outputTopic")
    private String outputTopic;

    @JsonProperty("outputBrokers")
    private String outputBrokers;

    @JsonProperty("outputKey")
    private String outputKey;

    @JsonProperty("outputHeaders")
    private Map<String, String> outputHeaders;

    public KafkaRequestContext() {
    }

    // Builder-style setters

    public KafkaRequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public KafkaRequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public KafkaRequestContext headers(List<Condition> headers) {
        this.headers = headers;
        return this;
    }

    public KafkaRequestContext addHeader(Condition condition) {
        if (this.headers == null) {
            this.headers = new ArrayList<>();
        }
        this.headers.add(condition);
        return this;
    }

    public KafkaRequestContext topic(String topic) {
        this.topic = topic;
        return this;
    }

    public KafkaRequestContext serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public KafkaRequestContext consumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
        return this;
    }

    public KafkaRequestContext sortArray(boolean sortArray) {
        this.sortArray = sortArray;
        return this;
    }

    public KafkaRequestContext outputTopic(String outputTopic) {
        this.outputTopic = outputTopic;
        return this;
    }

    public KafkaRequestContext outputBrokers(String outputBrokers) {
        this.outputBrokers = outputBrokers;
        return this;
    }

    public KafkaRequestContext outputKey(String outputKey) {
        this.outputKey = outputKey;
        return this;
    }

    public KafkaRequestContext outputHeaders(Map<String, String> outputHeaders) {
        this.outputHeaders = outputHeaders;
        return this;
    }

    // Getters

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Condition> getHeaders() {
        return headers;
    }

    public String getTopic() {
        return topic;
    }

    public String getServerName() {
        return serverName;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    public String getOutputTopic() {
        return outputTopic;
    }

    public String getOutputBrokers() {
        return outputBrokers;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public Map<String, String> getOutputHeaders() {
        return outputHeaders;
    }

    @Override
    public String toString() {
        return "KafkaRequestContext{" +
                "topic='" + topic + '\'' +
                ", serverName='" + serverName + '\'' +
                '}';
    }
}
