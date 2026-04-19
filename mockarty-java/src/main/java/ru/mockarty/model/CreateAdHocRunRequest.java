// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for
 * {@code POST /api/v1/namespaces/:namespace/test-runs/ad-hoc}.
 *
 * <p>An ad-hoc run creates a hidden "ad-hoc" Plan row under the hood and
 * dispatches a master run through the orchestrator in a single call —
 * perfect for CI pipelines that want to bundle arbitrary resources without
 * authoring a persisted plan.</p>
 *
 * <p>{@code schedule} follows the same vocabulary as {@link TestPlan}:
 * empty = FIFO, {@code parallel}, {@code dag}, or a cron expression.</p>
 *
 * <p>{@code namespace} is the URL segment; it is not serialised into the
 * JSON body.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAdHocRunRequest {

    @JsonIgnore
    private String namespace;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("schedule")
    private String schedule;

    @JsonProperty("items")
    private List<AdHocItem> items;

    public CreateAdHocRunRequest() {
    }

    public CreateAdHocRunRequest namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CreateAdHocRunRequest name(String name) {
        this.name = name;
        return this;
    }

    public CreateAdHocRunRequest description(String description) {
        this.description = description;
        return this;
    }

    public CreateAdHocRunRequest schedule(String schedule) {
        this.schedule = schedule;
        return this;
    }

    public CreateAdHocRunRequest items(List<AdHocItem> items) {
        this.items = items;
        return this;
    }

    @JsonIgnore
    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSchedule() {
        return schedule;
    }

    public List<AdHocItem> getItems() {
        return items;
    }
}
