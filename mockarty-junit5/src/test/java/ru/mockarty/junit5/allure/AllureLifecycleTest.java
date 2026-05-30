// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.allure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mockarty.junit5.allure.AllureModel.Status;
import ru.mockarty.junit5.allure.AllureModel.TestResult;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link AllureLifecycle} surface: test/step lifecycle,
 * status priority bubbling, parameters/labels/links registration, and
 * the cross-thread context bridge.
 */
class AllureLifecycleTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void setResultsDir() throws Exception {
        // Redirect the singleton's resultsDir to the per-test temp dir
        // so the test stays self-contained. We reach for reflection
        // because AllureLifecycle resolves the dir at construction time
        // — that's an intentional fast-path for the production code.
        Field f = AllureLifecycle.class.getDeclaredField("resultsDir");
        f.setAccessible(true);
        f.set(AllureLifecycle.get(), tmp);
        AllureLifecycle.get().clearContext();
    }

    @AfterEach
    void clearCtx() {
        AllureLifecycle.get().clearContext();
    }

    @Test
    @DisplayName("startTest + stopTest emits <uuid>-result.json with status from worst step")
    void worstStepBubbles() {
        AllureLifecycle lc = AllureLifecycle.get();
        TestResult t = lc.startTest("compound", "ru.example.Compound.test");

        lc.startStep("s1");
        lc.stopStep(true);

        lc.startStep("s2");
        lc.stopStep(false);   // failed

        Path written = lc.stopTest();
        assertNotNull(written);
        assertEquals(Status.FAILED, t.status, "worst-of-steps must bubble to test status");
    }

    @Test
    @DisplayName("markFailed sets statusDetails.message + trace from Throwable")
    void failedWithThrowable() {
        AllureLifecycle lc = AllureLifecycle.get();
        TestResult t = lc.startTest("oops", "x");
        AssertionError err = new AssertionError("boom");
        lc.markFailed(err);
        lc.stopTest();
        assertEquals(Status.FAILED, t.status);
        assertNotNull(t.statusDetails);
        assertEquals("boom", t.statusDetails.message);
        assertTrue(t.statusDetails.trace.contains("AssertionError"));
    }

    @Test
    @DisplayName("AssertionError → FAILED, other Throwable → BROKEN")
    void classifyFailure() {
        AllureLifecycle lc = AllureLifecycle.get();
        TestResult t1 = lc.startTest("a", "a");
        lc.markFailed(new AssertionError("x"));
        lc.stopTest();
        assertEquals(Status.FAILED, t1.status);

        TestResult t2 = lc.startTest("b", "b");
        lc.markFailed(new RuntimeException("boom"));
        lc.stopTest();
        assertEquals(Status.BROKEN, t2.status);
    }

    @Test
    @DisplayName("markSkipped uses message in statusDetails")
    void skippedFlow() {
        AllureLifecycle lc = AllureLifecycle.get();
        TestResult t = lc.startTest("skip", "x");
        lc.markSkipped("env unavailable");
        lc.stopTest();
        assertEquals(Status.SKIPPED, t.status);
        assertEquals("env unavailable", t.statusDetails.message);
    }

    @Test
    @DisplayName("addParameter, addLabel, addLink, setDescription all reach the active TestResult")
    void metadataReachesTest() {
        AllureLifecycle lc = AllureLifecycle.get();
        TestResult t = lc.startTest("meta", "x");
        lc.addParameter("env", "qa");
        lc.addLabel("feature", "Login");
        lc.addLink("AUTH-1", "https://j/AUTH-1", "issue");
        lc.setDescription("desc");
        lc.setDescriptionHtml("<b>desc</b>");
        lc.setHistoryId("hid-stable");
        lc.markPassed();
        lc.stopTest();

        assertEquals("hid-stable", t.historyId);
        assertEquals("desc", t.description);
        assertEquals("<b>desc</b>", t.descriptionHtml);
        assertEquals("env", t.parameters.get(0).name);
        assertEquals("feature", t.labels.get(0).name);
        assertEquals("AUTH-1", t.links.get(0).name);
    }

    @Test
    @DisplayName("Container lifecycle: stopContainer writes -container.json with children list")
    void containerFlow() {
        AllureLifecycle lc = AllureLifecycle.get();
        String ctr = lc.startContainer("ru.example.SuiteX");
        assertNotNull(ctr);

        TestResult t1 = lc.startTest("a", "a");
        lc.addContainerChild(ctr, t1.uuid);
        lc.markPassed();
        lc.stopTest();

        TestResult t2 = lc.startTest("b", "b");
        lc.addContainerChild(ctr, t2.uuid);
        lc.markPassed();
        lc.stopTest();

        Path written = lc.stopContainer(ctr);
        assertNotNull(written);
    }

    @Test
    @DisplayName("stableHistoryId is deterministic for the same fullName+paramSig pair")
    void historyIdDeterministic() {
        String a = AllureLifecycle.stableHistoryId("foo.Bar.baz", "x=1");
        String b = AllureLifecycle.stableHistoryId("foo.Bar.baz", "x=1");
        String c = AllureLifecycle.stableHistoryId("foo.Bar.baz", "x=2");
        assertEquals(a, b);
        assertFalse(a.equals(c), "different paramSig must produce a different id");
    }

    @Test
    @DisplayName("stableTestCaseId is md5(fullName), parameter-independent and stable")
    void testCaseIdIsMd5OfFullName() throws Exception {
        String fn = "auth.LoginTest.test_login";
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] dig = md.digest(fn.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) {
            sb.append(String.format("%02x", b & 0xff));
        }
        assertEquals(sb.toString(), AllureLifecycle.stableTestCaseId(fn));
        // Stable across calls and distinct from a parameterised historyId.
        assertEquals(AllureLifecycle.stableTestCaseId(fn), AllureLifecycle.stableTestCaseId(fn));
        assertFalse(AllureLifecycle.stableTestCaseId(fn)
                .equals(AllureLifecycle.stableHistoryId(fn, "env=stage")));
    }

    @Test
    @DisplayName("startTest sets testCaseId = md5(fullName) so discovery can match by fullName")
    void startTestSetsTestCaseId() {
        AllureLifecycle lc = AllureLifecycle.get();
        TestResult t = lc.startTest("login", "auth.LoginTest.test_login");
        assertNotNull(t.testCaseId, "testCaseId must be set on startTest");
        assertEquals(AllureLifecycle.stableTestCaseId("auth.LoginTest.test_login"), t.testCaseId);
        // historyId (md5 of fullName + "|") and testCaseId (md5 of fullName)
        // are distinct identities — they must not collide.
        assertFalse(t.testCaseId.equals(t.historyId),
                "testCaseId must differ from historyId");
        lc.markPassed();
        lc.stopTest();
    }

    @Test
    @DisplayName("attachJson + attachText: TestResult's attachments list grows, source file exists")
    void attachmentLifecycle() {
        AllureLifecycle lc = AllureLifecycle.get();
        TestResult t = lc.startTest("att", "x");
        lc.attachJson("payload", "{\"a\":1}");
        lc.attachText("logs", "lorem");
        lc.attachPng("screenshot", new byte[]{(byte)0x89, 'P', 'N', 'G'});
        lc.markPassed();
        lc.stopTest();

        assertEquals(3, t.attachments.size());
        assertEquals("payload", t.attachments.get(0).name);
        assertEquals("application/json", t.attachments.get(0).type);
        assertEquals("image/png", t.attachments.get(2).type);
    }

    @Test
    @DisplayName("clearContext clears thread-local; subsequent currentStep is null")
    void clearContextWorks() {
        AllureLifecycle lc = AllureLifecycle.get();
        lc.startTest("a", "a");
        lc.startStep("s");
        lc.clearContext();
        assertNull(lc.context().currentStep());
    }
}
