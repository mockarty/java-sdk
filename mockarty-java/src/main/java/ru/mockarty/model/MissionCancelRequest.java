// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

/** Durable operator intent accepted by the unified mission cancel endpoint. */
public class MissionCancelRequest {
    private String reason;
    private String idempotencyKey;

    public MissionCancelRequest reason(String value) {
        this.reason = value == null ? null : value.trim();
        return this;
    }

    public MissionCancelRequest idempotencyKey(String value) {
        this.idempotencyKey = value == null ? null : value.trim();
        return this;
    }

    public String getReason() { return reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
