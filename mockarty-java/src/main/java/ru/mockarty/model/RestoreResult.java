// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a single cascade-restore operation.
 *
 * <p>Mirrors {@code internal/model.RestoreResult} (camelCase wire format).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestoreResult {

    @JsonProperty("cascadeGroupId")
    private String cascadeGroupId;

    @JsonProperty("restoredCount")
    private int restoredCount;

    @JsonProperty("missingCount")
    private Integer missingCount;

    @JsonProperty("parentDeleted")
    private Boolean parentDeleted;

    public String getCascadeGroupId() { return cascadeGroupId; }
    public int getRestoredCount() { return restoredCount; }
    public Integer getMissingCount() { return missingCount; }
    public Boolean getParentDeleted() { return parentDeleted; }
}
