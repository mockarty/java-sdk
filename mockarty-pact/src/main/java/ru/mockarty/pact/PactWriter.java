// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Serialises a {@link Pact} to the wire JSON shape mandated by the Pact
 * specification — V3 (legacy, flat matching-rule keys) or V4 (nested
 * matcher records).
 *
 * <p>The writer walks the request/response bodies recursively. Whenever it
 * encounters a {@link Matcher} it does two things:</p>
 * <ol>
 *   <li>Emit the {@code example()} value into the body in its place.</li>
 *   <li>Emit a parallel matching-rule entry under the right
 *       JSONPath-style key (V3) or nested object-tree (V4).</li>
 * </ol>
 *
 * <p>V4-only constructs cause {@link IllegalStateException} when serialised
 * under V3, never silent downgrades.</p>
 */
final class PactWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    static String write(Pact pact) {
        Map<String, Object> top = new LinkedHashMap<>();
        top.put("consumer", Map.of("name", pact.consumer()));
        top.put("provider", Map.of("name", pact.provider()));

        List<Map<String, Object>> wireInteractions = new ArrayList<>();
        for (Interaction i : pact.interactions()) {
            wireInteractions.add(writeInteraction(i, pact.specVersion()));
        }
        top.put("interactions", wireInteractions);

        // Plugins (V4 only).
        if (!pact.plugins().isEmpty()) {
            if (pact.specVersion() == SpecVersion.V3) {
                // Defence-in-depth — Consumer.build() should have caught
                // this, but the writer is the last line of defence.
                throw new IllegalStateException(
                        "Plugins declared on a V3 pact: " + pact.plugins());
            }
            List<Map<String, Object>> pluginEntries = new ArrayList<>();
            for (String name : pact.plugins()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("name", name);
                p.put("version", pact.pluginVersions().getOrDefault(name, "unknown"));
                Map<String, Object> cfg = pact.pluginConfigs().get(name);
                if (cfg != null && !cfg.isEmpty()) {
                    p.put("configuration", cfg);
                }
                pluginEntries.add(p);
            }
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("plugins", pluginEntries);
            top.put("metadata", buildMetadata(pact.specVersion(), meta));
        } else {
            top.put("metadata", buildMetadata(pact.specVersion(), null));
        }

