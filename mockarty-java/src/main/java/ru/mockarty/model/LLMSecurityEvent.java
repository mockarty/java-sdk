// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

/** Metadata-only prompt-security decision. No prompt or matched text is exposed. */
public class LLMSecurityEvent {
    private String createdAt;
    private String mode;
    private String source;
    private String namespace;
    private String ruleId;
    private String profileId;
    private String category;
    private String decision;
    private String surface;
	private String trustClass;
	private String fingerprint;
	private String correlationId;
    private long id;
    private long latencyUs;
    private long policyRevision;
    private int matches;
    private int score;
    private boolean truncated;

    public String getCreatedAt() { return createdAt; }
    public String getMode() { return mode; }
    public String getSource() { return source; }
    public String getNamespace() { return namespace; }
    public String getRuleId() { return ruleId; }
    public String getProfileId() { return profileId; }
    public String getCategory() { return category; }
    public String getDecision() { return decision; }
    public String getSurface() { return surface; }
    public String getTrustClass() { return trustClass; }
	public String getFingerprint() { return fingerprint; }
	public String getCorrelationId() { return correlationId; }
    public long getId() { return id; }
    public long getLatencyUs() { return latencyUs; }
    public long getPolicyRevision() { return policyRevision; }
    public int getMatches() { return matches; }
    public int getScore() { return score; }
    public boolean isTruncated() { return truncated; }
}
