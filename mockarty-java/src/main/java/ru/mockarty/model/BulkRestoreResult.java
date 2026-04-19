// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Envelope returned by the bulk-restore endpoints (207-style partial results). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkRestoreResult {

    @JsonProperty("restored")
    private List<Outcome> restored;

    @JsonProperty("failed")
    private List<Outcome> failed;

    @JsonProperty("not_found")
    private List<String> notFound;

    public List<Outcome> getRestored() { return restored == null ? List.of() : restored; }
    public List<Outcome> getFailed() { return failed == null ? List.of() : failed; }
    public List<String> getNotFound() { return notFound == null ? List.of() : notFound; }

    /** Outcome of a single cascade group in a bulk restore. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Outcome {
        @JsonProperty("cascade_group_id")
        private String cascadeGroupId;

        @JsonProperty("entity_type")
        private String entityType;

        @JsonProperty("restored_count")
        private int restoredCount;

        @JsonProperty("error")
        private String error;

        public String getCascadeGroupId() { return cascadeGroupId; }
        public String getEntityType() { return entityType; }
        public int getRestoredCount() { return restoredCount; }
        public String getError() { return error; }
    }
}
