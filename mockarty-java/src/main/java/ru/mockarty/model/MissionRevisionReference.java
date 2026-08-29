// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

/** Exact target or artifact revision sealed into a mission brief. */
public class MissionRevisionReference {
    private String kind;
    private String id;
    private String digest;
    private long revision;

    public MissionRevisionReference kind(String value) { this.kind = value; return this; }
    public MissionRevisionReference id(String value) { this.id = value; return this; }
    public MissionRevisionReference digest(String value) { this.digest = value; return this; }
    public MissionRevisionReference revision(long value) { this.revision = value; return this; }

    public String getKind() { return kind; }
    public String getId() { return id; }
    public String getDigest() { return digest; }
    public long getRevision() { return revision; }
}
