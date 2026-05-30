// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.testcontainers;

import java.util.Locale;

/**
 * Stub-dialect mode passed to the running container via the
 * {@code MOCKARTY_STUB_FORMAT} env var.
 */
public enum Format {

    /** Sniff each file -- default. */
    AUTO,

    /** Force WireMock JSON parsing. */
    WIREMOCK,

    /** Force Mockarty native JSON parsing. */
    MOCKARTY,

    /** Force Mockoon-3.x environment JSON parsing. */
    MOCKOON;

    /** Lower-case slug the CLI reads. */
    public String slug() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parse a slug back to a Format, throwing on unknown values. Used
     * by builder validation so a typo never silently sets the wrong
     * mode.
     */
    public static Format fromSlug(String slug) {
        if (slug == null) {
            throw new IllegalArgumentException("format must not be null");
        }
        try {
            return Format.valueOf(slug.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "unknown format \"" + slug + "\" (valid: auto, wiremock, mockarty, mockoon)",
                e);
        }
    }
}
