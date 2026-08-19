// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMUsageOutcomeCost {
    private long providerCostMicros;
    private long customerCostMicros;
    private long calls;
    private String outcome;
    private String currency;
    public long getProviderCostMicros() { return providerCostMicros; }
    public long getCustomerCostMicros() { return customerCostMicros; }
    public long getCalls() { return calls; }
    public String getOutcome() { return outcome; }
    public String getCurrency() { return currency; }
}
