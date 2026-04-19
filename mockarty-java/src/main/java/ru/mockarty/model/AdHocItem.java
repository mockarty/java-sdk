// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One step in a {@link CreateAdHocRunRequest}.
 *
 * <p>The server accepts both canonical short names ({@code functional} /
 * {@code load} / {@code fuzz} / {@code chaos} / {@code contract}) and the
 * spec-level aliases ({@code collection} / {@code perf_config} /
 * {@code fuzz_config} / {@code chaos_experiment} / {@code contract_config}).
 * Prefer the short names for new code.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdHocItem {

    @JsonProperty("ref_id")
    private String refId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("depends_on")
    private List<String> dependsOn;

    @JsonProperty("delay_after_ms")
    private Long delayAfterMs;

    public AdHocItem() {
    }

    public AdHocItem refId(String refId) {
        this.refId = refId;
        return this;
    }

    public AdHocItem type(String type) {
        this.type = type;
        return this;
    }

    public AdHocItem order(Integer order) {
        this.order = order;
        return this;
    }

    public AdHocItem dependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
        return this;
    }

    public AdHocItem delayAfterMs(Long delayAfterMs) {
        this.delayAfterMs = delayAfterMs;
        return this;
    }

    public String getRefId() {
        return refId;
    }

    public String getType() {
        return type;
    }

    public Integer getOrder() {
        return order;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public Long getDelayAfterMs() {
        return delayAfterMs;
    }
}
