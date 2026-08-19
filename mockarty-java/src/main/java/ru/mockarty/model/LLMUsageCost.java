// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMUsageCost {
    private long providerCostMicros;
    private long platformFeeMicros;
    private long markupMicros;
    private long taxMicros;
    private long discountMicros;
    private long customerCostMicros;
    private long marginMicros;
    private long includedMicros;
    private long prepaidMicros;
    private long overageMicros;
    private long calls;
    private long byokCalls;
    private String currency;
    public long getProviderCostMicros() { return providerCostMicros; }
    public long getPlatformFeeMicros() { return platformFeeMicros; }
    public long getMarkupMicros() { return markupMicros; }
    public long getTaxMicros() { return taxMicros; }
    public long getDiscountMicros() { return discountMicros; }
    public long getCustomerCostMicros() { return customerCostMicros; }
    public long getMarginMicros() { return marginMicros; }
    public long getIncludedMicros() { return includedMicros; }
    public long getPrepaidMicros() { return prepaidMicros; }
    public long getOverageMicros() { return overageMicros; }
    public long getCalls() { return calls; }
    public long getByokCalls() { return byokCalls; }
    public String getCurrency() { return currency; }
}
