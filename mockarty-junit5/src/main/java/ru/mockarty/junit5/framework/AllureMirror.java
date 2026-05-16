// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection-based harvester for {@code io.qameta.allure.*} annotations
 * + best-effort bridge from Mockarty steps into Allure's runtime.
 *
 * <p>Owner decision 2026-05-16 ({@code SDK_FRAMEWORK_PLAN.md} §3.3 + §4.3.2):
 * mirror-mode is DEFAULT-ON across all three SDKs. The user's existing
 * Allure-annotated tests must flow through the Mockarty case frame
 * unchanged.</p>
 *
 * <h2>Design</h2>
 * <ul>
 *   <li><b>FQCN endsWith detection</b> — we match the annotation's fully
 *       qualified class name (e.g. {@code io.qameta.allure.Severity}) by
 *       suffix ({@code .Severity}). This avoids a hard dependency on
 *       {@code io.qameta.allure:allure-java-commons} from the user's
 *       project; the SDK works whether Allure is on the classpath or
 *       not.</li>
 *   <li><b>Per-method cache</b> — the harvest result for each
 *       {@link Method} is computed once and stored in a
 *       {@link ConcurrentHashMap}. After the first hit per test method
 *       lookup is a hash-map peek (~µs).</li>
 *   <li><b>Bridge from Mockarty to Allure</b> — when
 *       {@code io.qameta.allure.Allure} is on the classpath, every
 *       {@link Step} block also opens an Allure step via reflection so
 *       the Allure report records what the Mockarty case frame already
 *       has (no manual {@code Allure.step(...)} call required). Silent
 *       no-op when the class is absent.</li>
 * </ul>
 *
 * <h2>Cost</h2>
 * <p>~50-150 µs on first hit per method (annotation iteration + ~10
 * {@code String.endsWith} probes). After the first hit, ~30 ns map
 * lookup. The Allure bridge adds one {@code Class.forName} probe
 * (cached as a singleton boolean), then either a no-op or a single
 * reflective method invocation per step.</p>
 *
 * <h2>Disabling</h2>
 * <p>Set {@code @MockartyTest(mirrorAllure = false)} on the test class
 * to skip the harvest entirely. The Allure-to-step bridge does not need
 * disabling — without Allure on the classpath it is already a no-op.</p>
 */
public final class AllureMirror {

    /** Per-method cached harvest. */
    private static final Map<Method, Harvested> METHOD_CACHE = new ConcurrentHashMap<>();
    /** Per-class cached harvest. */
    private static final Map<Class<?>, Harvested> CLASS_CACHE = new ConcurrentHashMap<>();

    /** Suffixes (with leading dot) we treat as Allure-equivalent. Order
     * intentional — most common first to short-circuit. */
    private static final String[] ALLURE_SUFFIXES = {
            ".Step",
            ".Severity",
            ".Description",
            ".Feature",
            ".Story",
            ".Epic",
            ".Owner",
            ".Issue",
            ".TmsLink",
            ".Link",
            ".Tag",
            ".Label",
            ".Title",
            ".Parameter",
            ".Attachment",
    };

    /** Cached result of probing the Allure runtime bridge. {@code null}
     * means "not probed yet". */
    private static volatile Boolean allureRuntimeAvailable;

    private AllureMirror() {}

    /** Harvest Allure annotations from a single method (plus its declaring
     * class). Returns an empty {@link Harvested} when nothing is found —
     * never {@code null}. */
    public static Harvested harvest(Method method) {
        if (method == null) {
            return Harvested.EMPTY;
        }
        return METHOD_CACHE.computeIfAbsent(method, AllureMirror::computeMethod);
    }

    /** Harvest Allure annotations from a class (e.g. for class-level
     * {@code @Feature}/{@code @Story} that apply to every test). */
    public static Harvested harvest(Class<?> cls) {
        if (cls == null) {
            return Harvested.EMPTY;
        }
        return CLASS_CACHE.computeIfAbsent(cls, AllureMirror::computeClass);
    }

