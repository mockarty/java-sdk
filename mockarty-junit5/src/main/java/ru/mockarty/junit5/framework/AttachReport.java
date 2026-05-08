// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a test for auto-upload of its outcome and captured artifacts to
 * the bound TCM case run.
 *
 * <p>What gets uploaded:</p>
 * <ul>
 *   <li>Test outcome (passed / failed / skipped + duration).</li>
 *   <li>Recorded steps (every {@link Step} block with status + duration).</li>
 *   <li>Attachments registered via {@link Attachments#attach}.</li>
 *   <li>Failure reason (when present).</li>
 * </ul>
 *
 * <p>Fail-soft policy: when no Mockarty client is reachable or the case
 * binding is missing, the upload is silently skipped — your test never
 * fails because of reporting infrastructure.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AttachReport {
}
