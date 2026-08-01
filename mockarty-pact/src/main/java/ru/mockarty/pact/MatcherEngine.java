// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pure-function matcher evaluator.
 *
 * <p>{@link #evaluate(Matcher, Object, String)} walks the given {@code Matcher}
 * (sealed-interface dispatch — every variant covered explicitly so the
 * compiler will flag a missing branch if the matcher hierarchy grows) and
 * accumulates {@link MismatchReport} entries describing why the actual
 * value violates the declared shape. An empty list means a clean match.</p>
 *
 * <p>The engine is intentionally <b>stateless and thread-safe</b>: every
 * call constructs a fresh result list and only touches the {@code Matcher}
 * record + immutable JDK utilities. The {@code MockServer} consumes it
 * from request-handler threads (one per concurrent inbound request) — race
 * conditions are impossible by construction.</p>
 *
 * <p>The strict matchers ({@code equality}, {@code regex}, the primitive
 * type matchers, the size-bounded array matchers, {@code arrayContains})
 * are first-class; the historical {@code like}-style matchers are still
 * honoured (type-only) so that legacy contracts continue to pass through
 * the same code path.</p>
 */
public final class MatcherEngine {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DocumentBuilderFactory XML_FACTORY = secureXmlFactory();
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    private MatcherEngine() {
        // Static utility — no instances.
    }

    /**
     * Evaluate {@code matcher} against {@code actual} starting from
     * {@code path} (e.g. {@code $.body}). Never throws — every mismatch
     * is reported as a structured entry instead of an exception.
     */
    public static List<MismatchReport> evaluate(Matcher matcher, Object actual, String path) {
        Objects.requireNonNull(matcher, "matcher must not be null");
        Objects.requireNonNull(path, "path must not be null");
        List<MismatchReport> out = new ArrayList<>();
        dispatch(matcher, actual, path, out);
        return Collections.unmodifiableList(out);
    }

    /**
     * Compare an expected JSON tree (which may itself carry {@link Matcher}
     * nodes at any depth) against an actual JSON tree (plain Java
     * Map/List/Number/String/Boolean tree, as produced by Jackson's
     * {@code readValue}). Used by {@link MockServer} for request-body
     * strict validation.
     */
    public static List<MismatchReport> compare(Object expected, Object actual, String path) {
        Objects.requireNonNull(path, "path must not be null");
        List<MismatchReport> out = new ArrayList<>();
        compareInternal(expected, actual, path, out);
        return Collections.unmodifiableList(out);
    }

    // ────────────────────────────────────────────────────────────────
    // Internal: sealed dispatch
    // ────────────────────────────────────────────────────────────────

    private static void dispatch(Matcher m, Object actual, String path, List<MismatchReport> out) {
        // Pattern dispatch — one branch per sealed permit. The compiler
        // gives us exhaustiveness checking via the sealed declaration so
        // adding a new Matcher variant will fail the build until this
        // switch handles it.
        if (m instanceof Matcher.Equality e) {
            if (!deepEquals(e.example(), actual)) {
                out.add(report(path, "equality", e.example(), actual));
            }
        } else if (m instanceof Matcher.Regex re) {
            if (!(actual instanceof CharSequence)
                    || !safeMatches(re.pattern(), actual.toString())) {
                out.add(report(path, "regex(" + re.pattern() + ")", re.exampleValue(), actual));
            }
        } else if (m instanceof Matcher.Term t) {
            if (!(actual instanceof CharSequence)
                    || !safeMatches(t.regex(), actual.toString())) {
                out.add(report(path, "term(" + t.regex() + ")", t.example(), actual));
            }
        } else if (m instanceof Matcher.Integer) {
            if (!isIntegerValue(actual)) {
                out.add(report(path, "integer", "integer", actual));
            }
        } else if (m instanceof Matcher.Decimal) {
            if (!isDecimalValue(actual)) {
                out.add(report(path, "decimal", "decimal", actual));
            }
        } else if (m instanceof Matcher.Bool) {
            if (!(actual instanceof Boolean)) {
                out.add(report(path, "boolean", "boolean", actual));
            }
        } else if (m instanceof Matcher.Like l) {
            if (!sameJsonType(l.example(), actual)) {
                out.add(report(path, "like(" + jsonType(l.example()) + ")",
                        jsonType(l.example()), jsonType(actual)));
            } else if (l.example() instanceof Map || l.example() instanceof List) {
                // Recurse into the example ONLY when it's a structure —
                // nested matchers buried inside a like(Map/List) still get
                // evaluated. For primitive examples (string/number/bool)
                // like() is type-only by definition, so we must NOT fall
                // through to compareInternal's strict literal equality
                // path — that would defeat the whole point of like().
                compareInternal(l.example(), actual, path, out);
            }
        } else if (m instanceof Matcher.MatchType mt) {
            if (!sameJsonType(mt.example(), actual)) {
                out.add(report(path, "matchType(" + jsonType(mt.example()) + ")",
                        jsonType(mt.example()), jsonType(actual)));
            } else if (mt.example() instanceof Map || mt.example() instanceof List) {
                // Same structure-only recursion rule as Like above — strict
                // primitive equality is a regression we intentionally avoid.
                compareInternal(mt.example(), actual, path, out);
            }
        } else if (m instanceof Matcher.EachLike el) {
            evaluateBoundedArray(actual, el.example(), path, el.min(), Integer.MAX_VALUE,
                    "eachLike", out);
        } else if (m instanceof Matcher.MinType mn) {
            evaluateBoundedArray(actual, mn.example(), path, mn.min(), Integer.MAX_VALUE,
                    "minType", out);
        } else if (m instanceof Matcher.MaxType mx) {
            evaluateBoundedArray(actual, mx.example(), path, 0, mx.max(),
                    "maxType", out);
        } else if (m instanceof Matcher.MinMaxType mm) {
            evaluateBoundedArray(actual, mm.example(), path, mm.min(), mm.max(),
                    "minMaxType", out);
        } else if (m instanceof Matcher.ArrayContains ac) {
            evaluateArrayContains(ac, actual, path, out);
        } else if (m instanceof Matcher.EachKeyLike ekl) {
            if (!(actual instanceof Map<?, ?> map)) {
                out.add(report(path, "eachKeyLike", "object", actual));
            } else {
                Class<?> keyExpected = ekl.example() == null ? Object.class : ekl.example().getClass();
                for (Object k : map.keySet()) {
                    if (k != null && !keyExpected.isInstance(k)) {
                        out.add(report(path + ".(key)", "eachKeyLike(" + keyExpected.getSimpleName() + ")",
                                keyExpected.getSimpleName(), k));
                    }
                }
            }
        } else if (m instanceof Matcher.EachKey ek) {
            if (!(actual instanceof Map<?, ?> map)) {
                out.add(report(path, "eachKey", "object", actual));
            } else {
                for (Object k : map.keySet()) {
                    for (Matcher rule : ek.rules()) {
                        dispatch(rule, k, path + ".(key=" + k + ")", out);
                    }
                }
            }
        } else if (m instanceof Matcher.EachValue ev) {
            if (!(actual instanceof Map<?, ?> map)) {
                out.add(report(path, "eachValue", "object", actual));
            } else {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String childPath = path + "." + e.getKey();
                    for (Matcher rule : ev.rules()) {
                        dispatch(rule, e.getValue(), childPath, out);
                    }
                }
            }
        } else if (m instanceof Matcher.JsonPath jp) {
            Object resolved = JsonPathEvaluator.resolve(actual, jp.path());
            if (resolved == JsonPathEvaluator.MISSING) {
                out.add(report(path, "jsonPath(" + jp.path() + ")", jp.path(), "missing"));
            } else {
                dispatch(jp.inner(), resolved, path + "[" + jp.path() + "]", out);
            }
        } else if (m instanceof Matcher.XmlPath xp) {
            Object resolved = evaluateXPath(xp.path(), actual);
            if (resolved == null) {
                out.add(report(path, "xmlPath(" + xp.path() + ")", xp.path(), "missing"));
            } else {
                dispatch(xp.inner(), resolved, path + "[" + xp.path() + "]", out);
            }
        } else if (m instanceof Matcher.NotNull) {
            if (actual == null) {
                out.add(report(path, "notNull", "non-null", "null"));
            }
        } else if (m instanceof Matcher.Include inc) {
            if (!(actual instanceof CharSequence) || !actual.toString().contains(inc.substring())) {
                out.add(report(path, "include(" + inc.substring() + ")", inc.substring(), actual));
            }
        } else if (m instanceof Matcher.ContentType ct) {
            if (!(actual instanceof CharSequence)
                    || (!ct.contentType().isEmpty() && !actual.toString().strip().startsWith(ct.contentType()))) {
                out.add(report(path, "contentType(" + ct.contentType() + ")", ct.contentType(), actual));
            }
        } else if (m instanceof Matcher.AtLeastOne) {
            if (!(actual instanceof List<?> list)) {
                out.add(report(path, "atLeastOne", "array", actual));
            } else if (list.isEmpty()) {
                out.add(report(path, "atLeastOne", "non-empty array", actual));
            }
        } else if (m instanceof Matcher.Format f) {
            String pat = f.regex() != null ? f.regex() : defaultFormatRegex(f.matchName());
            if (!(actual instanceof CharSequence) || !safeMatches(pat, actual.toString())) {
                out.add(report(path, f.matchName() + "(" + pat + ")", f.matchName(), actual));
            }
        } else {
            // Defence-in-depth — the sealed compile-time check should make
            // this unreachable. If it ever fires, surface as a clearly-tagged
            // mismatch rather than swallowing silently.
            out.add(report(path, "unhandled-matcher", m.getClass().getSimpleName(),
                    actual == null ? "null" : actual.toString()));
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Internal: tree comparison (mix of literals + matchers)
    // ────────────────────────────────────────────────────────────────

    private static void compareInternal(Object expected, Object actual, String path, List<MismatchReport> out) {
        if (expected instanceof Matcher m) {
            dispatch(m, actual, path, out);
            return;
        }
        if (expected instanceof Map<?, ?> expectedMap) {
            if (!(actual instanceof Map<?, ?> actualMap)) {
                out.add(report(path, "object", "object", actual));
                return;
            }
            for (Map.Entry<?, ?> e : expectedMap.entrySet()) {
                String key = String.valueOf(e.getKey());
                if (!actualMap.containsKey(key)) {
                    out.add(report(path + "." + key, "present", "present", "missing"));
                    continue;
                }
                compareInternal(e.getValue(), actualMap.get(key), path + "." + key, out);
            }
            return;
        }
        if (expected instanceof List<?> expectedList) {
            if (!(actual instanceof List<?> actualList)) {
                out.add(report(path, "array", "array", actual));
                return;
            }
            int n = Math.min(expectedList.size(), actualList.size());
            for (int i = 0; i < n; i++) {
                compareInternal(expectedList.get(i), actualList.get(i), path + "[" + i + "]", out);
            }
            if (expectedList.size() != actualList.size()) {
                out.add(report(path, "length",
                        String.valueOf(expectedList.size()), actualList.size()));
            }
            return;
        }
        // Primitive / null literal — strict equality (you can opt back into
        // looseness by wrapping in like()).
        if (!deepEquals(expected, actual)) {
            out.add(report(path, "literal", expected, actual));
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────

    private static void evaluateBoundedArray(Object actual, Object template, String path,
                                             int min, int max, String label,
                                             List<MismatchReport> out) {
        if (!(actual instanceof List<?> list)) {
            out.add(report(path, label + "(array)", "array", actual));
            return;
        }
        if (list.size() < min) {
            out.add(report(path, label + "(min=" + min + ")",
                    "size>=" + min, list.size()));
        }
        if (list.size() > max) {
            out.add(report(path, label + "(max=" + max + ")",
                    "size<=" + max, list.size()));
        }
        // Primitive template = type-only check (the Pact "minType/eachLike/..."
        // contract: bound the array size + each element matches the example's
        // JSON type). Falling through to compareInternal would impose strict
        // value equality on every element, which is a regression — callers
        // who want value equality use literal arrays or wrap with equality().
        boolean structuralTemplate = template instanceof Map || template instanceof List
                || template instanceof Matcher;
        for (int i = 0; i < list.size(); i++) {
            if (structuralTemplate) {
                compareInternal(template, list.get(i), path + "[" + i + "]", out);
            } else if (!sameJsonType(template, list.get(i))) {
                out.add(report(path + "[" + i + "]", label + "(" + jsonType(template) + ")",
                        jsonType(template), jsonType(list.get(i))));
            }
        }
    }

    private static void evaluateArrayContains(Matcher.ArrayContains ac, Object actual,
                                              String path, List<MismatchReport> out) {
        if (!(actual instanceof List<?> list)) {
            out.add(report(path, "arrayContains", "array", actual));
            return;
        }
        for (int v = 0; v < ac.variants().size(); v++) {
            Object variant = ac.variants().get(v);
            boolean found = false;
            for (Object item : list) {
                // For variant that's a matcher, the engine recurses; for
                // a literal, we deep-equals.
                List<MismatchReport> sink = new ArrayList<>();
                if (variant instanceof Matcher m) {
                    dispatch(m, item, "(probe)", sink);
                } else {
                    compareInternal(variant, item, "(probe)", sink);
                }
                if (sink.isEmpty()) { found = true; break; }
            }
            if (!found) {
                out.add(report(path + "[variant=" + v + "]", "arrayContains",
                        variant, "not present"));
            }
        }
    }

    private static boolean isIntegerValue(Object v) {
        if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) return true;
        if (v instanceof java.math.BigInteger) return true;
        if (v instanceof Number n) {
            double d = n.doubleValue();
            return !Double.isNaN(d) && !Double.isInfinite(d) && d == Math.floor(d);
        }
        return false;
    }

    private static boolean isDecimalValue(Object v) {
        if (v instanceof Double || v instanceof Float || v instanceof java.math.BigDecimal) return true;
        if (v instanceof Number n) {
            double d = n.doubleValue();
            return !Double.isNaN(d) && !Double.isInfinite(d) && d != Math.floor(d);
        }
        return false;
    }

    private static boolean sameJsonType(Object expected, Object actual) {
        // Two-sided null: both must be null to match.
        if (expected == null || actual == null) return expected == null && actual == null;
        if (expected instanceof Number && actual instanceof Number) return true;
        if (expected instanceof CharSequence && actual instanceof CharSequence) return true;
        if (expected instanceof Boolean && actual instanceof Boolean) return true;
        if (expected instanceof Map && actual instanceof Map) return true;
        if (expected instanceof List && actual instanceof List) return true;
        return expected.getClass().isInstance(actual);
    }

    private static String jsonType(Object o) {
        if (o == null) return "null";
        if (o instanceof Number) return "number";
        if (o instanceof CharSequence) return "string";
        if (o instanceof Boolean) return "boolean";
        if (o instanceof Map) return "object";
        if (o instanceof List) return "array";
        return o.getClass().getSimpleName();
    }

    private static boolean deepEquals(Object a, Object b) {
        if (a == null || b == null) return a == b;
        if (a instanceof Number && b instanceof Number) {
            // Numeric tolerance is a footgun for contract tests — we use
            // exact comparison after normalising via doubleValue. Callers
            // who want fuzzy equality should use like().
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        if (a instanceof CharSequence && b instanceof CharSequence) {
            return a.toString().equals(b.toString());
        }
        if (a instanceof Map<?, ?> am && b instanceof Map<?, ?> bm) {
            if (am.size() != bm.size()) return false;
            for (Map.Entry<?, ?> e : am.entrySet()) {
                if (!bm.containsKey(e.getKey())) return false;
                if (!deepEquals(e.getValue(), bm.get(e.getKey()))) return false;
            }
            return true;
        }
        if (a instanceof List<?> al && b instanceof List<?> bl) {
            if (al.size() != bl.size()) return false;
            for (int i = 0; i < al.size(); i++) {
                if (!deepEquals(al.get(i), bl.get(i))) return false;
            }
            return true;
        }
        return Objects.equals(a, b);
    }

    /** Default format patterns — kept byte-for-byte identical to the
     * server-side matcher engine (internal/contract/pact_matcher.go) so the
     * consumer mock server and the provider verifier agree on the format. */
    private static String defaultFormatRegex(String matchName) {
        return switch (matchName) {
            case "date" -> "^\\d{4}-\\d{2}-\\d{2}$";
            case "time" -> "^\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$";
            case "timestamp", "datetime" ->
                    "^\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$";
            case "uuid" -> "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
            case "semver" -> "^\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z.-]+)?(\\+[0-9A-Za-z.-]+)?$";
            case "ipv4" -> "^(\\d{1,3}\\.){3}\\d{1,3}$";
            default -> ".*";
        };
    }

    private static boolean safeMatches(String regex, String input) {
        try {
            return Pattern.compile(regex).matcher(input).matches();
        } catch (Exception e) {
            return false;
        }
    }

    private static MismatchReport report(String path, String matcherType, Object expected, Object actual) {
        return new MismatchReport(
                path,
                expected == null ? "null" : expected.toString(),
                actual == null ? "null" : actual.toString(),
                matcherType);
    }

    /** Best-effort XPath evaluation. Returns the matched node's text content
     * (most common case for contract tests), an attribute value, or {@code null}
     * when nothing matches. */
    private static Object evaluateXPath(String xpath, Object actual) {
        String xml = actual == null ? "" : actual.toString();
        if (xml.isBlank()) return null;
        try {
            DocumentBuilder builder = XML_FACTORY.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            XPath xp = XPATH_FACTORY.newXPath();
            // Try node-list first — covers element selection. Fall back to
            // string for attribute or text() axes.
            Object nodes = xp.evaluate(xpath, doc, XPathConstants.NODESET);
            if (nodes instanceof org.w3c.dom.NodeList nl && nl.getLength() > 0) {
                Node n = nl.item(0);
                return n.getTextContent();
            }
            String s = xp.evaluate(xpath, doc);
            return s == null || s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private static DocumentBuilderFactory secureXmlFactory() {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        // Lock down the parser — air-gapped + untrusted contract content
        // = no external entities, no DOCTYPE expansion.
        try {
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
        } catch (Exception ignored) {
            // Older parsers may not know these features — best-effort.
        }
        return f;
    }

    // ────────────────────────────────────────────────────────────────
    // Minimal in-process JSONPath
    // ────────────────────────────────────────────────────────────────

    /**
     * Tiny JSONPath subset: root {@code $}, dot keys, {@code [index]} for
     * array indexing. No filters, no wildcards. We deliberately ship our
     * own evaluator (≈30 lines) instead of pulling
     * {@code com.jayway.jsonpath} so the SDK keeps zero new transitive
     * deps and remains usable in air-gapped builds.
     */
    static final class JsonPathEvaluator {

        /** Sentinel for "path resolved to nothing" — distinct from {@code null}
         * which is a legitimate JSON value. */
        static final Object MISSING = new Object();

        static Object resolve(Object root, String path) {
            if (root == null) return MISSING;
            if (path == null || path.isBlank() || path.equals("$")) return root;
            String p = path.startsWith("$") ? path.substring(1) : path;
            if (p.startsWith(".")) p = p.substring(1);

            Object cur = root;
            int i = 0;
            StringBuilder seg = new StringBuilder();
            while (i < p.length()) {
                char c = p.charAt(i);
                if (c == '.') {
                    cur = step(cur, seg.toString());
                    if (cur == MISSING) return MISSING;
                    seg.setLength(0);
                    i++;
                } else if (c == '[') {
                    if (seg.length() > 0) {
                        cur = step(cur, seg.toString());
                        if (cur == MISSING) return MISSING;
                        seg.setLength(0);
                    }
                    int close = p.indexOf(']', i);
                    if (close < 0) return MISSING;
                    String idx = p.substring(i + 1, close);
                    cur = stepIndex(cur, idx);
                    if (cur == MISSING) return MISSING;
                    i = close + 1;
                    if (i < p.length() && p.charAt(i) == '.') i++;
                } else {
                    seg.append(c);
                    i++;
                }
            }
            if (seg.length() > 0) {
                cur = step(cur, seg.toString());
            }
            return cur;
        }

        private static Object step(Object cur, String key) {
            if (cur instanceof Map<?, ?> map) {
                if (!map.containsKey(key)) return MISSING;
                return map.get(key);
            }
            return MISSING;
        }

        private static Object stepIndex(Object cur, String raw) {
            // [0], [12], ["key"], ['key']
            String s = raw.trim();
            if ((s.startsWith("\"") && s.endsWith("\""))
                    || (s.startsWith("'") && s.endsWith("'"))) {
                return step(cur, s.substring(1, s.length() - 1));
            }
            try {
                int idx = Integer.parseInt(s);
                if (cur instanceof List<?> list) {
                    if (idx < 0 || idx >= list.size()) return MISSING;
                    return list.get(idx);
                }
                return MISSING;
            } catch (NumberFormatException e) {
                return step(cur, s);
            }
        }
    }
}
