// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

/** Result of an atomic archive restore or exact idempotent replay. */
public class MissionArchiveRestoreResponse {
    private String id;
    private String digest;
    private boolean created;

    public String getId() { return id; }
    public String getDigest() { return digest; }
    public boolean isCreated() { return created; }
}
