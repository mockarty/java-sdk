// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One entry of a mock's version history.
 *
 * <p>The server stores the mock body of every revision alongside the revision
 * metadata, so a version row is NOT a {@link Mock} — the mock itself hangs off
 * {@link #getMock()}. Decoding these rows straight into {@code Mock} produced
 * entries whose id was the version-row id and whose body was empty, with no
 * error to show for it.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MockVersion {

    @JsonProperty("id")
    private String id;

    @JsonProperty("mock_id")
    private String mockId;

    /** Revision number, as passed to {@code getVersion} / {@code restoreVersion}. */
    @JsonProperty("version")
    private int version;

    /** The mock body as it was at this revision. */
    @JsonProperty("mock")
    private Mock mock;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("lifecycle_state")
    private String lifecycleState;

    @JsonProperty("environment")
    private String environment;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_by_email")
    private String createdByEmail;

    @JsonProperty("modified_by")
    private String modifiedBy;

    @JsonProperty("modified_by_email")
    private String modifiedByEmail;

    @JsonProperty("closed_at")
    private Long closedAt;

    public MockVersion() {
    }

    public String getId() {
        return id;
    }

    public String getMockId() {
        return mockId;
    }

    public int getVersion() {
        return version;
    }

    public Mock getMock() {
        return mock;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    public String getEnvironment() {
        return environment;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public String getModifiedByEmail() {
        return modifiedByEmail;
    }

    public Long getClosedAt() {
        return closedAt;
    }

    @Override
    public String toString() {
        return "MockVersion{" +
                "mockId='" + mockId + '\'' +
                ", version=" + version +
                ", tags=" + tags +
                '}';
    }
}
