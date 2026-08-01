// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A key/value tag attached to an {@link ExternalRunRequest}. The
 * {@code type} discriminator is informational ("string" | "number" | "url")
 * and drives how the TCM UI renders the value. Mirrors the Go SDK's
 * {@code CustomField} and the server's {@code internal/testcase/external_run.go}.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class CustomField {

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    @JsonProperty("type")
    private String type;

    public CustomField() {}

    public CustomField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public CustomField name(String n) { this.name = n; return this; }
    public CustomField value(String v) { this.value = v; return this; }
    public CustomField type(String t) { this.type = t; return this; }

    public String getName() { return name; }
    public String getValue() { return value; }
    public String getType() { return type; }
}
