// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.testplan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A parsed, validated Allure test plan ({@code testplan.json}).
 *
 * <p>Schema {@code version: "1.0"}:</p>
 *
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "tests": [
 *     {"id": 11111, "selector": "my.company.SimpleTest.simpleTestOne"}
 *   ]
 * }
 * }</pre>
 *
 * <p>{@code id} is the Allure id — the value an adapter carries on results
 * as the {@code AS_ID} / {@code ALLURE_ID} label, i.e. what
 * {@code @AllureId(123)} produces. {@code selector} is a full-name style
 * unique identifier. At least one of the two is present on every entry, and
 * a test is selected when <b>either</b> matches.</p>
 */
public final class AllureTestPlan {

    /** The only schema version Allure TestOps emits today. */
    public static final String SUPPORTED_VERSION = "1.0";

    private final String path;
    private final String version;
    private final List<Entry> entries;

    AllureTestPlan(String path, String version, List<Entry> entries) {
        this.path = path;
        this.version = version == null ? "" : version;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /** @return the file this plan was read from (for error messages). */
    public String getPath() {
        return path;
    }

    /** @return the declared schema version, or {@code ""} when absent. */
    public String getVersion() {
        return version;
    }

    /** @return the plan's entries, never null, never containing null. */
    public List<Entry> getEntries() {
        return entries;
    }

    /** @return true when the plan selects nothing ({@code "tests": []}). */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Match a discovered test against the plan.
     *
     * @param ids       every Allure id the test can be addressed by.
     * @param selectors every selector string the test can be addressed by.
     * @return true when any entry matches one of them.
     */
    public boolean matches(Collection<String> ids, Collection<String> selectors) {
        for (Entry entry : entries) {
            if (entry.getId() != null && ids != null && ids.contains(entry.getId())) {
                return true;
            }
            if (entry.getSelector() != null && selectors != null && selectors.contains(entry.getSelector())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "AllureTestPlan{path=" + path + ", version=" + version + ", entries=" + entries.size() + '}';
    }

    /** One {@code tests[]} element. At least one field is non-null. */
    public static final class Entry {

        private final String id;
        private final String selector;

        /**
         * @param id       the Allure id, or null.
         * @param selector the selector, or null.
         */
        public Entry(String id, String selector) {
            this.id = id;
            this.selector = selector;
        }

        /** @return the Allure id, or null when the entry is selector-only. */
        public String getId() {
            return id;
        }

        /** @return the selector, or null when the entry is id-only. */
        public String getSelector() {
            return selector;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry other = (Entry) o;
            return Objects.equals(id, other.id) && Objects.equals(selector, other.selector);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, selector);
        }

        @Override
        public String toString() {
            return "Entry{id=" + id + ", selector=" + selector + '}';
        }
    }
}
