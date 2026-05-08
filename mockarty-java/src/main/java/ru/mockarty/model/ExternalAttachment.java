// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Inline attachment for an {@link ExternalRunRequest}. Bodies are
 * base64-encoded so the whole envelope rides as a single JSON POST;
 * heavy artefacts should go through the regular {@code /tcm/attachments}
 * multipart endpoint instead.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalAttachment {

    @JsonProperty("name")
    private String name;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("bodyB64")
    private String bodyB64;

    public ExternalAttachment() {}

    /** Build a text/plain attachment from a UTF-8 string. */
    public static ExternalAttachment text(String name, String body) {
        return new ExternalAttachment()
                .name(name)
                .contentType("text/plain; charset=utf-8")
                .body(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
    }

    /** Build an application/json attachment from a serialised JSON string. */
    public static ExternalAttachment json(String name, String json) {
        return new ExternalAttachment()
                .name(name)
                .contentType("application/json")
                .body(json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8));
    }

    public ExternalAttachment name(String n) { this.name = n; return this; }
    public ExternalAttachment contentType(String t) { this.contentType = t; return this; }

    /** Encode raw bytes as base64 for transport. */
    public ExternalAttachment body(byte[] body) {
        this.bodyB64 = Base64.getEncoder().encodeToString(body == null ? new byte[0] : body);
        return this;
    }

    /** Set a pre-encoded base64 body directly (advanced — most callers want body()). */
    public ExternalAttachment bodyB64(String s) { this.bodyB64 = s; return this; }

    public String getName() { return name; }
    public String getContentType() { return contentType; }
    public String getBodyB64() { return bodyB64; }
}
