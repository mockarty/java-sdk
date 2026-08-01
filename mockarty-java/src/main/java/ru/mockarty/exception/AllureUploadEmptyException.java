// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Thrown when an {@code allure-results} directory contains no results at all.
 *
 * <p>A CI step that finds nothing to upload means the test run produced
 * nothing; returning an empty list turned that into a silently green
 * pipeline.</p>
 */
public class AllureUploadEmptyException extends MockartyException {

    public AllureUploadEmptyException(String message) {
        super(message);
    }
}
