// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

public class ExperienceReviewRelation {
    private String recordId;
    private String type;
    private boolean outgoing;

    public String getRecordId() { return recordId; }
    public String getType() { return type; }
    public boolean isOutgoing() { return outgoing; }
}
