// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class ExperienceRecordResponse {
    private String id;
    private String kind;
    private String provenance;
	private String state;
	private boolean reviewRequired;

    public String getId() { return id; }
    public String getKind() { return kind; }
    public String getProvenance() { return provenance; }
	public String getState() { return state; }
	public boolean isReviewRequired() { return reviewRequired; }
}
