// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.cucumber;

import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mockarty.junit5.allure.AllureLifecycle;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for the Cucumber-JVM plugin: drives a synthetic event
 * stream into {@link MockartyCucumberPlugin} and asserts the resulting
 * Allure artefacts.
 *
 * <p>Cucumber's plugin SPI accepts any class registered with the
 * {@link EventPublisher}; we don't need a Gherkin parser to verify the
 * shape — we instantiate the events directly. This isolates the test
 * from Cucumber's runtime configuration and feature-file scaffolding.</p>
 */
class MockartyCucumberPluginTest {

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

    @Test
    @DisplayName("Cucumber plugin emits one result.json per TestCase + one container.json per run")
    void emitOneResultPerCase() throws Exception {
        FakePublisher pub = new FakePublisher();
        MockartyCucumberPlugin plugin = new MockartyCucumberPlugin();
        plugin.setEventPublisher(pub);

        Instant t0 = Instant.now();
        pub.dispatch(new TestRunStarted(t0));

        TestCase tc = fakeTestCase("login.feature", "valid credentials");
        pub.dispatch(new TestCaseStarted(t0, tc));
        pub.dispatch(new TestCaseFinished(t0.plus(Duration.ofMillis(50)), tc,
                new Result(Status.PASSED, Duration.ofMillis(50), null)));

        TestCase tc2 = fakeTestCase("login.feature", "invalid credentials");
        pub.dispatch(new TestCaseStarted(t0, tc2));
        pub.dispatch(new TestCaseFinished(t0.plus(Duration.ofMillis(60)), tc2,
                new Result(Status.FAILED, Duration.ofMillis(60),
                        new AssertionError("bad credentials accepted"))));

        pub.dispatch(new TestRunFinished(t0.plus(Duration.ofMillis(120)),
                new Result(Status.PASSED, Duration.ofMillis(120), null)));

        long results = Files.list(tmp)
                .filter(p -> p.getFileName().toString().endsWith("-result.json"))
                .count();
        long containers = Files.list(tmp)
                .filter(p -> p.getFileName().toString().endsWith("-container.json"))
                .count();
        assertEquals(2, results, "two -result.json files expected (one per Cucumber TestCase)");
        assertEquals(1, containers, "one -container.json expected (one per Cucumber run)");
        assertTrue(containers >= 1);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    /** Lightweight stub of {@link EventPublisher} that dispatches events to handlers in registration order. */
    static final class FakePublisher implements EventPublisher {
        private final Map<Class<?>, EventHandler<?>> handlers = new HashMap<>();

        @Override
        public <T> void registerHandlerFor(Class<T> eventType, EventHandler<T> handler) {
            handlers.put(eventType, handler);
        }

        @Override
        public <T> void removeHandlerFor(Class<T> eventType, EventHandler<T> handler) {
            handlers.remove(eventType);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public void dispatch(Object event) {
            EventHandler h = handlers.get(event.getClass());
            if (h != null) {
                h.receive(event);
            }
        }
    }

    /**
     * Build a {@link TestCase} via reflection-proxy — Cucumber's {@code TestCase}
     * is an interface, no public constructor. We back the calls we use with
     * inline defaults.
     */
    static TestCase fakeTestCase(String featurePath, String name) {
        URI uri = URI.create("file:///" + featurePath);
        UUID id = UUID.randomUUID();
        return (TestCase) Proxy.newProxyInstance(
                MockartyCucumberPluginTest.class.getClassLoader(),
                new Class[]{TestCase.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        switch (method.getName()) {
                            case "getUri":      return uri;
                            case "getName":     return name;
                            case "getTags":     return List.of("@smoke");
                            case "getKeyword":  return "Scenario";
                            case "getId":       return id;
                            case "getLine":     return 1;
                            case "getLocation": return uri.toString();
                            case "getScenarioDesignation": return featurePath + ":" + name;
                            case "getTestSteps":           return java.util.Collections.emptyList();
                            default:
                                // Common Object methods + getters returning Object.
                                if (method.getReturnType() == String.class) return "";
                                if (method.getReturnType() == Integer.TYPE) return 0;
                                if (method.getReturnType() == Long.TYPE) return 0L;
                                if (method.getReturnType() == Boolean.TYPE) return false;
                                return null;
                        }
                    }
                });
    }
}
