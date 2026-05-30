// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.allure;

/**
 * Canonical Allure label names. Matches the {@code LabelName} enum in
 * allure-java-commons byte-for-byte. Using a class of {@code public
 * static final String} constants (instead of an enum) keeps the surface
 * stable when allure-java adds new label kinds — adding a constant
 * doesn't break consumers that only read the value.
 */
public final class Labels {

    private Labels() {}

    public static final String SUITE = "suite";
    public static final String PARENT_SUITE = "parentSuite";
    public static final String SUB_SUITE = "subSuite";

    public static final String FEATURE = "feature";
    public static final String STORY = "story";
    public static final String EPIC = "epic";

    public static final String OWNER = "owner";
    public static final String LEAD = "lead";

    public static final String SEVERITY = "severity";

    public static final String TAG = "tag";
    public static final String LABEL = "label";

    public static final String FRAMEWORK = "framework";
    public static final String LANGUAGE = "language";
    public static final String HOST = "host";
    public static final String THREAD = "thread";

    public static final String PACKAGE = "package";
    public static final String TEST_CLASS = "testClass";
    public static final String TEST_METHOD = "testMethod";

    public static final String AS_ID = "AS_ID";

    /** Severity values used with {@link #SEVERITY}. Wire form is lowercase. */
    public static final class Severity {
        private Severity() {}
        public static final String BLOCKER = "blocker";
        public static final String CRITICAL = "critical";
        public static final String NORMAL = "normal";
        public static final String MINOR = "minor";
        public static final String TRIVIAL = "trivial";

        /** Canonicalise free-form input (e.g. {@code "CRITICAL"} → {@code "critical"}). */
        public static String canonical(String raw) {
            if (raw == null) {
                return NORMAL;
            }
            String s = raw.trim().toLowerCase();
            switch (s) {
                case "blocker":
                case "critical":
                case "normal":
                case "minor":
                case "trivial":
                    return s;
                default:
                    return NORMAL;
            }
        }
    }

    /** Link type values. */
    public static final class LinkType {
        private LinkType() {}
        public static final String ISSUE = "issue";
        public static final String TMS = "tms";
        public static final String CUSTOM = "custom";

        /**
         * Resolve a link URL through {@code allure.link.{type}.pattern}
         * — same env contract as Allure-pytest/allure-junit5. Pattern may
         * contain a {@code {}} placeholder for the bare identifier.
         */
        public static String resolveUrl(String type, String idOrUrl) {
            if (idOrUrl == null || idOrUrl.isEmpty()) {
                return idOrUrl;
            }
            if (idOrUrl.startsWith("http://") || idOrUrl.startsWith("https://")) {
                return idOrUrl;
            }
            if (type == null || type.isEmpty()) {
                return idOrUrl;
            }
            String key = "allure.link." + type + ".pattern";
            String pattern = System.getProperty(key);
            if (pattern == null || pattern.isEmpty()) {
                pattern = System.getenv("ALLURE_LINK_" + type.toUpperCase() + "_PATTERN");
            }
            if (pattern == null || pattern.isEmpty()) {
                return idOrUrl;
            }
            return pattern.replace("{}", idOrUrl);
        }
    }
}
