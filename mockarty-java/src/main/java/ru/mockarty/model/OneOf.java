// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OneOf defines multiple possible responses for a mock.
 * Responses are returned either in order or randomly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OneOf {

    @JsonProperty("order")
    private String order;

    @JsonProperty("offset")
    private Integer offsetOrder;

    @JsonProperty("responses")
    private List<ContentResponse> responses;

    public OneOf() {
    }

    /**
     * Creates a OneOf with ordered responses.
     */
    public static OneOf ordered(ContentResponse... responses) {
        OneOf oneOf = new OneOf();
        oneOf.order = "order";
        oneOf.responses = new ArrayList<>(Arrays.asList(responses));
        return oneOf;
    }

    /**
     * Creates a OneOf with random responses.
     */
    public static OneOf random(ContentResponse... responses) {
        OneOf oneOf = new OneOf();
        oneOf.order = "random";
        oneOf.responses = new ArrayList<>(Arrays.asList(responses));
        return oneOf;
    }

    // Builder-style setters

    public OneOf order(String order) {
        this.order = order;
        return this;
    }

    public OneOf offsetOrder(int offsetOrder) {
        this.offsetOrder = offsetOrder;
        return this;
    }

    public OneOf responses(List<ContentResponse> responses) {
        this.responses = responses;
        return this;
    }

    public OneOf addResponse(ContentResponse response) {
        if (this.responses == null) {
            this.responses = new ArrayList<>();
        }
        this.responses.add(response);
        return this;
    }

    // Getters

    public String getOrder() {
        return order;
    }

    public Integer getOffsetOrder() {
        return offsetOrder;
    }

    public List<ContentResponse> getResponses() {
        return responses;
    }

    @Override
    public String toString() {
        return "OneOf{" +
                "order='" + order + '\'' +
                ", responses=" + (responses != null ? responses.size() : 0) +
                '}';
    }
}
