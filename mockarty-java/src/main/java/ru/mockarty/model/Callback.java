// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Callback (webhook) configuration that fires after a mock is matched.
 * Supports HTTP, Kafka, and RabbitMQ callback types.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Callback {

    @JsonProperty("type")
    private String type;

    @JsonProperty("url")
    private String url;

    @JsonProperty("method")
    private String method;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("body")
    private Object body;

    @JsonProperty("timeout")
    private Integer timeout;

    @JsonProperty("retryCount")
    private Integer retryCount;

    @JsonProperty("retryDelay")
    private Integer retryDelay;

    @JsonProperty("async")
    private Boolean async;

    @JsonProperty("trigger")
    private String trigger;

    // Kafka-specific fields

    @JsonProperty("kafkaBrokers")
    private String kafkaBrokers;

    @JsonProperty("kafkaTopic")
    private String kafkaTopic;

    @JsonProperty("kafkaKey")
    private String kafkaKey;

    @JsonProperty("kafkaUsername")
    private String kafkaUsername;

    @JsonProperty("kafkaPassword")
    private String kafkaPassword;

    @JsonProperty("kafkaUseSASL")
    private Boolean kafkaUseSASL;

    @JsonProperty("kafkaUseTLS")
    private Boolean kafkaUseTLS;

    // RabbitMQ-specific fields

    @JsonProperty("rabbitURL")
    private String rabbitURL;

    @JsonProperty("rabbitExchange")
    private String rabbitExchange;

    @JsonProperty("rabbitRoutingKey")
    private String rabbitRoutingKey;

    @JsonProperty("rabbitQueue")
    private String rabbitQueue;

    @JsonProperty("rabbitMandatory")
    private Boolean rabbitMandatory;

    public Callback() {
    }

    /**
     * Creates an HTTP callback with URL, method, and body.
     */
    public static Callback http(String url, String method, Object body) {
        Callback cb = new Callback();
        cb.type = "http";
        cb.url = url;
        cb.method = method;
        cb.body = body;
        return cb;
    }

    /**
     * Creates a Kafka callback.
     */
    public static Callback kafka(String brokers, String topic, Object body) {
        Callback cb = new Callback();
        cb.type = "kafka";
        cb.kafkaBrokers = brokers;
        cb.kafkaTopic = topic;
        cb.body = body;
        return cb;
    }

    /**
     * Creates a RabbitMQ callback.
     */
    public static Callback rabbitmq(String rabbitURL, String exchange, String routingKey, Object body) {
        Callback cb = new Callback();
        cb.type = "rabbitmq";
        cb.rabbitURL = rabbitURL;
        cb.rabbitExchange = exchange;
        cb.rabbitRoutingKey = routingKey;
        cb.body = body;
        return cb;
    }

    // Builder-style setters

    public Callback type(String type) {
        this.type = type;
        return this;
    }

    public Callback url(String url) {
        this.url = url;
        return this;
    }

    public Callback method(String method) {
        this.method = method;
        return this;
    }

    public Callback headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public Callback body(Object body) {
        this.body = body;
        return this;
    }

    public Callback timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Callback retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public Callback retryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
        return this;
    }

    public Callback async(boolean async) {
        this.async = async;
        return this;
    }

    public Callback trigger(String trigger) {
        this.trigger = trigger;
        return this;
    }

    public Callback kafkaBrokers(String kafkaBrokers) {
        this.kafkaBrokers = kafkaBrokers;
        return this;
    }

    public Callback kafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
        return this;
    }

    public Callback kafkaKey(String kafkaKey) {
        this.kafkaKey = kafkaKey;
        return this;
    }

    public Callback rabbitURL(String rabbitURL) {
        this.rabbitURL = rabbitURL;
        return this;
    }

    public Callback rabbitExchange(String rabbitExchange) {
        this.rabbitExchange = rabbitExchange;
        return this;
    }

    public Callback rabbitRoutingKey(String rabbitRoutingKey) {
        this.rabbitRoutingKey = rabbitRoutingKey;
        return this;
    }

    public Callback rabbitQueue(String rabbitQueue) {
        this.rabbitQueue = rabbitQueue;
        return this;
    }

    // Getters

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getBody() {
        return body;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Integer getRetryDelay() {
        return retryDelay;
    }

    public Boolean getAsync() {
        return async;
    }

    public String getTrigger() {
        return trigger;
    }

    public String getKafkaBrokers() {
        return kafkaBrokers;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public String getKafkaKey() {
        return kafkaKey;
    }

    public String getKafkaUsername() {
        return kafkaUsername;
    }

    public String getKafkaPassword() {
        return kafkaPassword;
    }

    public Boolean getKafkaUseSASL() {
        return kafkaUseSASL;
    }

    public Boolean getKafkaUseTLS() {
        return kafkaUseTLS;
    }

    public String getRabbitURL() {
        return rabbitURL;
    }

    public String getRabbitExchange() {
        return rabbitExchange;
    }

    public String getRabbitRoutingKey() {
        return rabbitRoutingKey;
    }

    public String getRabbitQueue() {
        return rabbitQueue;
    }

    public Boolean getRabbitMandatory() {
        return rabbitMandatory;
    }

    @Override
    public String toString() {
        return "Callback{" +
                "type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