    /** Lift Allure metadata onto the active Mockarty case frame.
     * Fail-soft: no-op when there is no active case frame. */
    public static void apply(MockartyContext.CaseFrame frame, Harvested h) {
        if (frame == null || h == null || h.isEmpty()) {
            return;
        }
        if (h.title != null) {
            frame.metadata.put("title", h.title);
        }
        if (h.description != null) {
            frame.metadata.put("description", h.description);
        }
        if (h.severity != null) {
            frame.metadata.put("severity", h.severity);
        }
        if (h.owner != null) {
            frame.metadata.put("owner", h.owner);
        }
        if (!h.features.isEmpty()) {
            frame.metadata.put("features", new ArrayList<>(h.features));
        }
        if (!h.stories.isEmpty()) {
            frame.metadata.put("stories", new ArrayList<>(h.stories));
        }
        if (!h.epics.isEmpty()) {
            frame.metadata.put("epics", new ArrayList<>(h.epics));
        }
        if (!h.issues.isEmpty()) {
            frame.metadata.put("issues", new ArrayList<>(h.issues));
        }
        if (!h.tmsLinks.isEmpty()) {
            frame.metadata.put("tmsLinks", new ArrayList<>(h.tmsLinks));
        }
        if (!h.links.isEmpty()) {
            frame.metadata.put("links", new ArrayList<>(h.links));
        }
        if (!h.tags.isEmpty()) {
            frame.metadata.put("tags", new ArrayList<>(h.tags));
        }
        if (!h.labels.isEmpty()) {
            frame.metadata.put("labels", new HashMap<>(h.labels));
        }
        if (!h.parameters.isEmpty()) {
            frame.metadata.put("parameters", new HashMap<>(h.parameters));
        }
    }

    // ── Allure runtime bridge ────────────────────────────────────────

