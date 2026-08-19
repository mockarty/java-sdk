// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMUsageTotals {
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private long calls;
    public long getInputTokens() { return inputTokens; }
    public long getOutputTokens() { return outputTokens; }
    public long getTotalTokens() { return totalTokens; }
    public long getCalls() { return calls; }
}
