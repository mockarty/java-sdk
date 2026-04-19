// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Envelope returned by the bulk-purge endpoints (207-style partial results). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkPurgeResult {

    @JsonProperty("purged")
    private List<Outcome> purged;

    @JsonProperty("failed")
    private List<Outcome> failed;

    @JsonProperty("not_found")
    private List<String> notFound;

    public List<Outcome> getPurged() { return purged == null ? List.of() : purged; }
    public List<Outcome> getFailed() { return failed == null ? List.of() : failed; }
    public List<String> getNotFound() { return notFound == null ? List.of() : notFound; }

    /** Outcome of a single cascade group in a bulk purge. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Outcome {
        @JsonProperty("cascade_group_id")
        private String cascadeGroupId;

        @JsonProperty("entity_type")
        private String entityType;

        @JsonProperty("rows_deleted")
        private long rowsDeleted;

        @JsonProperty("error")
        private String error;

        public String getCascadeGroupId() { return cascadeGroupId; }
        public String getEntityType() { return entityType; }
        public long getRowsDeleted() { return rowsDeleted; }
        public String getError() { return error; }
    }
}
