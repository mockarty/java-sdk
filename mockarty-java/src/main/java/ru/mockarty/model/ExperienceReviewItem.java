// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.Map;

public class ExperienceReviewItem {
    private String id;
    private String title;
    private String content;
	private String contentSha256;
	private Map<String, String> metadata;
    private String kind;
    private String source;
    private String missionId;
    private String provenance;
    private String state;
    private String createdAt;
    private String updatedAt;
    private String publishedAt;
    private String expiresAt;
    private long eventSeq;
    private long version;
    private double confidence;
    private boolean contentTruncated;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
	public String getContentSha256() { return contentSha256; }
	public Map<String, String> getMetadata() { return metadata == null ? Collections.emptyMap() : metadata; }
    public String getKind() { return kind; }
    public String getSource() { return source; }
    public String getMissionId() { return missionId; }
    public String getProvenance() { return provenance; }
    public String getState() { return state; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getPublishedAt() { return publishedAt; }
    public String getExpiresAt() { return expiresAt; }
    public long getEventSeq() { return eventSeq; }
    public long getVersion() { return version; }
    public double getConfidence() { return confidence; }
    public boolean isContentTruncated() { return contentTruncated; }
}