    /** Probe (and cache) whether {@code io.qameta.allure.Allure} is on
     * the classpath. */
    public static boolean isAllureRuntimeAvailable() {
        Boolean cached = allureRuntimeAvailable;
        if (cached != null) {
            return cached;
        }
        boolean available;
        try {
            Class.forName("io.qameta.allure.Allure", false,
                    AllureMirror.class.getClassLoader());
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
        allureRuntimeAvailable = available;
        return available;
    }

    /** Mirror a Mockarty step into Allure's runtime via reflection.
     * Silent no-op when Allure is absent OR no Allure test case is
     * currently active (i.e. the user did not register an Allure
     * listener for this JUnit test). Returns an opaque token that must
     * be passed to {@link #endAllureStep(Object, boolean)} on close. */
    public static Object beginAllureStep(String name) {
        if (!isAllureRuntimeAvailable() || name == null || name.isEmpty()) {
            return null;
        }
        try {
            Class<?> allure = Class.forName("io.qameta.allure.Allure",
                    true, AllureMirror.class.getClassLoader());
            Method getLifecycle = allure.getMethod("getLifecycle");
            Object lifecycle = getLifecycle.invoke(null);
            // Guard: only mirror when Allure already has a current test
            // container/case. Otherwise startStep logs ERROR and the
            // step is rejected — that pollutes the user's test output.
            if (!allureHasCurrentTestCase(lifecycle)) {
                return null;
            }
            // Allure.startStep(uuid, stepResult) — returns void; we
            // generate a uuid ourselves so we can close it deterministically.
            String uuid = java.util.UUID.randomUUID().toString();
            Class<?> stepResult = Class.forName(
                    "io.qameta.allure.model.StepResult",
                    true, AllureMirror.class.getClassLoader());
            Object result = stepResult.getDeclaredConstructor().newInstance();
            stepResult.getMethod("setName", String.class).invoke(result, name);
            lifecycle.getClass()
                    .getMethod("startStep", String.class, stepResult)
                    .invoke(lifecycle, uuid, result);
            return uuid;
        } catch (Throwable t) {
            // Fail-soft — Allure on classpath but API mismatch.
            return null;
        }
    }

    /** Probe whether the active Allure lifecycle has a current test
     * case. Allure 2.x exposes {@code getCurrentTestCase()} returning
     * {@code Optional<String>} — older variants returned {@code String}
     * directly. We tolerate both. */
    private static boolean allureHasCurrentTestCase(Object lifecycle) {
        try {
            Object res = lifecycle.getClass()
                    .getMethod("getCurrentTestCase")
                    .invoke(lifecycle);
            if (res == null) {
                return false;
            }
            if (res instanceof java.util.Optional) {
                return ((java.util.Optional<?>) res).isPresent();
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Close a previously-opened Allure step. */
    public static void endAllureStep(Object token, boolean passed) {
        if (token == null || !isAllureRuntimeAvailable()) {
            return;
        }
        try {
            Class<?> allure = Class.forName("io.qameta.allure.Allure",
                    true, AllureMirror.class.getClassLoader());
            Object lifecycle = allure.getMethod("getLifecycle").invoke(null);
            Class<?> statusCls = Class.forName(
                    "io.qameta.allure.model.Status",
                    true, AllureMirror.class.getClassLoader());
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object status = Enum.valueOf((Class<Enum>) statusCls.asSubclass(Enum.class),
                    passed ? "PASSED" : "FAILED");
            lifecycle.getClass().getMethod("updateStep",
                            String.class, java.util.function.Consumer.class)
                    .invoke(lifecycle, token, (java.util.function.Consumer<Object>) s -> {
                        try {
                            s.getClass().getMethod("setStatus", statusCls).invoke(s, status);
                        } catch (Throwable ignored) {
                            // best-effort
                        }
                    });
            lifecycle.getClass().getMethod("stopStep", String.class)
                    .invoke(lifecycle, token);
        } catch (Throwable t) {
            // fail-soft
        }
    }

    /** Clear all caches. Test-only — exposed so unit tests that mutate
     * classpath state (e.g. reflective fixtures) can force a re-probe. */
    static void clearCachesForTest() {
        METHOD_CACHE.clear();
        CLASS_CACHE.clear();
        allureRuntimeAvailable = null;
    }

    // ── Internal harvest computation ─────────────────────────────────

    private static Harvested computeMethod(Method method) {
        Harvested out = new Harvested();
        scan(method, out);
        // Class-level annotations get folded in so a method-level harvest
        // sees the union — callers don't need to call both harvests.
        Class<?> declaring = method.getDeclaringClass();
        scan(declaring, out);
        // Walk enclosing classes (Allure conventions allow @Feature on
        // outer class with nested @Nested test classes).
        Class<?> enclosing = declaring.getEnclosingClass();
        while (enclosing != null) {
            scan(enclosing, out);
            enclosing = enclosing.getEnclosingClass();
        }
        return out;
    }

    private static Harvested computeClass(Class<?> cls) {
        Harvested out = new Harvested();
        scan(cls, out);
        Class<?> enclosing = cls.getEnclosingClass();
        while (enclosing != null) {
            scan(enclosing, out);
            enclosing = enclosing.getEnclosingClass();
        }
        return out;
    }

    private static void scan(AnnotatedElement element, Harvested out) {
        for (Annotation a : element.getAnnotations()) {
            String fqcn = a.annotationType().getName();
            if (!fqcn.startsWith("io.qameta.allure.")
                    && !matchesSuffix(fqcn)) {
                continue;
            }
            // Allure annotations frequently carry a `value()` method
            // whose return type is the only piece we need (String, enum,
            // or String[]). Match by FQCN suffix; tolerate missing
            // methods (fail-soft).
            String suffix = lastSegment(fqcn);
            try {
                Object value = invokeValue(a);
                String text = value == null ? "" : String.valueOf(value);
                switch (suffix) {
                    case "Step":
                        // class-level @Step is rare but valid (template); store on metadata as note
                        if (!text.isEmpty()) {
                            out.classStepHint = text;
                        }
                        break;
                    case "Severity":
                        if (out.severity == null && !text.isEmpty()) {
                            out.severity = text.toLowerCase();
                        }
                        break;
                    case "Description":
                        if (out.description == null && !text.isEmpty()) {
                            out.description = text;
                        }
                        break;
                    case "Owner":
                        if (out.owner == null && !text.isEmpty()) {
                            out.owner = text;
                        }
                        break;
                    case "Title":
                        if (out.title == null && !text.isEmpty()) {
                            out.title = text;
                        }
                        break;
                    case "Feature":
                        addAll(out.features, value);
                        break;
                    case "Story":
                        addAll(out.stories, value);
                        break;
                    case "Epic":
                        addAll(out.epics, value);
                        break;
                    case "Tag":
                        addAll(out.tags, value);
                        break;
                    case "Issue":
                        addAll(out.issues, value);
                        break;
                    case "TmsLink":
                        addAll(out.tmsLinks, value);
                        break;
                    case "Link":
                        addAll(out.links, value);
                        break;
                    case "Label":
                        // @Label(name="...", value="...")
                        try {
                            Object name = a.annotationType().getMethod("name").invoke(a);
                            if (name != null && value != null) {
                                out.labels.put(String.valueOf(name), text);
                            }
                        } catch (Throwable ignored) {
                            // older Allure variants: fall back to value-only
                            if (!text.isEmpty()) {
                                out.labels.put("label", text);
                            }
                        }
                        break;
                    case "Parameter":
                        try {
                            Object name = a.annotationType().getMethod("value").invoke(a);
                            if (name != null) {
                                out.parameters.put(String.valueOf(name), "");
                            }
                        } catch (Throwable ignored) {
                            // fall-through
                        }
                        break;
                    case "Attachment":
                        // attachments are produced by aspect at runtime;
                        // metadata-only annotation has nothing actionable
                        // for us here. We DO scan for it so users see it
                        // in mirrored test reports as a tag-style hint.
                        out.labels.put("hasAttachment", "true");
                        break;
                    default:
                        // Unknown but allure-shaped — drop into labels as
                        // free-form metadata so it isn't silently lost.
                        if (!text.isEmpty()) {
                            out.labels.put(suffix.toLowerCase(), text);
                        }
                }
            } catch (Throwable ignored) {
                // any reflection error: skip this annotation, never throw
            }
        }
    }

    private static boolean matchesSuffix(String fqcn) {
        for (String s : ALLURE_SUFFIXES) {
            if (fqcn.endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    private static String lastSegment(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot < 0 ? fqcn : fqcn.substring(dot + 1);
    }

    private static Object invokeValue(Annotation a) {
        try {
            return a.annotationType().getMethod("value").invoke(a);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void addAll(List<String> target, Object value) {
        if (value == null) return;
        if (value instanceof String[]) {
            for (String s : (String[]) value) {
                if (s != null && !s.isEmpty()) {
                    target.add(s);
                }
            }
        } else {
            String s = String.valueOf(value);
            if (!s.isEmpty()) {
                target.add(s);
            }
        }
    }

    // ── Harvested data ───────────────────────────────────────────────

    /** Normalised view of Allure annotations harvested off a method/class. */
    public static final class Harvested {

        /** Sentinel for "nothing found" — immutable, safe to share. */
        public static final Harvested EMPTY = new Harvested();
        static {
            // make EMPTY truly immutable defensively
            EMPTY.features = Collections.emptyList();
            EMPTY.stories = Collections.emptyList();
            EMPTY.epics = Collections.emptyList();
            EMPTY.tags = Collections.emptyList();
            EMPTY.issues = Collections.emptyList();
            EMPTY.tmsLinks = Collections.emptyList();
            EMPTY.links = Collections.emptyList();
            EMPTY.labels = Collections.emptyMap();
            EMPTY.parameters = Collections.emptyMap();
        }

        public String title;
        public String description;
        public String severity;
        public String owner;
        public String classStepHint;
        public List<String> features = new ArrayList<>();
        public List<String> stories = new ArrayList<>();
        public List<String> epics = new ArrayList<>();
        public List<String> tags = new ArrayList<>();
        public List<String> issues = new ArrayList<>();
        public List<String> tmsLinks = new ArrayList<>();
        public List<String> links = new ArrayList<>();
        public Map<String, String> labels = new HashMap<>();
        public Map<String, String> parameters = new HashMap<>();

        public boolean isEmpty() {
            return title == null
                    && description == null
                    && severity == null
                    && owner == null
                    && features.isEmpty()
                    && stories.isEmpty()
                    && epics.isEmpty()
                    && tags.isEmpty()
                    && issues.isEmpty()
                    && tmsLinks.isEmpty()
                    && links.isEmpty()
                    && labels.isEmpty()
                    && parameters.isEmpty();
        }
    }
}
