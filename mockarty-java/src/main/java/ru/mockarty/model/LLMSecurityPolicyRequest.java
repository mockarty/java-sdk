// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMSecurityPolicyRequest {
    private LLMSecurityPolicyDocument document;
    private String mode = "merge";
    private Boolean active;
    private long expectedRevision;

    public LLMSecurityPolicyDocument getDocument() { return document; }
    public String getMode() { return mode; }
    public Boolean getActive() { return active; }
    public long getExpectedRevision() { return expectedRevision; }
    public LLMSecurityPolicyRequest document(LLMSecurityPolicyDocument value) { document = value; return this; }
    public LLMSecurityPolicyRequest mode(String value) { mode = value; return this; }
    public LLMSecurityPolicyRequest active(boolean value) { active = value; return this; }
    public LLMSecurityPolicyRequest expectedRevision(long value) { expectedRevision = value; return this; }
}
