// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class ExperienceReviewMutation {
    private String operation;
    private String actor;
    private String reason;
    private String createdAt;
    private long version;

    public String getOperation() { return operation; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public String getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
