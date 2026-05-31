// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.discovery;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import ru.mockarty.model.DiscoveryManifest;
import ru.mockarty.model.DiscoveryManifestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pure (no-I/O) assembly of a {@link DiscoveryManifest} from a JUnit
 * Platform {@link TestPlan}.
 *
 * <p>Walks the discovered test tree and emits one
 * {@link DiscoveryManifestCase} per leaf test identifier
 * ({@code TestIdentifier.isTest()}). Containers (classes, engines,
 * {@code @Nested} groups, parameterized-test parents) are descended into
 * but never emitted as cases.</p>
 *
 * <p>Field mapping ({@code JUnit Platform → discovery manifest}):</p>
 * <ul>
 *   <li><b>fullName</b> — {@code ClassName#methodName(types)} derived from
 *       the test's {@link MethodSource} when available (stable, matches the
 *       {@code /tcm/external-runs} identity and the Allure {@code fullName}
 *       convention); falls back to the unique-id when no source is
 *       attached.</li>
 *   <li><b>name</b> — the {@link TestIdentifier#getDisplayName() display
 *       name}.</li>
 *   <li><b>suite</b> — the nearest ancestor container's display name
 *       (typically the test class).</li>
 *   <li><b>sourceRef</b> — {@code ClassName.java} from the
 *       {@link ClassSource}/{@link MethodSource}, for "jump to source".</li>
 *   <li><b>labels</b> — JUnit {@link TestTag}s ({@code @Tag(...)}).</li>
 * </ul>
 *
 * <p>Kept separate from the listener so the tree-walk is unit-testable
 * without a live launcher.</p>
 */
public final class DiscoveryManifestAssembler {

    private DiscoveryManifestAssembler() {}

    /**
     * Build a manifest from a discovered {@link TestPlan}.
     *
     * @param plan         the discovered plan (collect-only phase).
     * @param source       the scope key (e.g. {@code junit5:auth-suite}).
     * @param pruneMissing whether to mark cases absent from this manifest
     *                     as orphaned.
     * @return a manifest with {@code framework=junit5} and one case per
     *         leaf test.
     */
    public static DiscoveryManifest assemble(TestPlan plan, String source, boolean pruneMissing) {
        DiscoveryManifest manifest = new DiscoveryManifest(source)
                .framework("junit5")
                .pruneMissing(pruneMissing);
        if (plan == null) {
            return manifest;
        }
        List<DiscoveryManifestCase> cases = new ArrayList<>();
        for (TestIdentifier root : plan.getRoots()) {
            collect(plan, root, null, cases);
        }
        manifest.cases(cases);
        return manifest;
    }

    private static void collect(TestPlan plan, TestIdentifier id,
                                TestIdentifier nearestContainer,
                                List<DiscoveryManifestCase> out) {
        if (id.isTest()) {
            out.add(toCase(id, nearestContainer));
            return;
        }
        // Container: descend. Track the nearest *named* container (the test
        // class / @Nested group) so leaf tests can attribute their suite.
        TestIdentifier container = isSuiteCandidate(id) ? id : nearestContainer;
        for (TestIdentifier child : plan.getChildren(id)) {
            collect(plan, child, container, out);
        }
    }

    /**
     * Convert one leaf {@link TestIdentifier} into a manifest case.
     * Package-visible for unit testing.
     */
    static DiscoveryManifestCase toCase(TestIdentifier id, TestIdentifier nearestContainer) {
        String displayName = id.getDisplayName();
        DiscoveryManifestCase c = new DiscoveryManifestCase()
                .fullName(fullName(id))
                .name(displayName);

        if (nearestContainer != null) {
            c.suite(nearestContainer.getDisplayName());
        }

        String sourceRef = sourceRef(id.getSource().orElse(null));
        if (sourceRef != null) {
            c.sourceRef(sourceRef);
        }

        Set<TestTag> tags = id.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<String> labels = new ArrayList<>(tags.size());
            for (TestTag t : tags) {
                labels.add(t.getName());
            }
            c.labels(labels);
        }
        return c;
    }

    /**
     * Deterministic identity for a test. Prefers {@code Class#method} from a
     * {@link MethodSource} (stable across runs and equal to the Allure
     * {@code fullName} the {@code /tcm/external-runs} path uses); falls back
     * to the JUnit unique-id when no method source is attached
     * (engine-specific dynamic tests).
     */
    static String fullName(TestIdentifier id) {
        TestSource src = id.getSource().orElse(null);
        if (src instanceof MethodSource) {
            MethodSource ms = (MethodSource) src;
            String params = ms.getMethodParameterTypes();
            String method = (params == null || params.isEmpty())
                    ? ms.getMethodName()
                    : ms.getMethodName() + "(" + params + ")";
            return ms.getClassName() + "#" + method;
        }
        if (src instanceof ClassSource) {
            return ((ClassSource) src).getClassName();
        }
        return id.getUniqueId();
    }

    /**
     * Best-effort {@code File.java} source reference from a class- or
     * method-source, derived from the simple class name. Returns null when
     * the source carries no class info.
     */
    static String sourceRef(TestSource src) {
        String className = null;
        if (src instanceof MethodSource) {
            className = ((MethodSource) src).getClassName();
        } else if (src instanceof ClassSource) {
            className = ((ClassSource) src).getClassName();
        }
        if (className == null || className.isEmpty()) {
            return null;
        }
        // Top-level file is named after the top-level (outer) class.
        int dollar = className.indexOf('$');
        String topLevel = dollar >= 0 ? className.substring(0, dollar) : className;
        int dot = topLevel.lastIndexOf('.');
        String simple = dot >= 0 ? topLevel.substring(dot + 1) : topLevel;
        return simple + ".java";
    }

    /**
     * A container worth recording as a leaf test's suite: anything backed by
     * a {@link ClassSource} (test class / {@code @Nested} group). The engine
     * and synthetic roots are skipped so the suite is the user-meaningful
     * class name.
     */
    private static boolean isSuiteCandidate(TestIdentifier id) {
        Optional<TestSource> src = id.getSource();
        return src.isPresent() && src.get() instanceof ClassSource;
    }
}
