// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.testplan;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.mockarty.junit5.framework.TestCase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fixture tests for the Allure test-plan filter. They carry real
 * {@code @Test} methods purely so the filter tests can {@code selectClass()}
 * them; they are excluded from this module's own test run (see
 * {@code build.gradle.kts}).
 */
final class TestPlanFixtures {

    private TestPlanFixtures() {}

    /**
     * Local stand-in for {@code io.qameta.allure.AllureId}. The filter reads
     * the annotation reflectively by SIMPLE NAME, so Allure does not have to
     * be on a consumer's classpath — this fixture proves exactly that.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AllureId {
        String value();
    }

    /** Addressable by selector only. */
    static class SelectorOnly {
        @Test
        void alpha() {}

        @Test
        void gamma() {}

        /** Carries a parameter, so its fullName includes the type list. */
        @Test
        void withParam(org.junit.jupiter.api.TestInfo info) {}
    }

    /** Addressable by the various id shapes. */
    static class WithIds {
        @AllureId("777")
        @Test
        void pinnedByAnnotation() {}

        @Tag("allure.id:888")
        @Test
        void pinnedByTag() {}

        @TestCase("CASE-9")
        @Test
        void pinnedByMockartyCase() {}
    }
}