        try {
            return MAPPER.writeValueAsString(top);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise pact contract", e);
        }
    }

    // ── Interactions ────────────────────────────────────────────────

    private static Map<String, Object> writeInteraction(Interaction i, SpecVersion v) {
        Map<String, Object> w = new LinkedHashMap<>();
        if (v == SpecVersion.V4) {
            // V4 carries an interaction type discriminator so a single
            // pact file can mix synchronous HTTP with async / message
            // interactions in future revisions.
            w.put("type", "Synchronous/HTTP");
        }
        w.put("description", i.description());

        if (v == SpecVersion.V3) {
            if (!i.providerStates().isEmpty()) {
                // V3 idiom: concat names with " AND ". Parameters are
                // V4-only — fail loud rather than discard them silently.
                StringBuilder sb = new StringBuilder();
                for (int idx = 0; idx < i.providerStates().size(); idx++) {
                    ProviderState ps = i.providerStates().get(idx);
                    if (!ps.params().isEmpty()) {
                        throw new IllegalStateException(
                                "Provider-state '" + ps.name() + "' has parameters "
                                        + ps.params() + " — parameters are V4-only");
                    }
                    if (idx > 0) sb.append(" AND ");
                    sb.append(ps.name());
                }
                w.put("providerState", sb.toString());
            }
        } else {
            if (!i.providerStates().isEmpty()) {
                List<Map<String, Object>> psList = new ArrayList<>();
                for (ProviderState ps : i.providerStates()) {
                    Map<String, Object> psMap = new LinkedHashMap<>();
                    psMap.put("name", ps.name());
                    if (!ps.params().isEmpty()) {
                        psMap.put("params", ps.params());
                    }
                    psList.add(psMap);
                }
                w.put("providerStates", psList);
            }
        }

        // Request
        w.put("request", writeRequest(i.request(), v));
        // Response
        w.put("response", writeResponse(i.response(), v));
        return w;
    }

    private static Map<String, Object> writeRequest(PactRequest r, SpecVersion v) {
        Map<String, Object> w = new LinkedHashMap<>();
        w.put("method", r.method());
        w.put("path", r.path());

        // Headers + query rewrite the structure, peeling matchers.
        RuleCollector rc = new RuleCollector();

        if (r.pathMatcher() instanceof Matcher m) {
            ensureSpecCompat(m, v);
            rc.recordPath(m, v);
        }

        if (!r.headers().isEmpty()) {
            Map<String, Object> hdrs = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : r.headers().entrySet()) {
                hdrs.put(e.getKey(), unwrapForHeader(e.getKey(), e.getValue(), rc, v));
            }
            w.put("headers", hdrs);
        }

        if (!r.query().isEmpty()) {
            // V3 represents query as a map of String -> List<String>;
            // V4 keeps the structured form. We pick the V3-compat shape
            // for both because pact-broker readers handle both.
            Map<String, List<Object>> q = new LinkedHashMap<>();
            for (Map.Entry<String, List<Object>> e : r.query().entrySet()) {
                List<Object> vals = new ArrayList<>();
                for (int idx = 0; idx < e.getValue().size(); idx++) {
                    Object raw = e.getValue().get(idx);
                    vals.add(unwrapForQuery(e.getKey(), idx, raw, rc, v));
                }
                q.put(e.getKey(), vals);
            }
            w.put("query", q);
        }

        Object body = writeBody(r.body(), rc, v);
        if (body != OMITTED) {
            w.put("body", body);
        }

        attachMatchingRules(w, rc, v);
        return w;
    }

    private static Map<String, Object> writeResponse(PactResponse r, SpecVersion v) {
        Map<String, Object> w = new LinkedHashMap<>();
        w.put("status", r.status());

        RuleCollector rc = new RuleCollector();

        if (!r.headers().isEmpty()) {
            Map<String, Object> hdrs = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : r.headers().entrySet()) {
                hdrs.put(e.getKey(), unwrapForHeader(e.getKey(), e.getValue(), rc, v));
            }
            w.put("headers", hdrs);
        }

        Object body = writeBody(r.body(), rc, v);
        if (body != OMITTED) {
            w.put("body", body);
        }

        attachMatchingRules(w, rc, v);
        return w;
    }

    // ── Body walk ────────────────────────────────────────────────────

    private static final Object OMITTED = new Object();

    private static Object writeBody(PactBody b, RuleCollector rc, SpecVersion v) {
        if (b instanceof PactBody.Empty) return OMITTED;
        if (b instanceof PactBody.Text t) return t.body();
        if (b instanceof PactBody.Binary bin) {
            if (v != SpecVersion.V4) {
                throw new IllegalStateException(
                        "Binary bodies require Pact V4; current spec is " + v);
            }
            // V4 attaches binary as base64-encoded body with the original
            // content type carried via headers. We emit a structured
            // representation so future plugin runners can recover the
            // raw bytes.
            Map<String, Object> wrap = new LinkedHashMap<>();
            wrap.put("content", Base64.getEncoder().encodeToString(bin.body()));
            wrap.put("contentType", bin.contentType());
            wrap.put("encoded", "base64");
            return wrap;
        }
        if (b instanceof PactBody.Json j) {
            return walk(j.root(), "$.body", rc, v);
        }
        return OMITTED;
    }

    /** Recursive walk: returns the example value with all matchers peeled
     * out, recording rules into {@code rc} as it descends. */
    private static Object walk(Object node, String path, RuleCollector rc, SpecVersion v) {
        if (node instanceof Matcher m) {
            ensureSpecCompat(m, v);
            rc.record(path, m, v);
            // The example side may itself contain nested matchers (e.g.
            // eachLike(Map.of("amount", like(100)))) — recurse.
            return walk(m.example(), pathForNested(path, m), rc, v);
        }
        if (node instanceof Map<?, ?> raw) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                String key = String.valueOf(e.getKey());
                Object child = walk(e.getValue(), path + "." + key, rc, v);
                out.put(key, child);
            }
            return out;
        }
        if (node instanceof List<?> raw) {
            List<Object> out = new ArrayList<>(raw.size());
            for (int idx = 0; idx < raw.size(); idx++) {
                Object child = walk(raw.get(idx), path + "[" + idx + "]", rc, v);
                out.add(child);
            }
            return out;
        }
        return node;
    }

    /** For matchers that apply to all elements of a container, the inner
     * path uses a wildcard so a single rule covers the whole shape. */
    private static String pathForNested(String path, Matcher m) {
        if (m instanceof Matcher.EachLike || m instanceof Matcher.MinType
                || m instanceof Matcher.MaxType || m instanceof Matcher.MinMaxType) {
            return path + "[*]";
        }
        if (m instanceof Matcher.EachKeyLike || m instanceof Matcher.EachKey
                || m instanceof Matcher.EachValue) {
            return path + ".*";
        }
        return path;
    }

    // ── Header / query unwrap ────────────────────────────────────────

    private static Object unwrapForHeader(String name, Object value, RuleCollector rc, SpecVersion v) {
        if (value instanceof Matcher m) {
            ensureSpecCompat(m, v);
            rc.record("$.header." + name, m, v);
            return walk(m.example(), "$.header." + name, rc, v);
        }
        return value;
    }

    private static Object unwrapForQuery(String name, int idx, Object value, RuleCollector rc, SpecVersion v) {
        if (value instanceof Matcher m) {
            ensureSpecCompat(m, v);
            rc.record("$.query." + name + "[" + idx + "]", m, v);
            return walk(m.example(), "$.query." + name + "[" + idx + "]", rc, v);
        }
        return value;
    }

    // ── Metadata ─────────────────────────────────────────────────────

    private static Map<String, Object> buildMetadata(SpecVersion v, Map<String, Object> extra) {
        Map<String, Object> meta = new LinkedHashMap<>();
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("version", v.wire());
        meta.put("pactSpecification", spec);
        meta.put("pact-jvm", Map.of("version", "mockarty-0.3.0"));
        if (extra != null) {
            meta.putAll(extra);
        }
        return meta;
    }

    // ── Rule attachment ──────────────────────────────────────────────

    private static void attachMatchingRules(
            Map<String, Object> wire, RuleCollector rc, SpecVersion v) {
        if (rc.isEmpty()) return;
        if (v == SpecVersion.V3) {
            wire.put("matchingRules", rc.toV3());
        } else {
            wire.put("matchingRules", rc.toV4());
        }
    }

    // ── V3/V4 compatibility gate ─────────────────────────────────────

    private static void ensureSpecCompat(Matcher m, SpecVersion v) {
        if (m.v4Only() && v == SpecVersion.V3) {
            throw new IllegalStateException(
                    "Matcher " + m.getClass().getSimpleName()
                            + " is V4-only — current SpecVersion is V3. "
                            + "Switch to specVersion(SpecVersion.V4) or use a V3-compatible matcher.");
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Rule collector
    // ────────────────────────────────────────────────────────────────

    /**
     * Accumulates rule entries during the body walk. Knows how to emit
     * both V3 flat layout (path → list of rule objects) and V4 nested
     * layout (path → {@code matchers} array with {@code combine} mode).
     */
    private static final class RuleCollector {

        // Insertion-ordered + sorted for diff stability across runs.
        private final Map<String, List<Map<String, Object>>> byPath = new TreeMap<>();

        void record(String path, Matcher m, SpecVersion v) {
            byPath.computeIfAbsent(path, k -> new ArrayList<>()).add(serialise(m, v));
        }

        void recordPath(Matcher m, SpecVersion v) {
            byPath.computeIfAbsent("$.path", k -> new ArrayList<>()).add(serialise(m, v));
        }

        boolean isEmpty() { return byPath.isEmpty(); }

        // ── V3 layout ───────────────────────────────────────────────

        Map<String, Object> toV3() {
            // V3 puts each path key directly under matchingRules with
            // {match: type, ...} entries.
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> e : byPath.entrySet()) {
                // V3 expects a single rule per path key (the first one wins).
                out.put(e.getKey(), e.getValue().get(0));
            }
            return out;
        }

        // ── V4 layout ───────────────────────────────────────────────

        Map<String, Object> toV4() {
            // V4 splits matchingRules by category: body, header, query, path.
            Map<String, Map<String, Object>> categories = new LinkedHashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> e : byPath.entrySet()) {
                String[] split = splitCategory(e.getKey());
                String category = split[0];
                String pathKey = split[1];
                categories.computeIfAbsent(category, k -> new LinkedHashMap<>())
                        .put(pathKey, wrapV4(e.getValue()));
            }
            return new LinkedHashMap<>(categories);
        }

        private static Map<String, Object> wrapV4(List<Map<String, Object>> rules) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("matchers", rules);
            out.put("combine", "AND");
            return out;
        }

        private static String[] splitCategory(String path) {
            if (path.startsWith("$.header.")) {
                return new String[] {"header", path.substring("$.header.".length())};
            }
            if (path.startsWith("$.query.")) {
                return new String[] {"query", path.substring("$.query.".length())};
            }
            if (path.equals("$.path")) {
                return new String[] {"path", ""};
            }
            // Anything else is body — strip the "$.body" prefix because the
            // Pact V4 body category re-roots on the body element.
            if (path.startsWith("$.body")) {
                String rest = path.substring("$.body".length());
                if (rest.isEmpty()) rest = "$";
                else rest = "$" + rest;
                return new String[] {"body", rest};
            }
            return new String[] {"body", path};
        }

        // ── Matcher → JSON ───────────────────────────────────────────

        private static Map<String, Object> serialise(Matcher m, SpecVersion v) {
            return MatcherJson.rule(m, v);
        }
    }
}
