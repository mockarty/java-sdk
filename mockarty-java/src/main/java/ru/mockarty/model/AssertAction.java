// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Assert actions for mock condition matching.
 * Maps to the server-side AssertAction type.
 */
public enum AssertAction {

    @JsonProperty("equals")
    EQUALS,

    @JsonProperty("contains")
    CONTAINS,

    @JsonProperty("not_equals")
    NOT_EQUALS,

    @JsonProperty("not_contains")
    NOT_CONTAINS,

    @JsonProperty("any")
    ANY,

    @JsonProperty("notEmpty")
    NOT_EMPTY,

    @JsonProperty("empty")
    EMPTY,

    @JsonProperty("matches")
    MATCHES
}
