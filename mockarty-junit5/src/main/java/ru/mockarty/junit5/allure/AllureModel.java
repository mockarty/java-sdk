// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.allure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory model of the Allure 2 result schema.
 *
 * <p>Two payload shapes ship out of this package:</p>
 * <ul>
 *   <li>{@link TestResult} → emitted as {@code <uuid>-result.json}.</li>
 *   <li>{@link Container} → emitted as {@code <uuid>-container.json} when
 *       a JUnit5 {@code @BeforeAll}/{@code @AfterAll} fixture wraps the
 *       contained {@link TestResult}s.</li>
 * </ul>
 *
 * <p>Fields are public for direct mutation — the writer never observes
 * the model outside the test thread that owns it, so a record-style API
 * would needlessly multiply allocations on every step push.</p>
 *
 * <h2>Schema parity with allure2</h2>
 * <p>Field names match the canonical Allure 2 schema exactly — see
 * {@code io.qameta.allure.model.TestResult} / {@code StepResult} in
 * allure-java-commons. Optional fields stay {@code null} so the writer
 * can drop them from the JSON output (the schema treats absent and
 * {@code null} identically).</p>
 */
public final class AllureModel {

    private AllureModel() {}

    /** Canonical Allure status enum values. Must be lowercase strings on the wire. */
    public enum Status {
        PASSED, FAILED, BROKEN, SKIPPED, UNKNOWN;

        public String toWire() {
            return name().toLowerCase();
        }

        /**
         * Pick the worst status across a list of steps. Allure priority:
         * failed &gt; broken &gt; unknown &gt; skipped &gt; passed.
         */
        public static Status worst(List<StepResult> steps) {
            Status worst = PASSED;
            for (StepResult s : steps) {
                Status st = s.status == null ? PASSED : s.status;
                if (priority(st) > priority(worst)) {
                    worst = st;
                }
            }
            return worst;
        }

        private static int priority(Status s) {
            switch (s) {
                case FAILED:  return 4;
                case BROKEN:  return 3;
                case UNKNOWN: return 2;
                case SKIPPED: return 1;
                case PASSED:  default: return 0;
            }
        }
    }

    /** Canonical Allure stage enum. */
    public enum Stage {
        SCHEDULED, RUNNING, FINISHED, PENDING, INTERRUPTED;

        public String toWire() {
            return name().toLowerCase();
        }
    }

    /**
     * Detail block carried inside both {@code TestResult} and
     * {@code StepResult} to describe a failure: short message + full
     * stack trace + flake bit + known bit.
     */
    public static final class StatusDetails {
        public boolean known;
        public boolean muted;
        public boolean flaky;
        public String message;
        public String trace;
    }

    /** One label k=v pair attached to the test (or container). */
    public static final class Label {
        public String name;
        public String value;

        public Label() {}
        public Label(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /** External link (issue tracker, TMS, custom). */
    public static final class Link {
        public String name;
        public String url;
        public String type; // "issue" | "tms" | custom

        public Link() {}
        public Link(String name, String url, String type) {
            this.name = name;
            this.url = url;
            this.type = type;
        }
    }

    /** Test-level / step-level parameter. */
    public static final class Parameter {
        public String name;
        public String value;
        /** "default" | "masked" | "hidden" — Allure 2.13+. */
        public String mode;
        public Boolean excluded;

        public Parameter() {}
        public Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /** Attachment record carried inside Test/Step result. */
    public static final class Attachment {
        public String name;
        /** Relative filename on disk inside {@code allure-results/}. */
        public String source;
        public String type; // MIME

        public Attachment() {}
        public Attachment(String name, String source, String type) {
            this.name = name;
            this.source = source;
            this.type = type;
        }
    }

    /** One nested step inside a TestResult. Allure supports arbitrary depth. */
    public static final class StepResult {
        public String name;
        public Status status;
        public StatusDetails statusDetails;
        public Stage stage;
        public long start;
        public long stop;
        public final List<Parameter> parameters = new ArrayList<>();
        public final List<Attachment> attachments = new ArrayList<>();
        public final List<StepResult> steps = new ArrayList<>();
    }

    /** Top-level test record — emitted as {@code <uuid>-result.json}. */
    public static final class TestResult {
        public String uuid;
        public String historyId;
        public String testCaseId;
        public String name;
        public String fullName;
        public Status status;
        public StatusDetails statusDetails;
        public Stage stage;
        public long start;
        public long stop;
        public String description;
        public String descriptionHtml;
        public final List<Label> labels = new ArrayList<>();
        public final List<Link> links = new ArrayList<>();
        public final List<Parameter> parameters = new ArrayList<>();
        public final List<Attachment> attachments = new ArrayList<>();
        public final List<StepResult> steps = new ArrayList<>();
    }

    /** Container — emitted as {@code <uuid>-container.json}. Wraps before/after fixtures. */
    public static final class Container {
        public String uuid;
        public String name;
        public long start;
        public long stop;
        public final List<String> children = new ArrayList<>();
        public final List<StepResult> befores = new ArrayList<>();
        public final List<StepResult> afters = new ArrayList<>();
        public final List<Link> links = new ArrayList<>();
    }

    /** Mutable per-thread label/link aggregator used by AllureLifecycle. */
    public static final class MutableLabels {
        public final Map<String, List<Label>> byName = new LinkedHashMap<>();

        public void add(Label l) {
            byName.computeIfAbsent(l.name, k -> new ArrayList<>()).add(l);
        }

        public List<Label> flatten() {
            List<Label> out = new ArrayList<>();
            for (List<Label> v : byName.values()) {
                out.addAll(v);
            }
            return out;
        }
    }
}
