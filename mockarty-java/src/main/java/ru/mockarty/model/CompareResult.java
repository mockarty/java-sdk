// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff envelope returned by
 * {@code GET /api/v1/test-plans/runs/compare?run_a=&run_b=}.
 *
 * <p>Both runs MUST live in the caller's namespace (the server returns 404 on
 * cross-tenant probes). Comparing runs of different plans IS allowed; the
 * {@link CompareSummary#isDifferentPlans()} flag surfaces the case so callers
 * can show a banner. Pass the older/baseline run as {@code run_a} and the
 * newer/target run as {@code run_b} to keep regression / improvement signs
 * intuitive.</p>
 *
 * <p>Phase-4 task #82.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompareResult {

    @JsonProperty("runA")
    private CompareRunSide runA;

    @JsonProperty("runB")
    private CompareRunSide runB;

    @JsonProperty("items")
    private List<CompareItem> items = new ArrayList<>();

    @JsonProperty("summary")
    private CompareSummary summary;

    public CompareResult() {
    }

    public CompareRunSide getRunA() { return runA; }
    public void setRunA(CompareRunSide runA) { this.runA = runA; }

    public CompareRunSide getRunB() { return runB; }
    public void setRunB(CompareRunSide runB) { this.runB = runB; }

    public List<CompareItem> getItems() { return items; }
    public void setItems(List<CompareItem> items) { this.items = items; }

    public CompareSummary getSummary() { return summary; }
    public void setSummary(CompareSummary summary) { this.summary = summary; }

    // -----------------------------------------------------------------------
    // Nested types (one public class per file rule — these stay package-local
    // by being static nested types of CompareResult).
    // -----------------------------------------------------------------------

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompareRunSide {
        @JsonProperty("startedAt")     private String startedAt;
        @JsonProperty("completedAt")   private String completedAt;
        @JsonProperty("planName")      private String planName;
        @JsonProperty("status")        private String status;
        @JsonProperty("namespace")     private String namespace;
        @JsonProperty("id")            private String id;
        @JsonProperty("planId")        private String planId;
        @JsonProperty("planNumericId") private long planNumericId;
        @JsonProperty("totalItems")    private int totalItems;
        @JsonProperty("completedItems") private int completedItems;
        @JsonProperty("failedItems")   private int failedItems;
        @JsonProperty("passedItems")   private int passedItems;
        @JsonProperty("skippedItems")  private int skippedItems;
        @JsonProperty("durationMs")    private long durationMs;

        public String getStartedAt() { return startedAt; }
        public String getCompletedAt() { return completedAt; }
        public String getPlanName() { return planName; }
        public String getStatus() { return status; }
        public String getNamespace() { return namespace; }
        public String getId() { return id; }
        public String getPlanId() { return planId; }
        public long getPlanNumericId() { return planNumericId; }
        public int getTotalItems() { return totalItems; }
        public int getCompletedItems() { return completedItems; }
        public int getFailedItems() { return failedItems; }
        public int getPassedItems() { return passedItems; }
        public int getSkippedItems() { return skippedItems; }
        public long getDurationMs() { return durationMs; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompareItemSide {
        @JsonProperty("startedAt")   private String startedAt;
        @JsonProperty("completedAt") private String completedAt;
        @JsonProperty("status")      private String status;
        @JsonProperty("skipReason")  private String skipReason;
        @JsonProperty("error")       private String error;
        @JsonProperty("durationMs")  private long durationMs;
        @JsonProperty("attempts")    private int attempts;
        @JsonProperty("present")     private boolean present;

        public String getStartedAt() { return startedAt; }
        public String getCompletedAt() { return completedAt; }
        public String getStatus() { return status; }
        public String getSkipReason() { return skipReason; }
        public String getError() { return error; }
        public long getDurationMs() { return durationMs; }
        public int getAttempts() { return attempts; }
        public boolean isPresent() { return present; }
    }

    /**
     * Per-item diff classifier. {@code regressionType} is one of
     * {@code unchanged}, {@code pass_to_fail}, {@code fail_to_pass},
     * {@code skipped_to_ran}, {@code ran_to_skipped}, {@code fail_to_fail},
     * {@code pass_to_pass}, {@code added}, {@code removed}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompareItemDiff {
        @JsonProperty("regressionType")   private String regressionType;
        @JsonProperty("durationDeltaMs")  private long durationDeltaMs;
        @JsonProperty("statusChanged")    private boolean statusChanged;
        @JsonProperty("isRegression")     private boolean isRegression;
        @JsonProperty("isImprovement")    private boolean isImprovement;
        @JsonProperty("durationWorsened") private boolean durationWorsened;

        public String getRegressionType() { return regressionType; }
        public long getDurationDeltaMs() { return durationDeltaMs; }
        public boolean isStatusChanged() { return statusChanged; }
        public boolean isRegression() { return isRegression; }
        public boolean isImprovement() { return isImprovement; }
        public boolean isDurationWorsened() { return durationWorsened; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompareItem {
        @JsonProperty("a")       private CompareItemSide a;
        @JsonProperty("b")       private CompareItemSide b;
        @JsonProperty("diff")    private CompareItemDiff diff;
        @JsonProperty("type")    private String type;
        @JsonProperty("name")    private String name;
        @JsonProperty("itemUid") private String itemUid;

        public CompareItemSide getA() { return a; }
        public CompareItemSide getB() { return b; }
        public CompareItemDiff getDiff() { return diff; }
        public String getType() { return type; }
        public String getName() { return name; }
        public String getItemUid() { return itemUid; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompareItemRef {
        @JsonProperty("type")    private String type;
        @JsonProperty("name")    private String name;
        @JsonProperty("itemUid") private String itemUid;

        public String getType() { return type; }
        public String getName() { return name; }
        public String getItemUid() { return itemUid; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompareSummary {
        @JsonProperty("addedItems")     private List<CompareItemRef> addedItems = new ArrayList<>();
        @JsonProperty("removedItems")   private List<CompareItemRef> removedItems = new ArrayList<>();
        @JsonProperty("totalA")         private int totalA;
        @JsonProperty("totalB")         private int totalB;
        @JsonProperty("passToFail")     private int passToFail;
        @JsonProperty("failToPass")     private int failToPass;
        @JsonProperty("skippedToRan")   private int skippedToRan;
        @JsonProperty("ranToSkipped")   private int ranToSkipped;
        @JsonProperty("regressions")    private int regressions;
        @JsonProperty("improvements")   private int improvements;
        @JsonProperty("unchangedItems") private int unchangedItems;
        @JsonProperty("differentPlans") private boolean differentPlans;

        public List<CompareItemRef> getAddedItems() { return addedItems; }
        public List<CompareItemRef> getRemovedItems() { return removedItems; }
        public int getTotalA() { return totalA; }
        public int getTotalB() { return totalB; }
        public int getPassToFail() { return passToFail; }
        public int getFailToPass() { return failToPass; }
        public int getSkippedToRan() { return skippedToRan; }
        public int getRanToSkipped() { return ranToSkipped; }
        public int getRegressions() { return regressions; }
        public int getImprovements() { return improvements; }
        public int getUnchangedItems() { return unchangedItems; }
        public boolean isDifferentPlans() { return differentPlans; }
    }
}
