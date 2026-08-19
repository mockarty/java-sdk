// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class LLMUsageReport {
    private LLMUsageTotals totals;
    private List<LLMUsageGroup> rows;
    private List<LLMUsageCost> costs;
    private List<LLMUsageForecast> forecast;
    private List<LLMUsageOutcomeCost> outcomeCosts;
    private LLMUsageReconciliation reconciliation;
    private long unpricedCalls;
    public LLMUsageTotals getTotals() { return totals; }
    public List<LLMUsageGroup> getRows() { return rows == null ? Collections.emptyList() : rows; }
    public List<LLMUsageCost> getCosts() { return costs == null ? Collections.emptyList() : costs; }
    public List<LLMUsageForecast> getForecast() { return forecast == null ? Collections.emptyList() : forecast; }
    public List<LLMUsageOutcomeCost> getOutcomeCosts() { return outcomeCosts == null ? Collections.emptyList() : outcomeCosts; }
    public LLMUsageReconciliation getReconciliation() { return reconciliation; }
    public long getUnpricedCalls() { return unpricedCalls; }
}
