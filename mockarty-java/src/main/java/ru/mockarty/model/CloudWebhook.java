// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** A workspace-scoped outbound webhook in Mockarty Cloud. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudWebhook {
    private String id;

    @JsonProperty("workspace_id")
    private String workspaceId;

    private String name;
    private String url;
    private List<String> events;

    @JsonProperty("signing_status")
    private String signingStatus;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private boolean active;

    @JsonProperty("signing_ready")
    private boolean signingReady;

    public CloudWebhook() {
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public List<String> getEvents() {
        return events;
    }

    public String getSigningStatus() {
        return signingStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSigningReady() {
        return signingReady;
    }
}
