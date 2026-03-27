// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Condition for mock matching. Each condition evaluates a specific path
 * in the request against an expected value using the specified assert action.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Condition {

    @JsonProperty("path")
    private String path;

    @JsonProperty("assertAction")
    private AssertAction assertAction;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("valueFromFile")
    private String valueFromFile;

    @JsonProperty("sortArray")
    private Boolean applySortArray;

    @JsonProperty("decode")
    private String decode;

    public Condition() {
    }

    /**
     * Creates a new condition with the given path, action, and value.
     */
    public static Condition of(String path, AssertAction action, Object value) {
        return new Condition().path(path).assertAction(action).value(value);
    }

    /**
     * Creates a condition that checks if a path equals a value.
     */
    public static Condition equals(String path, Object value) {
        return of(path, AssertAction.EQUALS, value);
    }

    /**
     * Creates a condition that checks if a path contains a value.
     */
    public static Condition contains(String path, Object value) {
        return of(path, AssertAction.CONTAINS, value);
    }

    /**
     * Creates a condition that checks if a path matches a regex.
     */
    public static Condition matches(String path, String regex) {
        return of(path, AssertAction.MATCHES, regex);
    }

    /**
     * Creates a condition that checks if a path is not empty.
     */
    public static Condition notEmpty(String path) {
        return of(path, AssertAction.NOT_EMPTY, null);
    }

    // Builder-style setters

    public Condition path(String path) {
        this.path = path;
        return this;
    }

    public Condition assertAction(AssertAction action) {
        this.assertAction = action;
        return this;
    }

    public Condition value(Object value) {
        this.value = value;
        return this;
    }

    public Condition valueFromFile(String valueFromFile) {
        this.valueFromFile = valueFromFile;
        return this;
    }

    public Condition applySortArray(Boolean applySortArray) {
        this.applySortArray = applySortArray;
        return this;
    }

    public Condition decode(String decode) {
        this.decode = decode;
        return this;
    }

    // Getters

    public String getPath() {
        return path;
    }

    public AssertAction getAssertAction() {
        return assertAction;
    }

    public Object getValue() {
        return value;
    }

    public String getValueFromFile() {
        return valueFromFile;
    }

    public Boolean getApplySortArray() {
        return applySortArray;
    }

    public String getDecode() {
        return decode;
    }

    @Override
    public String toString() {
        return "Condition{" +
                "path='" + path + '\'' +
                ", assertAction=" + assertAction +
                ", value=" + value +
                '}';
    }
}
