// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.
package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The submit answer. {@link #isReplayed()} distinguishes an idempotent retry from
 * a fresh placement, so a pipeline never reports the same run twice.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeshSubmitResult {
    private MeshCommand command;
    private boolean replayed;

    public MeshCommand getCommand() { return command; }
    public boolean isReplayed() { return replayed; }
}
