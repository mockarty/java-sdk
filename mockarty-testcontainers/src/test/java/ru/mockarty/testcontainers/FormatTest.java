// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.testcontainers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Format enum slug + parse coverage. */
class FormatTest {

    @Test
    void slugIsLowercase() {
        assertEquals("auto", Format.AUTO.slug());
        assertEquals("wiremock", Format.WIREMOCK.slug());
        assertEquals("mockarty", Format.MOCKARTY.slug());
        assertEquals("mockoon", Format.MOCKOON.slug());
    }

    @ParameterizedTest
    @ValueSource(strings = {"auto", "AUTO", "Auto", "wiremock", "WIREMOCK"})
    void fromSlugAcceptsCaseInsensitive(String input) {
        Format f = Format.fromSlug(input);
        assertEquals(input.toLowerCase(java.util.Locale.ROOT), f.slug());
    }

    @Test
    void fromSlugRejectsNull() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Format.fromSlug(null));
        assertTrue(ex.getMessage().contains("must not be null"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "zzz", "json", "wirem0ck"})
    void fromSlugRejectsUnknown(String bad) {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Format.fromSlug(bad));
        assertTrue(ex.getMessage().contains("unknown format"));
    }
}
