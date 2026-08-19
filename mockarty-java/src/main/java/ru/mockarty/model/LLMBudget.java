// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMBudget {
    private long includedMicros;
    private long prepaidMicros;
    private long softLimitMicros;
    private long hardLimitMicros;
    private long spentMicros;
    private long reservedMicros;
    private String createdAt;
    private String updatedAt;
    private String periodStart;
    private String periodEnd;
    private String id;
    private String namespace;
    private String scopeType;
    private String scopeId;
    private String currency;
    private boolean overageAllowed;
    private boolean requirePriced = true;
    private boolean enabled = true;

    public long getIncludedMicros() { return includedMicros; }
    public long getPrepaidMicros() { return prepaidMicros; }
    public long getSoftLimitMicros() { return softLimitMicros; }
    public long getHardLimitMicros() { return hardLimitMicros; }
    public long getSpentMicros() { return spentMicros; }
    public long getReservedMicros() { return reservedMicros; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getPeriodStart() { return periodStart; }
    public String getPeriodEnd() { return periodEnd; }
    public String getId() { return id; }
    public String getNamespace() { return namespace; }
    public String getScopeType() { return scopeType; }
    public String getScopeId() { return scopeId; }
    public String getCurrency() { return currency; }
    public boolean isOverageAllowed() { return overageAllowed; }
    public boolean isRequirePriced() { return requirePriced; }
    public boolean isEnabled() { return enabled; }
    public LLMBudget includedMicros(long value) { includedMicros = value; return this; }
    public LLMBudget prepaidMicros(long value) { prepaidMicros = value; return this; }
    public LLMBudget softLimitMicros(long value) { softLimitMicros = value; return this; }
    public LLMBudget hardLimitMicros(long value) { hardLimitMicros = value; return this; }
    public LLMBudget periodStart(String value) { periodStart = value; return this; }
    public LLMBudget periodEnd(String value) { periodEnd = value; return this; }
    public LLMBudget id(String value) { id = value; return this; }
    public LLMBudget namespace(String value) { namespace = value; return this; }
    public LLMBudget scopeType(String value) { scopeType = value; return this; }
    public LLMBudget scopeId(String value) { scopeId = value; return this; }
    public LLMBudget currency(String value) { currency = value; return this; }
    public LLMBudget overageAllowed(boolean value) { overageAllowed = value; return this; }
    public LLMBudget requirePriced(boolean value) { requirePriced = value; return this; }
    public LLMBudget enabled(boolean value) { enabled = value; return this; }
}
