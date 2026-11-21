// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

import com.fasterxml.jackson.databind.JsonNode;

/** Portable digest-bound Mission, immutable Brief, and complete journal. */
public class MissionArchiveEnvelope {
    private String digest;
    private JsonNode payload;

    public String getDigest() { return digest; }
    public JsonNode getPayload() { return payload; }

    public MissionArchiveEnvelope digest(String value) { this.digest = value; return this; }
    public MissionArchiveEnvelope payload(JsonNode value) { this.payload = value; return this; }
}
