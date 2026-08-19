// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMUsageForecast {
    private long observedMicros;
    private long dailyRunRateMicros;
    private long projected30DayMicros;
    private long recent24HoursMicros;
    private long priorDailyMicros;
    private double recentToBaselineRatio;
    private String currency;
    private String status;
    public long getObservedMicros() { return observedMicros; }
    public long getDailyRunRateMicros() { return dailyRunRateMicros; }
    public long getProjected30DayMicros() { return projected30DayMicros; }
    public long getRecent24HoursMicros() { return recent24HoursMicros; }
    public long getPriorDailyMicros() { return priorDailyMicros; }
    public double getRecentToBaselineRatio() { return recentToBaselineRatio; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
}
