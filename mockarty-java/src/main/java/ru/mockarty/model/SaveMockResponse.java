// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned when creating or updating a mock.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaveMockResponse {

    @JsonProperty("overwritten")
    private boolean overwritten;

    @JsonProperty("mock")
    private Mock mock;

    public SaveMockResponse() {
    }

    public boolean isOverwritten() {
        return overwritten;
    }

    public void setOverwritten(boolean overwritten) {
        this.overwritten = overwritten;
    }

    public Mock getMock() {
        return mock;
    }

    public void setMock(Mock mock) {
        this.mock = mock;
    }

    @Override
    public String toString() {
        return "SaveMockResponse{" +
                "overwritten=" + overwritten +
                ", mock=" + mock +
                '}';
    }
}
