// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LLMSecurityPolicy {
    private Map<String, String> surfaceActions;
    private List<String> ruleIds;
    private List<String> blockedCapabilities;
    private String mode;
    private Boolean enabled;
    private Boolean failClosed;
    private long maxInputBytes;
    private long maxOutputBytes;
    private long maxDecodedBytes;
    private int blockThreshold;
    private int redactThreshold;
    private int maxFindings;
    private int maxDecodeCandidates;
    private int maxDecodeDepth;

    public Map<String, String> getSurfaceActions() { return surfaceActions == null ? Collections.emptyMap() : surfaceActions; }
    public List<String> getRuleIds() { return ruleIds == null ? Collections.emptyList() : ruleIds; }
    public List<String> getBlockedCapabilities() { return blockedCapabilities == null ? Collections.emptyList() : blockedCapabilities; }
    public String getMode() { return mode; }
    public Boolean getEnabled() { return enabled; }
    public Boolean getFailClosed() { return failClosed; }
    public long getMaxInputBytes() { return maxInputBytes; }
    public long getMaxOutputBytes() { return maxOutputBytes; }
    public long getMaxDecodedBytes() { return maxDecodedBytes; }
    public int getBlockThreshold() { return blockThreshold; }
    public int getRedactThreshold() { return redactThreshold; }
    public int getMaxFindings() { return maxFindings; }
    public int getMaxDecodeCandidates() { return maxDecodeCandidates; }
    public int getMaxDecodeDepth() { return maxDecodeDepth; }

    public LLMSecurityPolicy surfaceActions(Map<String, String> value) { surfaceActions = value; return this; }
    public LLMSecurityPolicy ruleIds(List<String> value) { ruleIds = value; return this; }
    public LLMSecurityPolicy blockedCapabilities(List<String> value) { blockedCapabilities = value; return this; }
    public LLMSecurityPolicy mode(String value) { mode = value; return this; }
    public LLMSecurityPolicy enabled(boolean value) { enabled = value; return this; }
    public LLMSecurityPolicy failClosed(boolean value) { failClosed = value; return this; }
    public LLMSecurityPolicy maxInputBytes(long value) { maxInputBytes = value; return this; }
    public LLMSecurityPolicy maxOutputBytes(long value) { maxOutputBytes = value; return this; }
    public LLMSecurityPolicy maxDecodedBytes(long value) { maxDecodedBytes = value; return this; }
    public LLMSecurityPolicy blockThreshold(int value) { blockThreshold = value; return this; }
    public LLMSecurityPolicy redactThreshold(int value) { redactThreshold = value; return this; }
    public LLMSecurityPolicy maxFindings(int value) { maxFindings = value; return this; }
    public LLMSecurityPolicy maxDecodeCandidates(int value) { maxDecodeCandidates = value; return this; }
    public LLMSecurityPolicy maxDecodeDepth(int value) { maxDecodeDepth = value; return this; }
}
