// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMPrice {
    private long inputMicrosPerMillion;
    private long outputMicrosPerMillion;
    private long cacheReadMicrosPerMillion;
    private long cacheWriteMicrosPerMillion;
    private String createdAt;
    private String effectiveFrom;
    private String id;
    private String provider;
    private String model;
    private String currency;
    private String source;

    public long getInputMicrosPerMillion() { return inputMicrosPerMillion; }
    public long getOutputMicrosPerMillion() { return outputMicrosPerMillion; }
    public long getCacheReadMicrosPerMillion() { return cacheReadMicrosPerMillion; }
    public long getCacheWriteMicrosPerMillion() { return cacheWriteMicrosPerMillion; }
    public String getCreatedAt() { return createdAt; }
    public String getEffectiveFrom() { return effectiveFrom; }
    public String getId() { return id; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getCurrency() { return currency; }
    public String getSource() { return source; }
    public LLMPrice inputMicrosPerMillion(long value) { inputMicrosPerMillion = value; return this; }
    public LLMPrice outputMicrosPerMillion(long value) { outputMicrosPerMillion = value; return this; }
    public LLMPrice cacheReadMicrosPerMillion(long value) { cacheReadMicrosPerMillion = value; return this; }
    public LLMPrice cacheWriteMicrosPerMillion(long value) { cacheWriteMicrosPerMillion = value; return this; }
    public LLMPrice effectiveFrom(String value) { effectiveFrom = value; return this; }
    public LLMPrice provider(String value) { provider = value; return this; }
    public LLMPrice model(String value) { model = value; return this; }
    public LLMPrice currency(String value) { currency = value; return this; }
}
