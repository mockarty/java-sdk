// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

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
