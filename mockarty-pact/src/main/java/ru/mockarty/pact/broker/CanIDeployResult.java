// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.broker;

/**
 * Result of {@code GET /can-i-deploy}.
 *
 * @param deployable {@code true} when the broker says safe to deploy.
 * @param reason     human-readable explanation when not deployable.
 * @param raw        full response body in case callers need extra fields
 *                   (verifications matrix, etc.).
 */
public record CanIDeployResult(boolean deployable, String reason, byte[] raw) {
}
