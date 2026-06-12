// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour tests for the format / scalar matcher catalogue added for
 * parity with the Mockarty server matcher engine: notNull, include,
 * contentType, atLeastOne, date, time, dateTime(+timestamp), uuid, semver,
 * ipv4. Each asserts accept (clean) + reject (mismatch) through the engine,
 * plus the emitted wire shape via the writer.
 */
class FormatMatchersTest {

    private static boolean ok(Matcher m, Object actual) {
        return MatcherEngine.evaluate(m, actual, "$").isEmpty();
    }

    @Test
    @DisplayName("format matchers accept valid, reject malformed")
    void formatMatchers() {
        assertTrue(ok(Matchers.date("2026-06-12"), "2026-06-12"));
        assertFalse(ok(Matchers.date("2026-06-12"), "12/06/2026"));

        assertTrue(ok(Matchers.time("10:30:00"), "10:30:00"));
        assertFalse(ok(Matchers.time("10:30:00"), "10:30"));

        assertTrue(ok(Matchers.dateTime("2026-06-12T10:30:00Z"), "2026-06-12T10:30:00Z"));
        assertFalse(ok(Matchers.dateTime("2026-06-12T10:30:00Z"), "nope"));
        assertTrue(ok(Matchers.timestamp("2026-06-12T10:30:00Z"), "2026-06-12T10:30:00+02:00"));

        assertTrue(ok(Matchers.uuid("550e8400-e29b-41d4-a716-446655440000"),
                "550e8400-e29b-41d4-a716-446655440000"));
        assertFalse(ok(Matchers.uuid("550e8400-e29b-41d4-a716-446655440000"), "not-a-uuid"));

        assertTrue(ok(Matchers.semver("1.2.3"), "1.2.3-rc.1"));
        assertFalse(ok(Matchers.semver("1.2.3"), "1.2"));

        assertTrue(ok(Matchers.ipv4("192.168.0.1"), "10.0.0.1"));
        assertFalse(ok(Matchers.ipv4("192.168.0.1"), "::1"));
    }

    @Test
    @DisplayName("date regex override replaces the default pattern")
    void dateRegexOverride() {
        Matcher m = Matchers.date("31/12/2026", "^\\d{2}/\\d{2}/\\d{4}$");
        assertTrue(ok(m, "31/12/2026"));
        assertFalse(ok(m, "2026-12-31"));
    }

    @Test
    @DisplayName("scalar matchers: notNull / include / contentType / atLeastOne")
    void scalarMatchers() {
        assertTrue(ok(Matchers.notNull("x"), "anything"));
        assertFalse(ok(Matchers.notNull("x"), null));

        assertTrue(ok(Matchers.include("@"), "user@host"));
        assertFalse(ok(Matchers.include("@"), "userhost"));

        assertTrue(ok(Matchers.contentType("application/json"), "application/json; charset=utf-8"));
        assertFalse(ok(Matchers.contentType("application/json"), "text/plain"));

        assertTrue(ok(Matchers.atLeastOne(Map.of("id", 1)), List.of(Map.of("id", 7))));
        assertFalse(ok(Matchers.atLeastOne(Map.of("id", 1)), List.of()));
        assertFalse(ok(Matchers.atLeastOne(Map.of("id", 1)), "not-an-array"));
    }

    @Test
    @DisplayName("writer emits the correct match tags (V4)")
    void writerShapes() {
        Pact pact = Consumer.named("c")
                .withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it
                        .uponReceiving("format body")
                        .withRequest("POST", "/x")
                        .willRespondWith(200)
                        .withJsonBody(Map.of(
                                "u", Matchers.uuid("550e8400-e29b-41d4-a716-446655440000"),
                                "n", Matchers.notNull("x"),
                                "c", Matchers.contentType("application/json"),
                                "a", Matchers.atLeastOne(1))))
                .build();
        String json = pact.toJson();
        assertTrue(json.contains("\"uuid\""), json);
        assertTrue(json.contains("\"notNull\""), json);
        assertTrue(json.contains("\"contentType\""), json);
        assertTrue(json.contains("\"atLeastOne\""), json);
    }
}
