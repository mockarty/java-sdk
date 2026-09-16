// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The create/rotate response — the ONLY place a plain bearer is returned.
 *
 * <p>The row stores bcrypt + sha256, so the bearer cannot be read back. A caller
 * that loses it must rotate.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegatedCredentialWithToken {
    @JsonProperty("credential") private DelegatedCredential credential;
    @JsonProperty("token") private String token;
    @JsonProperty("message") private String message;

    public DelegatedCredentialWithToken() { }

    public DelegatedCredential getCredential() { return credential; }
    public String getToken() { return token; }
    public String getMessage() { return message; }
}
