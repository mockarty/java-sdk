// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/** The delegated-credential list response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegatedCredentialList {
    @JsonProperty("credentials") private List<DelegatedCredential> credentials;
    @JsonProperty("count") private int count;

    public DelegatedCredentialList() { }

    public List<DelegatedCredential> getCredentials() {
        return credentials == null ? Collections.emptyList() : credentials;
    }

    public int getCount() { return count; }
}
