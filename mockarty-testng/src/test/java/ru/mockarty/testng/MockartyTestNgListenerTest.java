// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.testng;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import ru.mockarty.junit5.allure.AllureLifecycle;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives a tiny TestNG suite programmatically and asserts that the
 * Mockarty listener emits one Allure {@code -result.json} per TestNG
 * test method + one {@code -container.json} per test class.
 *
 * <p>Uses JUnit5 as the harness so we can mix-and-match with the rest
 * of the SDK's test infrastructure — TestNG is wired in only inside
 * the inner {@link SampleTestNgSuite} class.</p>
 */
class MockartyTestNgListenerTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void redirectResultsDir() throws Exception {
        Field f = AllureLifecycle.class.getDeclaredField("resultsDir");
        f.setAccessible(true);
        f.set(AllureLifecycle.get(), tmp);
        AllureLifecycle.get().clearContext();
    }

    @AfterEach
    void cleanCtx() {
        AllureLifecycle.get().clearContext();
    }

    public static class SampleTestNgSuite {
        @org.testng.annotations.Test
        public void passingMethod() {}

        @org.testng.annotations.Test
        public void anotherPassingMethod() {}
    }

    @Test
    @DisplayName("TestNG suite of 2 methods → 2 -result.json + 1 -container.json")
    void testNgListenerEmitsAllure() throws Exception {
        TestNG ng = new TestNG();
        ng.setListenerClasses(Collections.singletonList(MockartyTestNgListener.class));

        XmlSuite suite = new XmlSuite();
        suite.setName("mockarty-testng-smoke");
        XmlTest test = new XmlTest(suite);
        test.setName("smoke");
        test.setXmlClasses(Collections.singletonList(new XmlClass(SampleTestNgSuite.class)));
        ng.setXmlSuites(Collections.singletonList(suite));

        // Suppress TestNG's report-to-disk in cwd.
        ng.setOutputDirectory(tmp.resolve("testng-report").toString());
        ng.setVerbose(0);
        ng.run();

        long results = Files.list(tmp).filter(p -> p.getFileName().toString().endsWith("-result.json")).count();
        long containers = Files.list(tmp).filter(p -> p.getFileName().toString().endsWith("-container.json")).count();
        assertEquals(2, results, "two -result.json files expected (one per test method)");
        assertEquals(1, containers, "one -container.json expected (one per test class)");
        assertTrue(ng.hasFailure() == false, "no test methods should fail");
    }
}
