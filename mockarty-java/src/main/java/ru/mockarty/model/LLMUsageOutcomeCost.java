// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMUsageOutcomeCost {
    private long providerCostMicros;
    private long customerCostMicros;
    private long calls;
    private long resourceEvents;
    private long resourceQuantity;
    private String outcome;
    private String currency;
    public long getProviderCostMicros() { return providerCostMicros; }
    public long getCustomerCostMicros() { return customerCostMicros; }
    public long getCalls() { return calls; }
    public long getResourceEvents() { return resourceEvents; }
    public long getResourceQuantity() { return resourceQuantity; }
    public String getOutcome() { return outcome; }
    public String getCurrency() { return currency; }
}
