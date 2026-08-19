// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.Map;

public class ExperienceItem {
    private Map<String, String> metadata;
    private String id;
    private String kind;
    private String title;
    private String text;
    private String source;
    private String provenance;
    private String missionId;
    private long eventSeq;
    private double score;

    public Map<String, String> getMetadata() { return metadata == null ? Collections.emptyMap() : metadata; }
    public String getId() { return id; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getSource() { return source; }
    public String getProvenance() { return provenance; }
    public String getMissionId() { return missionId; }
    public long getEventSeq() { return eventSeq; }
    public double getScore() { return score; }
}
