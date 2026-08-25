// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Map;

public class ExperienceRecordRequest {
    private Map<String, String> metadata;
    private String kind;
    private String title;
    private String text;
    private String source;
    private String missionId;
    private long eventSeq;

    public Map<String, String> getMetadata() { return metadata; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getSource() { return source; }
    public String getMissionId() { return missionId; }
    public long getEventSeq() { return eventSeq; }
    public ExperienceRecordRequest metadata(Map<String, String> value) { metadata = value; return this; }
    public ExperienceRecordRequest kind(String value) { kind = value; return this; }
    public ExperienceRecordRequest title(String value) { title = value; return this; }
    public ExperienceRecordRequest text(String value) { text = value; return this; }
    public ExperienceRecordRequest source(String value) { source = value; return this; }
    public ExperienceRecordRequest missionId(String value) { missionId = value; return this; }
    public ExperienceRecordRequest eventSeq(long value) { eventSeq = value; return this; }
}
