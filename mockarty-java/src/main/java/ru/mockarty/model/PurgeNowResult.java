// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response of the manual retention-scheduler tick. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurgeNowResult {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("purged_total")
    private long purgedTotal;

    @JsonProperty("namespaces_scanned")
    private int namespacesScanned;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public long getPurgedTotal() { return purgedTotal; }
    public int getNamespacesScanned() { return namespacesScanned; }
}
