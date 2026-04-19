// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A file attachment referenced by an {@link ItemSummary} or
 * {@link ItemSummaryStep} — maps to an Allure attachment entry.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemSummaryAttachment {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("source")
    private String source;

    public ItemSummaryAttachment() {
    }

    public String getName() {
        return name;
    }

    /** MIME type (e.g. {@code application/json}, {@code text/plain}). */
    public String getType() {
        return type;
    }

    /**
     * Opaque URL or storage key the server uses to retrieve the bytes.
     * Download via the signed link returned by the report endpoint.
     */
    public String getSource() {
        return source;
    }
}
