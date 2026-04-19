// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 202 Accepted envelope returned by
 * {@code POST /api/v1/namespaces/:namespace/test-runs/ad-hoc}.
 *
 * <p>The {@code _links} block carries canonical follow-up URLs (self,
 * status, report). Treat them as hints rather than a stable contract and
 * prefer the {@code runId} for subsequent polling via
 * {@code TestPlanApi.getRun}, {@code waitForRun}, or {@code streamRun}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdHocRunResponse {

    @JsonProperty("run_id")
    private String runId;

    @JsonProperty("plan_id")
    private String planId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("adhoc")
    private Boolean adhoc;

    @JsonProperty("_links")
    private Map<String, String> links;

    public AdHocRunResponse() {
    }

    public String getRunId() {
        return runId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getAdhoc() {
        return adhoc;
    }

    public Map<String, String> getLinks() {
        return links;
    }
}
