// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test fixtures discovered explicitly (via {@code selectClass}) by the
 * discovery tree-walk tests. They live under their own top-level file so
 * the assembler's {@code sourceRef} maps to {@code DiscoveryFixtures.java}
 * (the top-level source file), and so the module's own test engine never
 * picks them up as real tests — they are nested under this non-test holder
 * and only reached by explicit selection.
 */
final class DiscoveryFixtures {

    private DiscoveryFixtures() {}

    static class SampleTest {
        @Test
        @Tag("smoke")
        @Tag("fast")
        void alpha() {}

        @Test
        @DisplayName("Beta scenario")
        void beta() {}

        @Test
        void gamma() {}
    }

    static class OuterTest {
        @Test
        void outerCase() {}

        @Nested
        class Inner {
            @Test
            void innerCase() {}
        }
    }
}
