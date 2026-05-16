// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ru.mockarty.pact.plugins.Plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * In-process Pact mock server built on the JDK's
 * {@link com.sun.net.httpserver.HttpServer}.
 *
 * <p>Started ephemeral (port 0 → OS picks a free port), serves the
 * interactions declared on the wrapped {@link Pact}, records which
 * interactions were actually called, and supports a strict
 * {@link #verify()} that fails if any interaction went uncalled.</p>
 *
 * <p>{@link AutoCloseable}: closing the server shuts down the underlying
 * {@link HttpServer} and writes the pact.json file to
 * {@link Pact#outputDir()} (if any) — exactly the lifecycle the JUnit5
 * extension relies on.</p>
 */
public final class MockServer implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Pact pact;
    private final HttpServer server;
    private final URI baseUri;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<Integer, Integer> hits = new ConcurrentHashMap<>();
    private final List<String> unexpectedRequests = new CopyOnWriteArrayList<>();
    private final List<MismatchReport> mismatches = new CopyOnWriteArrayList<>();
    private final boolean writeOnClose;
    private final boolean strict;

    private MockServer(Pact pact, HttpServer server, URI baseUri, boolean writeOnClose, boolean strict) {
        this.pact = pact;
        this.server = server;
        this.baseUri = baseUri;
        this.writeOnClose = writeOnClose;
        this.strict = strict;
    }

    /** Start a mock server bound to an ephemeral port on 127.0.0.1
     *  with strict body-matching enabled (any declared matcher is enforced
     *  on the inbound request before the mock accepts it). */
    public static MockServer start(Pact pact) {
        return start(pact, true, true);
    }

    /** Start a mock server; control whether the pact file is written on close. */
    public static MockServer start(Pact pact, boolean writeOnClose) {
        return start(pact, writeOnClose, true);
    }

    /**
     * Full-control entry point.
     *
     * @param pact         the contract to serve.
     * @param writeOnClose write {@code <consumer>-<provider>.json} on close.
     * @param strict       if {@code true}, validate every inbound request
     *                     against the declared matchers and plugin payload
     *                     rules; mismatched requests get a 422 and are
     *                     recorded so {@link #verify()} fails them. If
     *                     {@code false}, the server stays in legacy
     *                     existence-only matching mode.
     */
    public static MockServer start(Pact pact, boolean writeOnClose, boolean strict) {
        Objects.requireNonNull(pact, "pact must not be null");
        try {
            HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            // A dedicated executor keeps the mock isolated from the JVM
            // common pool — a slow test handler won't starve user code.
            http.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "mockarty-pact-mock-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }));
            URI uri = URI.create(
                    "http://127.0.0.1:" + http.getAddress().getPort());

            MockServer ms = new MockServer(pact, http, uri, writeOnClose, strict);
            http.createContext("/", new RouterHandler(pact, ms));
            http.start();
            return ms;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start Pact mock server", e);
        }
    }

    /** Base URI the consumer code should talk to. */
    public URI uri() { return baseUri; }

    /** The underlying contract. */
    public Pact pact() { return pact; }

    /**
     * Verify every declared interaction was hit at least once and no
     * unexpected request reached the server.
     *
     * @throws AssertionError when expectations don't match — surfaces in
     *     JUnit5 as a normal test failure.
     */
    public void verify() {
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < pact.interactions().size(); i++) {
            int hitCount = hits.getOrDefault(i, 0);
            if (hitCount == 0) {
                missing.add(pact.interactions().get(i).description());
            }
        }
        if (!missing.isEmpty() || !unexpectedRequests.isEmpty() || !mismatches.isEmpty()) {
            StringBuilder sb = new StringBuilder("Pact verification failed:");
            if (!missing.isEmpty()) {
                sb.append("\n  Uncalled interactions: ").append(missing);
            }
            if (!unexpectedRequests.isEmpty()) {
                sb.append("\n  Unexpected requests:");
                for (String s : unexpectedRequests) {
                    sb.append("\n    - ").append(s);
                }
            }
            if (!mismatches.isEmpty()) {
                sb.append("\n  Mismatches:");
                for (MismatchReport m : mismatches) {
                    sb.append("\n    - ").append(m.toLine());
                }
            }
            throw new AssertionError(sb.toString());
        }
    }

    /** Returns an immutable snapshot of recorded mismatches. */
    public List<MismatchReport> mismatches() {
        return Collections.unmodifiableList(new ArrayList<>(mismatches));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            // A zero-delay stop terminates the server immediately. We rely
            // on it because tests spawn many servers per class — a slow
            // shutdown would multiply suite duration.
            server.stop(0);
        } finally {
            if (writeOnClose && pact.outputDir() != null) {
                try {
                    pact.writeToFile();
                } catch (IOException e) {
                    // Best-effort: the verification has already happened.
                    // Surface as a wrapped error so a CI step that's
                    // strictly watching for pact files still sees it.
                    throw new IllegalStateException(
                            "Failed to write pact.json after close", e);
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Internal: routing
    // ────────────────────────────────────────────────────────────────

    private static final class RouterHandler implements HttpHandler {
        private final Pact pact;
        private final MockServer owner;

        RouterHandler(Pact pact, MockServer owner) {
            this.pact = pact;
            this.owner = owner;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getRawPath();
            byte[] requestBody = ex.getRequestBody().readAllBytes();

            int matchedIdx = -1;
            for (int i = 0; i < pact.interactions().size(); i++) {
                Interaction it = pact.interactions().get(i);
                if (matches(it.request(), method, path, ex)) {
                    matchedIdx = i;
                    break;
                }
            }

            if (matchedIdx == -1) {
                String summary = method + " " + path
                        + " headers=" + flattenHeaders(ex)
                        + " (no matching interaction declared)";
                owner.unexpectedRequests.add(summary);
                writeError(ex, 500, "Mockarty Pact: " + summary);
                return;
            }

            Interaction matched = pact.interactions().get(matchedIdx);

            // Strict body / plugin validation — runs only when the caller
            // opted into strict mode (default). Returns a non-empty list
            // when one or more matchers disagreed with the inbound bytes.
            if (owner.strict) {
                List<MismatchReport> ms = validateBody(matched.request(), requestBody, ex, pact.resolvedPlugins());
                if (!ms.isEmpty()) {
                    owner.mismatches.addAll(ms);
                    StringBuilder body = new StringBuilder("Mockarty Pact: body mismatch\n");
                    for (MismatchReport m : ms) body.append("  - ").append(m.toLine()).append('\n');
                    // 422 Unprocessable Entity — pact-jvm reference uses
                    // the same code so cross-tool consumers see a familiar
                    // failure mode.
                    writeError(ex, 422, body.toString());
                    // Still count the hit so verify()'s "uncalled" branch
                    // doesn't double-fail the same interaction. The
                    // mismatch list carries the real failure.
                    owner.hits.merge(matchedIdx, 1, Integer::sum);
                    return;
                }
            }

            owner.hits.merge(matchedIdx, 1, Integer::sum);
            writeResponse(ex, matched.response(), pact);
        }

        /**
         * Strict body validation: when the declared request body is JSON
         * with matchers, delegates to {@link MatcherEngine}; for binary
         * bodies the plugins registered on the pact get first dibs based
         * on content-type. Returns an empty list on a clean match.
         */
        private static List<MismatchReport> validateBody(PactRequest expected, byte[] actual, HttpExchange ex,
                                                          List<Plugin> plugins) {
            PactBody body = expected.body();
            String contentType = firstHeaderLower(ex, "Content-Type");

            // Plugin-owned bodies short-circuit JSON/Text comparison and
            // hand the raw bytes to the plugin's matchRequest.
            for (Plugin p : plugins) {
                if (p == null) continue;
                if (matchesContentType(p, contentType)) {
                    byte[] declaredExample = bodyAsBytes(body);
                    return p.matchRequest(contentType, declaredExample, actual);
                }
            }

            if (body instanceof PactBody.Empty) return Collections.emptyList();
            if (body instanceof PactBody.Text t) {
                String s = new String(actual, StandardCharsets.UTF_8);
                if (!t.body().equals(s)) {
                    return Collections.singletonList(new MismatchReport(
                            "$.body", t.body(), s, "text.equality"));
                }
                return Collections.emptyList();
            }
            if (body instanceof PactBody.Binary bin) {
                byte[] e = bin.body();
                if (e.length != actual.length) {
                    return Collections.singletonList(new MismatchReport(
                            "$.body[binary]",
                            "length=" + e.length, "length=" + actual.length,
                            "binary.length"));
                }
                for (int i = 0; i < e.length; i++) {
                    if (e[i] != actual[i]) {
                        return Collections.singletonList(new MismatchReport(
                                "$.body[binary][" + i + "]",
                                String.format("0x%02x", e[i]),
                                String.format("0x%02x", actual[i]),
                                "binary.byte"));
                    }
                }
                return Collections.emptyList();
            }
            if (body instanceof PactBody.Json j) {
                if (actual.length == 0) {
                    return Collections.singletonList(new MismatchReport(
                            "$.body", "non-empty JSON", "empty", "json.empty"));
                }
                try {
                    Object actualTree = MAPPER.readValue(actual, Object.class);
                    return MatcherEngine.compare(j.root(), actualTree, "$.body");
                } catch (IOException parseErr) {
                    return Collections.singletonList(new MismatchReport(
                            "$.body", "valid JSON",
                            "parse error: " + parseErr.getMessage(),
                            "json.parse"));
                }
            }
            return Collections.emptyList();
        }

        private static String firstHeaderLower(HttpExchange ex, String name) {
            for (Map.Entry<String, List<String>> e : ex.getRequestHeaders().entrySet()) {
                if (e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0).toLowerCase(Locale.ROOT);
                }
            }
            return "";
        }

        private static boolean matchesContentType(Plugin p, String contentType) {
            if (contentType == null || contentType.isBlank()) return false;
            String head = contentType.split(";", 2)[0].trim();
            for (String s : p.supportedContentTypes()) {
                if (s.equalsIgnoreCase(head)) return true;
            }
            return false;
        }

        private static byte[] bodyAsBytes(PactBody body) {
            if (body instanceof PactBody.Binary b) return b.body();
            if (body instanceof PactBody.Text t) return t.body().getBytes(StandardCharsets.UTF_8);
            if (body instanceof PactBody.Json j) {
                try { return MAPPER.writeValueAsBytes(j.root()); }
                catch (JsonProcessingException ignored) { return new byte[0]; }
            }
            return new byte[0];
        }


        private static boolean matches(PactRequest pr, String method, String path, HttpExchange ex) {
            if (!pr.method().equalsIgnoreCase(method)) return false;

            // Path match — honour a regex matcher if declared, else literal.
            if (pr.pathMatcher() instanceof Matcher.Term t) {
                if (!safeMatches(t.regex(), path)) return false;
            } else if (pr.pathMatcher() instanceof Matcher.Regex re) {
                if (!safeMatches(re.pattern(), path)) return false;
            } else {
                if (!pr.path().equals(path)) return false;
            }

            // Required headers — case-insensitive name lookup.
            for (Map.Entry<String, Object> e : pr.headers().entrySet()) {
                String name = e.getKey();
                Object expected = e.getValue();
                List<String> actual = ex.getRequestHeaders().get(name);
                if (actual == null || actual.isEmpty()) {
                    // Try case-insensitive lookup as a fallback.
                    actual = null;
                    for (Map.Entry<String, List<String>> h : ex.getRequestHeaders().entrySet()) {
                        if (h.getKey().equalsIgnoreCase(name)) {
                            actual = h.getValue();
                            break;
                        }
                    }
                    if (actual == null || actual.isEmpty()) return false;
                }
                if (!headerValueAcceptable(expected, actual)) return false;
            }

            return true;
        }

        private static boolean headerValueAcceptable(Object expected, List<String> actual) {
            // Treat the expected as a regex when it's a Term/Regex matcher,
            // a literal when it's anything else (incl. {@link Matcher.Like}
            // where any value is acceptable so long as it exists).
            if (expected instanceof Matcher.Term t) {
                return actual.stream().anyMatch(v -> safeMatches(t.regex(), v));
            }
            if (expected instanceof Matcher.Regex re) {
                return actual.stream().anyMatch(v -> safeMatches(re.pattern(), v));
            }
            if (expected instanceof Matcher) {
                // Other matcher kinds: existence is enough for the mock —
                // verification of value shape is the provider-side job.
                return true;
            }
            String s = String.valueOf(expected);
            return actual.contains(s);
        }

        private static boolean safeMatches(String regex, String input) {
            try {
                return Pattern.compile(regex).matcher(input).matches();
            } catch (PatternSyntaxException e) {
                return false;
            }
        }

        private static void writeResponse(HttpExchange ex, PactResponse r, Pact pact) throws IOException {
            for (Map.Entry<String, Object> e : r.headers().entrySet()) {
                ex.getResponseHeaders().add(e.getKey(), unwrapHeader(e.getValue()));
            }
            byte[] body = renderBody(r.body(), ex, pact);
            int len = body.length == 0 ? -1 : body.length;
            ex.sendResponseHeaders(r.status(), len);
            if (body.length > 0) {
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(body);
                }
            } else {
                ex.close();
            }
        }

        private static byte[] renderBody(PactBody b, HttpExchange ex, Pact pact) {
            if (b instanceof PactBody.Empty) return new byte[0];
            // Response-side content type drives plugin output. If a plugin
            // claims it, hand the example bytes off so it can re-frame /
            // re-encode (e.g. wrap a Protobuf message back into a gRPC frame).
            String responseCt = "";
            for (Map.Entry<String, List<String>> h : ex.getResponseHeaders().entrySet()) {
                if (h.getKey().equalsIgnoreCase("Content-Type") && !h.getValue().isEmpty()) {
                    responseCt = h.getValue().get(0).toLowerCase(Locale.ROOT);
                    break;
                }
            }
            for (Plugin p : pact.resolvedPlugins()) {
                if (p == null) continue;
                if (matchesContentType(p, responseCt)) {
                    return p.generateResponse(responseCt, bodyAsBytes(b));
                }
            }
            if (b instanceof PactBody.Text t) {
                return t.body().getBytes(StandardCharsets.UTF_8);
            }
            if (b instanceof PactBody.Binary bin) {
                return bin.body();
            }
            if (b instanceof PactBody.Json j) {
                // Peel matchers, render examples to bytes.
                Object peeled = peel(j.root());
                try {
                    return MAPPER.writeValueAsBytes(peeled);
                } catch (JsonProcessingException ignored) {
                    return new byte[0];
                }
            }
            return new byte[0];
        }

        private static Object peel(Object node) {
            if (node instanceof Matcher m) {
                return peel(m.example());
            }
            if (node instanceof Map<?, ?> raw) {
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : raw.entrySet()) {
                    out.put(String.valueOf(e.getKey()), peel(e.getValue()));
                }
                return out;
            }
            if (node instanceof List<?> raw) {
                List<Object> out = new ArrayList<>(raw.size());
                for (Object item : raw) out.add(peel(item));
                return out;
            }
            return node;
        }

        private static String unwrapHeader(Object value) {
            if (value instanceof Matcher m) {
                return String.valueOf(m.example());
            }
            return String.valueOf(value);
        }

        private static void writeError(HttpExchange ex, int status, String message) throws IOException {
            byte[] body = message.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(status, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }

        private static String flattenHeaders(HttpExchange ex) {
            return ex.getRequestHeaders().entrySet().stream()
                    .map(e -> e.getKey().toLowerCase(Locale.ROOT) + "="
                            + String.join(",", e.getValue()))
                    .collect(Collectors.joining(";"));
        }
    }

    // ── Internal accessors for tests ────────────────────────────────

    /** Returns an immutable snapshot of how many times each interaction was hit. */
    public Map<Integer, Integer> hits() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(hits));
    }

    /** Returns the list of unexpected requests received so far. */
    public List<String> unexpectedRequests() {
        return Collections.unmodifiableList(new ArrayList<>(unexpectedRequests));
    }
}
