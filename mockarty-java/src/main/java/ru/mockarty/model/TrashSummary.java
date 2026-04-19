// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Per-entity-type aggregate counts from the Recycle Bin summary endpoints. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrashSummary {

    @JsonProperty("counts")
    private List<Count> counts;

    @JsonProperty("total")
    private long total;

    public List<Count> getCounts() { return counts == null ? List.of() : counts; }
    public long getTotal() { return total; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Count {
        @JsonProperty("entity_type")
        private String entityType;

        @JsonProperty("count")
        private long count;

        public String getEntityType() { return entityType; }
        public long getCount() { return count; }
    }
}
