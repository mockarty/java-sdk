// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire envelope for {@code POST /api/v1/api-tester/flow-runs}. Mirrors
 * the Go SDK's {@code FlowRunRequest} and the admin handler's
 * {@code flowRunRequest} 1:1.
 *
 * <p>{@code flow} is left as an opaque {@code Object} so callers can
 * pass any Jackson-serialisable shape — a {@code Map<String,Object>},
 * a generated POJO, or a pre-built IR struct.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowRunRequest {

    @JsonProperty("flow")
    private Object flow;

    @JsonProperty("base_url")
    private String baseURL;

    public Object getFlow() { return flow; }
    public String getBaseURL() { return baseURL; }

    public void setFlow(Object flow) { this.flow = flow; }
    public void setBaseURL(String baseURL) { this.baseURL = baseURL; }

    public FlowRunRequest flow(Object f) { this.flow = f; return this; }
    public FlowRunRequest baseURL(String b) { this.baseURL = b; return this; }
}
