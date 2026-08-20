// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LLMSecurityPolicyResponse {
    private LLMSecurityPolicy effective;
    private LLMSecurityPolicyDocument document;
    private Map<String, Object> restrictions;
    private List<Map<String, Object>> applied;
    private String mode;
    private String layer;
    private String namespace;
    private long revision;
    private boolean active;
    private boolean local;
    private boolean deliveryDeferred;

    public LLMSecurityPolicy getEffective() { return effective; }
    public LLMSecurityPolicyDocument getDocument() { return document; }
    public Map<String, Object> getRestrictions() { return restrictions == null ? Collections.emptyMap() : restrictions; }
    public List<Map<String, Object>> getApplied() { return applied == null ? Collections.emptyList() : applied; }
    public String getMode() { return mode; }
    public String getLayer() { return layer; }
    public String getNamespace() { return namespace; }
    public long getRevision() { return revision; }
    public boolean isActive() { return active; }
    public boolean isLocal() { return local; }
    public boolean isDeliveryDeferred() { return deliveryDeferred; }
}
