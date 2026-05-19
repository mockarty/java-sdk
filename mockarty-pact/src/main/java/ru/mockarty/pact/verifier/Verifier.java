// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mockarty.pact.broker.BrokerClient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/**
 * Pact provider verifier — replays consumer-published pact
 * interactions against a real provider HTTP service and reports
 * verification results.
 *
 * <p>Mirrors the Go SDK {@code pact.Verifier} and Python SDK
 * {@code mockarty.pact.Verifier} so cross-language CI surfaces a
 * single mental model.</p>
 *
 * <pre>{@code
 * Verifier v = Verifier.builder()
 *     .providerUrl("http://localhost:8080")
 *     .providerName("OrderAPI")
 *     .providerVersion("1.2.3")
 *     .broker(broker)
 *     .stateHandler("order 42 exists", (state, params) -> seedOrder(42))
 *     .build();
 *
 * VerificationResult res = v.verifyFromBroker("OrderClient", "OrderAPI", "latest");
 * if (!res.ok()) throw new AssertionError(res.summary());
 * v.publishResults("OrderClient", "OrderAPI", "1.0", res);
 * }</pre>
 */
public final class Verifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Callback invoked once per provider-state before request replay. */
    @FunctionalInterface
    public interface StateHandler {
        void setUp(String state, Map<String, Object> params) throws Exception;
    }

    /** Rewrites an outgoing HTTP request before it hits the provider. */
    @FunctionalInterface
    public interface RequestFilter {
        void apply(VerifierRequest request) throws Exception;
    }

    /**
     * Producer callback for message-pact verification.
     * Returns the bytes the provider would publish for this
     * interaction, plus any metadata (Kafka headers, AMQP properties).
     */
    @FunctionalInterface
    public interface MessageProducer {
        MessagePayload produce(String description,
                               java.util.List<java.util.Map<String, Object>> states)
            throws Exception;
    }

    /** Bytes + metadata returned by a {@link MessageProducer}. */
    public record MessagePayload(byte[] body, java.util.Map<String, String> metadata) {
        public MessagePayload {
            if (body == null) body = new byte[0];
            if (metadata == null) metadata = java.util.Map.of();
        }
    }

    private final String providerUrl;
    private final String providerName;
    private final String providerVersion;
    private final String providerBranch;
    private final BrokerClient broker;
    private final Map<String, StateHandler> stateHandlers;
    private final String stateSetupUrl;
    private final RequestFilter requestFilter;
    private final Map<String, MessageProducer> messageProducers;
    private final HttpClient http;
    private final Duration timeout;

    private Verifier(Builder b) {
        this.providerUrl = Objects.requireNonNull(b.providerUrl, "providerUrl is required")
            .replaceAll("/+$", "");
        if (providerUrl.isBlank()) {
            throw new IllegalArgumentException("providerUrl must not be blank");
        }
        this.providerName = nz(b.providerName);
        this.providerVersion = nz(b.providerVersion);
        this.providerBranch = nz(b.providerBranch);
        this.broker = b.broker;
        this.stateHandlers = Map.copyOf(b.stateHandlers);
        this.stateSetupUrl = nz(b.stateSetupUrl);
        this.requestFilter = b.requestFilter;
        this.messageProducers = Map.copyOf(b.messageProducers);
        this.timeout = b.timeout != null ? b.timeout : Duration.ofSeconds(30);
        this.http = b.http != null ? b.http
            : HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String providerUrl;
        private String providerName;
        private String providerVersion;
        private String providerBranch;
        private BrokerClient broker;
        private final Map<String, StateHandler> stateHandlers = new LinkedHashMap<>();
        private String stateSetupUrl;
        private RequestFilter requestFilter;
        private final Map<String, MessageProducer> messageProducers = new LinkedHashMap<>();
        private Duration timeout;
        private HttpClient http;

        public Builder providerUrl(String v) { this.providerUrl = v; return this; }
        public Builder providerName(String v) { this.providerName = v; return this; }
        public Builder providerVersion(String v) { this.providerVersion = v; return this; }
        public Builder providerBranch(String v) { this.providerBranch = v; return this; }
        public Builder broker(BrokerClient v) { this.broker = v; return this; }
        public Builder stateHandler(String state, StateHandler h) {
            this.stateHandlers.put(state, h); return this;
        }
        public Builder stateSetupUrl(String v) { this.stateSetupUrl = v; return this; }
        public Builder requestFilter(RequestFilter v) { this.requestFilter = v; return this; }
        public Builder messageProducer(String description, MessageProducer fn) {
            this.messageProducers.put(description, fn); return this;
        }
        public Builder timeout(Duration v) { this.timeout = v; return this; }
        public Builder httpClient(HttpClient v) { this.http = v; return this; }
        public Verifier build() { return new Verifier(this); }
    }

    // ------------------------------------------------------------------
    // entry points
    // ------------------------------------------------------------------

    public VerificationResult verifyPactBytes(byte[] raw) throws IOException, InterruptedException {
        ParsedPact p = parsePactDoc(raw);
        return verifyInteractions(p.interactions);
    }

    public VerificationResult verifyPactFile(Path path) throws IOException, InterruptedException {
        return verifyPactBytes(Files.readAllBytes(path));
    }

    public VerificationResult verifyFromBroker(String consumer, String provider, String version)
            throws IOException, InterruptedException {
        if (broker == null) {
            throw new IllegalStateException("verifyFromBroker requires .broker(...)");
        }
        return verifyPactBytes(broker.fetch(consumer, provider, version));
    }

    /**
     * Verify a message-pact document (Asynchronous/Messages
     * interactions). For each expected message, looks up the
     * {@link MessageProducer} registered for that description, asks
     * it for the actual bytes, and matches them against the recorded
     * content shape using the same matcher engine as the HTTP path.
     */
    public VerificationResult verifyMessagePactBytes(byte[] raw)
            throws IOException, InterruptedException {
        Instant start = Instant.now();
        List<ru.mockarty.pact.message.MessagePact.Message> msgs =
            ru.mockarty.pact.message.MessagePactParser.parse(raw);
        List<InteractionResult> out = new ArrayList<>(msgs.size());
        for (ru.mockarty.pact.message.MessagePact.Message m : msgs) {
            String stateName = m.states.isEmpty() ? ""
                : String.valueOf(m.states.get(0).getOrDefault("name", ""));
            try {
                for (Map<String, Object> st : m.states) setUpState(st);
            } catch (Exception e) {
                out.add(InteractionResult.error(m.description, stateName,
                    "state setup: " + e.getMessage()));
                continue;
            }
            MessageProducer producer = messageProducers.get(m.description);
            if (producer == null) {
                out.add(InteractionResult.error(m.description, stateName,
                    "no MessageProducer registered for description \""
                        + m.description + "\""));
                continue;
            }
            MessagePayload payload;
            try {
                payload = producer.produce(m.description, m.states);
            } catch (Exception e) {
                out.add(InteractionResult.error(m.description, stateName,
                    "producer: " + e.getMessage()));
                continue;
            }
            List<Mismatch> mismatches = compareMessageBody(m.content, payload.body());
            out.add(new InteractionResult(m.description, stateName, 0,
                mismatches.isEmpty(), "", mismatches));
        }
        return new VerificationResult(providerName, start, Instant.now(), out);
    }

    private static List<Mismatch> compareMessageBody(Object expected, byte[] actual) {
        // Reuse the HTTP body-mismatch path by funneling through the
        // same Map-based comparison. The verifier's HTTP path expects
        // `Map<String,Object>` with a "body" key — we build that shape
        // here so future matcher additions land in one place.
        Map<String, Object> wrap = new LinkedHashMap<>();
        wrap.put("status", 0); // 0 = skip status check
        wrap.put("body", expected);
        return compareResponse(wrap, 0, Map.of(), actual);
    }

    /** POST verification result back to the broker (pact-foundation contract). */
    public void publishResults(String consumer, String provider, String version,
                               VerificationResult result)
            throws IOException, InterruptedException {
        if (broker == null) {
            throw new IllegalStateException("publishResults requires .broker(...)");
        }
        if (providerVersion.isBlank()) {
            throw new IllegalStateException("publishResults requires providerVersion");
        }
        Objects.requireNonNull(result, "result");

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("success", result.ok());
        payload.put("providerApplicationVersion", providerVersion);
        ObjectNode verifiedBy = payload.putObject("verifiedBy");
        verifiedBy.put("implementation", "mockarty-java-sdk");
        verifiedBy.put("version", "1");
        if (!providerBranch.isBlank()) {
            payload.put("branch", providerBranch);
        }
        var arr = payload.putArray("testResults");
        for (InteractionResult ir : result.interactions()) {
            ObjectNode row = arr.addObject();
            row.put("interactionDescription", ir.description());
            row.put("success", ir.passed());
            if (!ir.state().isBlank()) row.put("providerState", ir.state());
            if (!ir.error().isBlank()) row.put("error", ir.error());
            if (!ir.mismatches().isEmpty()) {
                var mArr = row.putArray("mismatches");
                for (Mismatch m : ir.mismatches()) {
                    ObjectNode mr = mArr.addObject();
                    mr.put("path", m.path());
                    mr.put("reason", m.reason());
                    if (m.expected() != null) mr.putPOJO("expected", m.expected());
                    if (m.actual() != null)   mr.putPOJO("actual", m.actual());
                }
            }
        }
        byte[] body = MAPPER.writeValueAsBytes(payload);
        String path = "/pacts/provider/" + enc(provider)
            + "/consumer/" + enc(consumer)
            + "/pact-version/" + enc(version) + "/verification-results";
        HttpRequest.Builder rb = HttpRequest.newBuilder()
            .uri(URI.create(broker.baseUrl() + path))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        broker.applyAuth(rb);
        HttpResponse<byte[]> resp = http.send(rb.build(),
            HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400) {
            throw new IOException("publishResults HTTP " + resp.statusCode() + ": "
                + new String(resp.body(), StandardCharsets.UTF_8));
        }
    }

    // ------------------------------------------------------------------
    // verify pipeline
    // ------------------------------------------------------------------

    private VerificationResult verifyInteractions(List<Map<String, Object>> ins)
            throws IOException, InterruptedException {
        Instant start = Instant.now();
        List<InteractionResult> out = new ArrayList<>(ins.size());
        for (Map<String, Object> ix : ins) {
            out.add(verifyOne(ix));
        }
        return new VerificationResult(providerName, start, Instant.now(), out);
    }

    @SuppressWarnings("unchecked")
    private InteractionResult verifyOne(Map<String, Object> ix)
            throws IOException, InterruptedException {
        String desc = stringOf(ix.get("description"));
        List<Map<String, Object>> states = new ArrayList<>();
        if (ix.get("providerStates") instanceof List<?> arr) {
            for (Object o : arr) {
                if (o instanceof Map<?, ?> m) {
                    states.add((Map<String, Object>) m);
                }
            }
        } else if (ix.get("providerState") instanceof String s && !s.isBlank()) {
            states.add(Map.of("name", s));
        }
        String stateName = states.isEmpty() ? "" : stringOf(states.get(0).get("name"));

        try {
            for (Map<String, Object> st : states) {
                setUpState(st);
            }
        } catch (Exception e) {
            return InteractionResult.error(desc, stateName,
                "state setup: " + e.getMessage());
        }

        Map<String, Object> req = mapOf(ix.get("request"));
        Map<String, Object> resp = mapOf(ix.get("response"));
        String method = stringOf(req.getOrDefault("method", "GET")).toUpperCase();
        String path = stringOf(req.getOrDefault("path", "/"));
        String query = buildQueryString(req.get("query"));
        Map<String, String> headers = flattenHeaders(req.get("headers"));
        byte[] body = bodyToBytes(req.get("body"));
        if (body != null && !hasHeaderCI(headers, "Content-Type")) {
            headers.put("Content-Type", "application/json");
        }
        String url = providerUrl + path + (query.isEmpty() ? "" : "?" + query);

        VerifierRequest filterReq = new VerifierRequest(method, url, headers, body);
        if (requestFilter != null) {
            try { requestFilter.apply(filterReq); }
            catch (Exception e) {
                return InteractionResult.error(desc, stateName,
                    "request filter: " + e.getMessage());
            }
        }

        HttpRequest.Builder rb = HttpRequest.newBuilder()
            .uri(URI.create(filterReq.url()))
            .timeout(timeout);
        for (Map.Entry<String, String> e : filterReq.headers().entrySet()) {
            rb.header(e.getKey(), e.getValue());
        }
        if (filterReq.body() == null) {
            rb.method(filterReq.method(), HttpRequest.BodyPublishers.noBody());
        } else {
            rb.method(filterReq.method(),
                HttpRequest.BodyPublishers.ofByteArray(filterReq.body()));
        }
        HttpResponse<byte[]> hr;
        try {
            hr = http.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            return InteractionResult.error(desc, stateName,
                "transport: " + e.getMessage());
        }

        List<Mismatch> mismatches = compareResponse(resp, hr.statusCode(),
            hr.headers().map(), hr.body());
        boolean passed = mismatches.isEmpty();
        return new InteractionResult(desc, stateName, hr.statusCode(), passed,
            "", mismatches);
    }

    private void setUpState(Map<String, Object> state) throws Exception {
        String name = stringOf(state.get("name"));
        @SuppressWarnings("unchecked")
        Map<String, Object> params = state.get("params") instanceof Map<?, ?> p
            ? (Map<String, Object>) p
            : Map.of();
        StateHandler handler = stateHandlers.get(name);
        if (handler != null) {
            handler.setUp(name, params);
            return;
        }
        if (!stateSetupUrl.isBlank()) {
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("state", name);
            payload.putPOJO("params", params);
            payload.put("action", "setup");
            byte[] body = MAPPER.writeValueAsBytes(payload);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(stateSetupUrl))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 400) {
                throw new IOException("state-setup HTTP " + resp.statusCode());
            }
        }
    }

    // ------------------------------------------------------------------
    // matching
    // ------------------------------------------------------------------

    private static List<Mismatch> compareResponse(
            Map<String, Object> expected, int actualStatus,
            Map<String, List<String>> actualHeaders, byte[] actualBody) {
        List<Mismatch> miss = new ArrayList<>();
        Object expStatus = expected.get("status");
        if (expStatus instanceof Number n && n.intValue() != actualStatus) {
            miss.add(new Mismatch("$.status", "status mismatch",
                n.intValue(), actualStatus));
        }
        Object expHeaders = expected.get("headers");
        if (expHeaders instanceof Map<?, ?> mh) {
            for (Map.Entry<?, ?> e : mh.entrySet()) {
                String key = e.getKey().toString();
                String got = headerLookup(actualHeaders, key);
                if (got.isEmpty()) {
                    miss.add(new Mismatch("$.headers." + key,
                        "expected header missing", e.getValue(), null));
                    continue;
                }
                List<String> wanted = new ArrayList<>();
                if (e.getValue() instanceof String s) wanted.add(s);
                else if (e.getValue() instanceof List<?> ls) {
                    for (Object o : ls) if (o instanceof String s) wanted.add(s);
                }
                for (String w : wanted) {
                    if (!got.contains(w)) {
                        miss.add(new Mismatch("$.headers." + key,
                            "header value mismatch", w, got));
                    }
                }
            }
        }
        Object expBody = expected.get("body");
        if (expBody != null) {
            miss.addAll(bodyMismatches(expBody, actualBody));
        }
        return miss;
    }

    private static List<Mismatch> bodyMismatches(Object expected, byte[] actualBytes) {
        if (actualBytes == null || actualBytes.length == 0) {
            return List.of(new Mismatch("$.body",
                "empty body where one was expected", expected, null));
        }
        JsonNode actual;
        try {
            actual = MAPPER.readTree(actualBytes);
        } catch (IOException e) {
            return List.of(new Mismatch("$.body",
                "actual body is not valid JSON: " + e.getMessage(),
                expected, new String(actualBytes, StandardCharsets.UTF_8)));
        }
        List<Mismatch> out = new ArrayList<>();
        JsonNode expectedNode = MAPPER.valueToTree(expected);
        strictMatch(expectedNode, actual, "$.body", out);
        return out;
    }

    private static void strictMatch(JsonNode expected, JsonNode actual, String path,
                                    List<Mismatch> out) {
        if (expected == null || expected.isNull()) {
            if (actual != null && !actual.isNull()) {
                out.add(new Mismatch(path, "value mismatch", null, jsonToVal(actual)));
            }
            return;
        }
        if (expected.isObject()) {
            if (!actual.isObject()) {
                out.add(new Mismatch(path, "expected object",
                    jsonToVal(expected), jsonToVal(actual)));
                return;
            }
            Iterator<Map.Entry<String, JsonNode>> it = expected.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String sub = path + "." + e.getKey();
                if (!actual.has(e.getKey())) {
                    out.add(new Mismatch(sub, "key missing",
                        jsonToVal(e.getValue()), null));
                    continue;
                }
                strictMatch(e.getValue(), actual.get(e.getKey()), sub, out);
            }
            return;
        }
        if (expected.isArray()) {
            if (!actual.isArray()) {
                out.add(new Mismatch(path, "expected array",
                    jsonToVal(expected), jsonToVal(actual)));
                return;
            }
            if (expected.size() != actual.size()) {
                out.add(new Mismatch(path, "array length mismatch",
                    expected.size(), actual.size()));
                return;
            }
            for (int i = 0; i < expected.size(); i++) {
                strictMatch(expected.get(i), actual.get(i), path + "[" + i + "]", out);
            }
            return;
        }
        if (!nodesEqual(expected, actual)) {
            out.add(new Mismatch(path, "value mismatch",
                jsonToVal(expected), jsonToVal(actual)));
        }
    }

    private static boolean nodesEqual(JsonNode a, JsonNode b) {
        if (a.isNumber() && b.isNumber()) {
            // Compare as BigDecimal so int64 IDs > 2^53 (common for
            // generated UUIDs encoded as numbers) don't lose precision
            // through Double conversion. Strip trailing zeros so 1 and
            // 1.0 still match.
            java.math.BigDecimal av = a.decimalValue().stripTrailingZeros();
            java.math.BigDecimal bv = b.decimalValue().stripTrailingZeros();
            return av.compareTo(bv) == 0;
        }
        return Objects.equals(jsonToVal(a), jsonToVal(b));
    }

    private static Object jsonToVal(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isBoolean()) return n.asBoolean();
        if (n.isInt()) return n.asInt();
        if (n.isLong()) return n.asLong();
        if (n.isDouble() || n.isFloatingPointNumber()) return n.asDouble();
        if (n.isTextual()) return n.asText();
        return n.toString();
    }

    private static boolean hasHeaderCI(Map<String, String> headers, String key) {
        for (String k : headers.keySet()) {
            if (k.equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private static String headerLookup(Map<String, List<String>> headers, String key) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) {
                return String.join(",", e.getValue());
            }
        }
        return "";
    }

    // ------------------------------------------------------------------
    // parsing (V3 + V4 union)
    // ------------------------------------------------------------------

    static ParsedPact parsePactDoc(byte[] raw) {
        JsonNode root;
        try {
            root = MAPPER.readTree(raw);
        } catch (IOException e) {
            throw new IllegalArgumentException("pact JSON parse: " + e.getMessage(), e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("pact root must be a JSON object");
        }
        JsonNode arr = root.path("interactions");
        List<Map<String, Object>> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode ix : arr) {
                if (ix.isObject()) {
                    out.add(jsonToMap(ix));
                } else {
                    out.add(new LinkedHashMap<>());
                }
            }
        }
        return new ParsedPact(out);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonToMap(JsonNode n) {
        Object v = MAPPER.convertValue(n, Object.class);
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : new LinkedHashMap<>();
    }

    static final class ParsedPact {
        final List<Map<String, Object>> interactions;
        ParsedPact(List<Map<String, Object>> in) { this.interactions = in; }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static String stringOf(Object v) { return v == null ? "" : v.toString(); }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Object v) {
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : new LinkedHashMap<>();
    }

    private static String buildQueryString(Object q) {
        if (q == null) return "";
        if (q instanceof String s) return s; // pre-encoded
        if (!(q instanceof Map<?, ?> m)) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            List<String> vals = new ArrayList<>();
            if (e.getValue() instanceof String s) vals.add(s);
            else if (e.getValue() instanceof List<?> ls) {
                for (Object o : ls) vals.add(String.valueOf(o));
            } else if (e.getValue() != null) {
                vals.add(String.valueOf(e.getValue()));
            }
            for (String val : vals) {
                if (sb.length() > 0) sb.append('&');
                sb.append(enc(e.getKey().toString())).append('=').append(enc(val));
            }
        }
        return sb.toString();
    }

    private static Map<String, String> flattenHeaders(Object headers) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!(headers instanceof Map<?, ?> m)) return out;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            String key = e.getKey().toString();
            if (e.getValue() instanceof String s) out.put(key, s);
            else if (e.getValue() instanceof List<?> ls) {
                StringBuilder sb = new StringBuilder();
                for (Object o : ls) {
                    if (o instanceof String s) {
                        if (sb.length() > 0) sb.append(',');
                        sb.append(s);
                    }
                }
                if (sb.length() > 0) out.put(key, sb.toString());
            }
        }
        return out;
    }

    private static byte[] bodyToBytes(Object body) {
        if (body == null) return null;
        if (body instanceof byte[] b) return b;
        if (body instanceof String s) return s.getBytes(StandardCharsets.UTF_8);
        try {
            return MAPPER.writeValueAsBytes(body);
        } catch (IOException e) {
            return ("{\"jsonMarshalError\":\"" + e.getMessage() + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // nz: null→"" + trim, matching BrokerClient.nz so a stray whitespace
    // in providerVersion / providerName etc. doesn't propagate into
    // published verification results.
    private static String nz(String s) { return s == null ? "" : s.trim(); }

    /** Mutable view of an outgoing request — for {@link RequestFilter}. */
    public static final class VerifierRequest {
        private String method;
        private String url;
        private final Map<String, String> headers;
        private byte[] body;

        VerifierRequest(String method, String url, Map<String, String> headers, byte[] body) {
            this.method = method; this.url = url; this.headers = headers; this.body = body;
        }
        public String method() { return method; }
        public String url() { return url; }
        public Map<String, String> headers() { return headers; }
        public byte[] body() { return body; }
        public void method(String v) { this.method = v; }
        public void url(String v) { this.url = v; }
        public void body(byte[] v) { this.body = v; }
    }
}
