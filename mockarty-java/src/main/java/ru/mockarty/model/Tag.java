// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a tag used to categorize mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tag {

    @JsonProperty("name")
    private String name;

    @JsonProperty("count")
    private Integer count;

    public Tag() {
    }

    // Builder-style setters

    public Tag name(String name) {
        this.name = name;
        return this;
    }

    public Tag count(Integer count) {
        this.count = count;
        return this;
    }

    // Getters

    public String getName() {
        return name;
    }

    public Integer getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", count=" + count +
                '}';
    }
}
