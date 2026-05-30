// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.cucumber;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import io.cucumber.plugin.event.WriteEvent;
import io.cucumber.plugin.event.EmbedEvent;
import ru.mockarty.junit5.allure.AllureLifecycle;
import ru.mockarty.junit5.allure.AllureModel;
import ru.mockarty.junit5.allure.Labels;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Cucumber-JVM plugin that emits Allure-2 result files via the shared
 * {@link AllureLifecycle}. Activate by adding
 * {@code --plugin ru.mockarty.cucumber.MockartyCucumberPlugin} to the
 * Cucumber options (or via {@code @CucumberOptions(plugin = ...)} on the
 * runner class).
 *
 * <p>One Cucumber {@code TestCase} = one Allure {@code TestResult}. Each
 * Gherkin step ({@code Given}/{@code When}/{@code Then}) becomes an
 * Allure step on that result.</p>
 *
 * <p>Run-level container: every Cucumber {@code TestRun} maps to a
 * single {@link AllureModel.Container} so the per-test results stay
 * grouped in the report tree.</p>
 */
public final class MockartyCucumberPlugin implements ConcurrentEventListener {

    /** Run-wide container UUID. */
    private volatile String runContainer;
    /** Pending child UUIDs (added under the run container). */
    private final List<String> pendingChildren = new ArrayList<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::onRunStarted);
        publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
        publisher.registerHandlerFor(TestCaseStarted.class, this::onCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onCaseFinished);
        publisher.registerHandlerFor(TestStepStarted.class, this::onStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
        publisher.registerHandlerFor(WriteEvent.class, this::onWrite);
        publisher.registerHandlerFor(EmbedEvent.class, this::onEmbed);
    }

    private void onRunStarted(TestRunStarted ev) {
        this.runContainer = AllureLifecycle.get().startContainer("Cucumber Run");
    }

    private void onRunFinished(TestRunFinished ev) {
        // Drain queued children — race-free because Cucumber events are
        // serialised per pickle on the publisher thread.
        synchronized (pendingChildren) {
            if (runContainer != null) {
                for (String child : pendingChildren) {
                    AllureLifecycle.get().addContainerChild(runContainer, child);
                }
                pendingChildren.clear();
                AllureLifecycle.get().stopContainer(runContainer);
                runContainer = null;
            }
        }
    }

    private void onCaseStarted(TestCaseStarted ev) {
        URI uri = ev.getTestCase().getUri();
        String name = ev.getTestCase().getName();
        String fullName = (uri == null ? "" : uri.toString()) + ":" + name;
        AllureLifecycle lc = AllureLifecycle.get();
        AllureModel.TestResult tr = lc.startTest(name, fullName);
        tr.historyId = AllureLifecycle.stableHistoryId(fullName, "");
        // Cucumber tags → Allure tags.
        for (String tag : ev.getTestCase().getTags()) {
            lc.addLabel(Labels.TAG, tag);
        }
        lc.addLabel(Labels.LANGUAGE, "java");
        lc.addLabel(Labels.FRAMEWORK, "cucumber-jvm");
        lc.addLabel(Labels.FEATURE, ev.getTestCase().getName());
        lc.addLabel(Labels.THREAD, Thread.currentThread().getName());
        synchronized (pendingChildren) {
            pendingChildren.add(tr.uuid);
        }
    }

    private void onCaseFinished(TestCaseFinished ev) {
        AllureLifecycle lc = AllureLifecycle.get();
        Result r = ev.getResult();
        if (r.getStatus() == Status.PASSED) {
            lc.markPassed();
        } else if (r.getStatus() == Status.SKIPPED || r.getStatus() == Status.PENDING) {
            lc.markSkipped(r.getError() == null ? null : r.getError().getMessage());
        } else if (r.getStatus() == Status.FAILED) {
            lc.markFailed(r.getError());
        } else if (r.getStatus() == Status.AMBIGUOUS || r.getStatus() == Status.UNDEFINED) {
            lc.markFailed(r.getError() == null
                    ? new RuntimeException("Cucumber status: " + r.getStatus())
                    : r.getError());
        }
        lc.stopTest();
    }

    private void onStepStarted(TestStepStarted ev) {
        if (ev.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) ev.getTestStep();
            // Gherkin keyword + step text — same shape as allure-cucumber7-jvm.
            String name = step.getStep().getKeyword() + step.getStep().getText();
            AllureLifecycle.get().startStep(name);
        }
    }

    private void onStepFinished(TestStepFinished ev) {
        if (ev.getTestStep() instanceof PickleStepTestStep) {
            Result r = ev.getResult();
            if (r.getStatus() == Status.PASSED) {
                AllureLifecycle.get().stopStep(true);
            } else if (r.getError() != null) {
                AllureLifecycle.get().stopStepFailed(r.getError());
            } else {
                AllureLifecycle.get().stopStep(false);
            }
        }
    }

    private void onWrite(WriteEvent ev) {
        AllureLifecycle.get().attachText("Cucumber log", ev.getText());
    }

    private void onEmbed(EmbedEvent ev) {
        AllureLifecycle.get().attachBinary(
                ev.getName() == null ? "embed" : ev.getName(),
                ev.getData(),
                ev.getMediaType());
    }

    // ── Utility surface kept package-visible for test ────────────────

    static byte[] toBytes(String s) {
        return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
    }
}
