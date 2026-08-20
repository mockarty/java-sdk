// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LLMSecurityPolicyDocument {
    private LLMSecurityPolicy value;
    private Map<String, List<String>> additions;
    private Map<String, List<String>> denies;
    private Map<String, List<String>> allows;
    private Map<String, Double> caps;
    private List<Map<String, Object>> delegations;

    public LLMSecurityPolicy getValue() { return value; }
    public Map<String, List<String>> getAdditions() { return additions == null ? Collections.emptyMap() : additions; }
    public Map<String, List<String>> getDenies() { return denies == null ? Collections.emptyMap() : denies; }
    public Map<String, List<String>> getAllows() { return allows == null ? Collections.emptyMap() : allows; }
    public Map<String, Double> getCaps() { return caps == null ? Collections.emptyMap() : caps; }
    public List<Map<String, Object>> getDelegations() { return delegations == null ? Collections.emptyList() : delegations; }

    public LLMSecurityPolicyDocument value(LLMSecurityPolicy policy) { value = policy; return this; }
    public LLMSecurityPolicyDocument additions(Map<String, List<String>> entries) { additions = entries; return this; }
    public LLMSecurityPolicyDocument denies(Map<String, List<String>> entries) { denies = entries; return this; }
    public LLMSecurityPolicyDocument allows(Map<String, List<String>> entries) { allows = entries; return this; }
    public LLMSecurityPolicyDocument caps(Map<String, Double> entries) { caps = entries; return this; }
    public LLMSecurityPolicyDocument delegations(List<Map<String, Object>> entries) { delegations = entries; return this; }
}
