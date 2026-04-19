// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single soft-deleted entity row returned by the Recycle Bin list endpoints.
 *
 * <p>Mirrors the {@code trashListItemView} JSON projection in
 * {@code internal/webui/integration_routes_trash_list.go}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrashItem {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("entity_type")
    private String entityType;

    @JsonProperty("closed_at")
    private String closedAt;

    @JsonProperty("closed_by")
    private String closedBy;

    @JsonProperty("closed_reason")
    private String closedReason;

    @JsonProperty("cascade_group_id")
    private String cascadeGroupId;

    @JsonProperty("numeric_id")
    private Long numericId;

    @JsonProperty("restore_available")
    private boolean restoreAvailable;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getNamespace() { return namespace; }
    public String getEntityType() { return entityType; }
    public String getClosedAt() { return closedAt; }
    public String getClosedBy() { return closedBy; }
    public String getClosedReason() { return closedReason; }
    public String getCascadeGroupId() { return cascadeGroupId; }
    public Long getNumericId() { return numericId; }
    public boolean isRestoreAvailable() { return restoreAvailable; }
}
