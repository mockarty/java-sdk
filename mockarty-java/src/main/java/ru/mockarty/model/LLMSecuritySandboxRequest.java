// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMSecuritySandboxRequest {
    private LLMSecurityPolicyDocument document;
    private String text;
    private String mode;
    private String surface = "input";
    private String trustClass = "user";
    private Boolean active;
    private long expectedRevision;

    public LLMSecurityPolicyDocument getDocument() { return document; }
    public String getText() { return text; }
    public String getMode() { return mode; }
    public String getSurface() { return surface; }
    public String getTrustClass() { return trustClass; }
    public Boolean getActive() { return active; }
    public long getExpectedRevision() { return expectedRevision; }
    public LLMSecuritySandboxRequest document(LLMSecurityPolicyDocument value) { document = value; return this; }
    public LLMSecuritySandboxRequest text(String value) { text = value; return this; }
    public LLMSecuritySandboxRequest mode(String value) { mode = value; return this; }
    public LLMSecuritySandboxRequest surface(String value) { surface = value; return this; }
    public LLMSecuritySandboxRequest trustClass(String value) { trustClass = value; return this; }
    public LLMSecuritySandboxRequest active(boolean value) { active = value; return this; }
    public LLMSecuritySandboxRequest expectedRevision(long value) { expectedRevision = value; return this; }
}
