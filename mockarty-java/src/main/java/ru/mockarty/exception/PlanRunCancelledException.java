// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Thrown by {@code TestPlanApi.waitForRun} when a run finishes in the
 * {@code cancelled} state.
 */
public class PlanRunCancelledException extends MockartyException {

    public PlanRunCancelledException(String message) {
        super(message);
    }
}
