// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class ResourcePrice {
    private long providerMicrosPerUnit;
    private long customerMicrosPerUnit;
    private String createdAt;
    private String effectiveFrom;
    private String id;
    private String eventKind;
    private String provider;
    private String resource;
    private String unit;
    private String currency;
    private String source;

    public long getProviderMicrosPerUnit() { return providerMicrosPerUnit; }
    public long getCustomerMicrosPerUnit() { return customerMicrosPerUnit; }
    public String getCreatedAt() { return createdAt; }
    public String getEffectiveFrom() { return effectiveFrom; }
    public String getId() { return id; }
    public String getEventKind() { return eventKind; }
    public String getProvider() { return provider; }
    public String getResource() { return resource; }
    public String getUnit() { return unit; }
    public String getCurrency() { return currency; }
    public String getSource() { return source; }
    public ResourcePrice providerMicrosPerUnit(long value) { providerMicrosPerUnit = value; return this; }
    public ResourcePrice customerMicrosPerUnit(long value) { customerMicrosPerUnit = value; return this; }
    public ResourcePrice effectiveFrom(String value) { effectiveFrom = value; return this; }
    public ResourcePrice eventKind(String value) { eventKind = value; return this; }
    public ResourcePrice provider(String value) { provider = value; return this; }
    public ResourcePrice resource(String value) { resource = value; return this; }
    public ResourcePrice unit(String value) { unit = value; return this; }
    public ResourcePrice currency(String value) { currency = value; return this; }
}
