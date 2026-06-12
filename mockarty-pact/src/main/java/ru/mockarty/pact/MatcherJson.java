// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for serialising a {@link Matcher} into its
 * on-the-wire {@code matchingRules} entry ({@code {"match": "..."}} plus any
 * extra fields). Shared by {@link PactWriter} (HTTP bodies/headers/query)
 * and {@code ru.mockarty.pact.message.MessagePact} (message content) so the
 * matcher catalogue serialises identically everywhere — adding a matcher
 * means touching exactly one switch.
 */
public final class MatcherJson {

    private MatcherJson() {
        // Static utility — no instances.
    }

    /** Serialise {@code m} into a single matcher-rule object for spec {@code v}. */
    public static Map<String, Object> rule(Matcher m, SpecVersion v) {
        Map<String, Object> r = new LinkedHashMap<>();
        if (m instanceof Matcher.Like) {
            r.put("match", "type");
        } else if (m instanceof Matcher.Term t) {
            r.put("match", "regex");
            r.put("regex", t.regex());
        } else if (m instanceof Matcher.EachLike el) {
            r.put("match", "type");
            r.put("min", el.min());
        } else if (m instanceof Matcher.EachKeyLike) {
            r.put("match", "type");
        } else if (m instanceof Matcher.Regex re) {
            r.put("match", "regex");
            r.put("regex", re.pattern());
        } else if (m instanceof Matcher.Integer) {
            r.put("match", "integer");
        } else if (m instanceof Matcher.Decimal) {
            r.put("match", "decimal");
        } else if (m instanceof Matcher.Bool) {
            r.put("match", "boolean");
        }
        // V4-only matchers
        else if (m instanceof Matcher.MatchType) {
            r.put("match", "type");
        } else if (m instanceof Matcher.MinType mt) {
            r.put("match", "type");
            r.put("min", mt.min());
        } else if (m instanceof Matcher.MaxType mx) {
            r.put("match", "type");
            r.put("max", mx.max());
        } else if (m instanceof Matcher.MinMaxType mm) {
            r.put("match", "type");
            r.put("min", mm.min());
            r.put("max", mm.max());
        } else if (m instanceof Matcher.ArrayContains ac) {
            r.put("match", "arrayContains");
            r.put("variants", ac.variants());
        } else if (m instanceof Matcher.Equality) {
            r.put("match", "equality");
        } else if (m instanceof Matcher.EachKey ek) {
            r.put("match", "eachKey");
            List<Map<String, Object>> nested = new ArrayList<>();
            for (Matcher inner : ek.rules()) nested.add(rule(inner, v));
            r.put("rules", nested);
        } else if (m instanceof Matcher.EachValue ev) {
            r.put("match", "eachValue");
            List<Map<String, Object>> nested = new ArrayList<>();
            for (Matcher inner : ev.rules()) nested.add(rule(inner, v));
            r.put("rules", nested);
        } else if (m instanceof Matcher.JsonPath jp) {
            r.put("match", "jsonPath");
            r.put("path", jp.path());
            r.put("rule", rule(jp.inner(), v));
        } else if (m instanceof Matcher.XmlPath xp) {
            r.put("match", "xmlPath");
            r.put("path", xp.path());
            r.put("rule", rule(xp.inner(), v));
        } else if (m instanceof Matcher.NotNull) {
            r.put("match", "notNull");
        } else if (m instanceof Matcher.Include inc) {
            r.put("match", "include");
            r.put("value", inc.substring());
        } else if (m instanceof Matcher.ContentType ct) {
            r.put("match", "contentType");
            r.put("value", ct.contentType());
        } else if (m instanceof Matcher.AtLeastOne) {
            r.put("match", "atLeastOne");
        } else if (m instanceof Matcher.Format f) {
            r.put("match", f.matchName());
            if (f.regex() != null) {
                r.put("regex", f.regex());
            }
        } else {
            throw new IllegalStateException(
                    "Unhandled matcher variant: " + m.getClass().getName());
        }
        return r;
    }
}
