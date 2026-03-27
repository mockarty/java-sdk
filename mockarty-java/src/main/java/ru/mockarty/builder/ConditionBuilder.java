// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.builder;

import ru.mockarty.model.AssertAction;
import ru.mockarty.model.Condition;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating lists of Conditions.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * List<Condition> conditions = ConditionBuilder.create()
 *     .equals("$.userId", "123")
 *     .contains("$.name", "John")
 *     .notEmpty("$.email")
 *     .matches("$.phone", "\\+\\d{10,}")
 *     .build();
 * }</pre>
 */
public class ConditionBuilder {

    private final List<Condition> conditions = new ArrayList<>();

    private ConditionBuilder() {
    }

    /**
     * Creates a new ConditionBuilder.
     */
    public static ConditionBuilder create() {
        return new ConditionBuilder();
    }

    /**
     * Adds a condition with the specified path, action, and value.
     */
    public ConditionBuilder add(String path, AssertAction action, Object value) {
        conditions.add(Condition.of(path, action, value));
        return this;
    }

    /**
     * Adds an equals condition.
     */
    public ConditionBuilder equals(String path, Object value) {
        conditions.add(Condition.equals(path, value));
        return this;
    }

    /**
     * Adds a contains condition.
     */
    public ConditionBuilder contains(String path, Object value) {
        conditions.add(Condition.contains(path, value));
        return this;
    }

    /**
     * Adds a not-equals condition.
     */
    public ConditionBuilder notEquals(String path, Object value) {
        conditions.add(Condition.of(path, AssertAction.NOT_EQUALS, value));
        return this;
    }

    /**
     * Adds a not-contains condition.
     */
    public ConditionBuilder notContains(String path, Object value) {
        conditions.add(Condition.of(path, AssertAction.NOT_CONTAINS, value));
        return this;
    }

    /**
     * Adds a not-empty condition.
     */
    public ConditionBuilder notEmpty(String path) {
        conditions.add(Condition.notEmpty(path));
        return this;
    }

    /**
     * Adds an empty condition.
     */
    public ConditionBuilder empty(String path) {
        conditions.add(Condition.of(path, AssertAction.EMPTY, null));
        return this;
    }

    /**
     * Adds a regex matches condition.
     */
    public ConditionBuilder matches(String path, String regex) {
        conditions.add(Condition.matches(path, regex));
        return this;
    }

    /**
     * Adds an "any" condition (matches any value).
     */
    public ConditionBuilder any(String path) {
        conditions.add(Condition.of(path, AssertAction.ANY, null));
        return this;
    }

    /**
     * Adds a pre-built condition.
     */
    public ConditionBuilder add(Condition condition) {
        conditions.add(condition);
        return this;
    }

    /**
     * Builds and returns the list of conditions.
     */
    public List<Condition> build() {
        return new ArrayList<>(conditions);
    }
}
