// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.builder;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fluent DSL for describing a load test.
 *
 * <p>Describe a load test in idiomatic Java and emit either a k6-compatible
 * JavaScript script ({@link #toK6Script()}) or a perf-config JSON
 * ({@link #toPerfConfig()} / {@link #toJson()} / {@link #save(String)}) that
 * {@code mockarty-cli perf run --from-config <file>} runs locally — the perf
 * engine runs in-process, no server required.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * LoadTestBuilder.named("checkout")
 *     .target("https://api.example.com")
 *     .get("/health")
 *     .post("/order", Map.of("sku", "abc"))
 *     .stage("30s", 50)
 *     .stage("1m", 50)
 *     .stage("10s", 0)
 *     .threshold("http_req_duration", "p(95)<800")
 *     .threshold("http_req_failed", "rate<0.01")
 *     .thinkTime(0.5)
 *     .save("checkout.json");
 * //   $ mockarty-cli perf run --from-config checkout.json
 * }</pre>
 *
 * <p>The builder is a thin wrapper around the existing perf engine; it does not
 * run anything itself.</p>
 */
public final class LoadTestBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** One HTTP request in the scenario's iteration body. */
    private static final class Req {
        final String method;
        final String path;
        final Object body;
        final Map<String, String> headers;
        // (name, expr) per-request k6 checks. When non-empty they replace the
        // default `status < 400` assertion.
        final List<String[]> checks = new ArrayList<>();

        Req(String method, String path, Object body, Map<String, String> headers) {
            this.method = method.toUpperCase();
            this.path = path;
            this.body = body;
            this.headers = headers;
        }
    }

    /** One ramp stage: reach {@code target} VUs over {@code duration}. */
    private static final class Stage {
        final String duration;
        final int target;
        final int targetRps;

        Stage(String duration, int target, int targetRps) {
            this.duration = duration;
            this.target = target;
            this.targetRps = targetRps;
        }
    }

    private final String name;
    private String baseUrl;
    private final List<Req> requests = new ArrayList<>();
    private final List<Stage> stages = new ArrayList<>();
    private final Map<String, List<String>> thresholds = new LinkedHashMap<>();
    private final Map<String, String> env = new LinkedHashMap<>();
    private Integer vus;
    private String duration;
    private Integer rps;
    private Integer maxVus;
    private Double think;

    private LoadTestBuilder(String name) {
        this.name = (name == null || name.isEmpty()) ? "load-test" : name;
    }

    /** Create a load test with the given name. */
    public static LoadTestBuilder named(String name) {
        return new LoadTestBuilder(name);
    }

    // -- target / requests ---------------------------------------------------

    /**
     * Set the base URL; request paths are joined onto it and the URL is exposed
     * to the script as {@code __ENV.BASE_URL}.
     */
    public LoadTestBuilder target(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.env.putIfAbsent("BASE_URL", this.baseUrl);
        return this;
    }

    /** Append an arbitrary request to the iteration body. */
    public LoadTestBuilder request(String method, String path, Object body, Map<String, String> headers) {
        requests.add(new Req(method, path, body, headers));
        return this;
    }

    public LoadTestBuilder get(String path) {
        return request("GET", path, null, null);
    }

    public LoadTestBuilder post(String path, Object body) {
        return request("POST", path, body, null);
    }

    public LoadTestBuilder put(String path, Object body) {
        return request("PUT", path, body, null);
    }

    public LoadTestBuilder patch(String path, Object body) {
        return request("PATCH", path, body, null);
    }

    public LoadTestBuilder delete(String path) {
        return request("DELETE", path, null, null);
    }

    /**
     * Attaches a named assertion to the MOST RECENTLY added request. {@code name}
     * is the check label; {@code expr} is a JavaScript boolean expression that
     * may reference the response as {@code res} (e.g.
     * {@code res.json().id !== undefined}). When a request has one or more checks
     * they REPLACE the default {@code status < 400} check. No-op if no request
     * has been added yet.
     */
    public LoadTestBuilder check(String name, String expr) {
        if (!requests.isEmpty()) {
            requests.get(requests.size() - 1).checks.add(new String[]{name, expr});
        }
        return this;
    }

    /** Shorthand for {@link #check} asserting the response status code. */
    public LoadTestBuilder expectStatus(int code) {
        return check("status is " + code, "res.status === " + code);
    }

    // -- load profile --------------------------------------------------------

    /** Set a constant virtual-user count (ignored when stages are set). */
    public LoadTestBuilder vus(int n) {
        this.vus = n;
        return this;
    }

    /** Set a constant run duration ("30s", "5m"); ignored with stages. */
    public LoadTestBuilder duration(String d) {
        this.duration = d;
        return this;
    }

    /** Target a steady requests-per-second arrival rate. */
    public LoadTestBuilder rps(int n) {
        this.rps = n;
        return this;
    }

    /** Cap concurrent VUs (mostly relevant in RPS / arrival-rate mode). */
    public LoadTestBuilder maxVus(int n) {
        this.maxVus = n;
        return this;
    }

    /** Append one ramp stage: reach {@code target} VUs over {@code duration}. */
    public LoadTestBuilder stage(String duration, int target) {
        stages.add(new Stage(duration, target, 0));
        return this;
    }

    /** Append an arrival-rate (RPS-target) ramp stage. */
    public LoadTestBuilder rpsStage(String duration, int targetRps) {
        stages.add(new Stage(duration, 0, targetRps));
        return this;
    }

    // -- thresholds / env ----------------------------------------------------

    /** Add a pass/fail expression on a metric, e.g. ("http_req_duration", "p(95)&lt;500"). */
    public LoadTestBuilder threshold(String metric, String expr) {
        thresholds.computeIfAbsent(metric, k -> new ArrayList<>()).add(expr);
        return this;
    }

    /** Add an environment variable, exposed as {@code __ENV.<KEY>} in the script. */
    public LoadTestBuilder env(String key, String value) {
        this.env.put(key, value);
        return this;
    }

    /** Add a {@code sleep(seconds)} at the end of each iteration. */
    public LoadTestBuilder thinkTime(double seconds) {
        this.think = seconds;
        return this;
    }

    // -- emit ----------------------------------------------------------------

    private List<Req> resolvedRequests() {
        if (!requests.isEmpty()) {
            return requests;
        }
        List<Req> def = new ArrayList<>();
        def.add(new Req("GET", "/", null, null));
        return def;
    }

    private static String jsStr(String s) {
        String escaped = s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");
        return "'" + escaped + "'";
    }

    /** Emit a k6-compatible JS script with {@code export const options}. */
    public String toK6Script() {
        StringBuilder sb = new StringBuilder();
        sb.append("import http from 'k6/http';\n");
        sb.append("import { check, sleep } from 'k6';\n\n");
        sb.append("export const options = ").append(optionsJson()).append(";\n\n");
        // Bake the target() base URL as a runnable default so the exported
        // script works out of the box (matching the perf engine's own builder
        // pattern), while staying overridable via `-e BASE_URL=...` / __ENV.
        if (baseUrl != null) {
            sb.append("const BASE_URL = __ENV.BASE_URL || ").append(jsStr(baseUrl)).append(";\n\n");
        }
        sb.append("export default function () {\n");
        sb.append("  let r;\n");
        for (Req req : resolvedRequests()) {
            sb.append(requestJs(req));
        }
        if (think != null) {
            sb.append("  sleep(").append(formatDouble(think)).append(");\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String formatDouble(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    private String optionsJson() {
        Map<String, Object> opts = new LinkedHashMap<>();
        if (!stages.isEmpty()) {
            opts.put("stages", stagesAsMaps());
        } else {
            if (vus != null) {
                opts.put("vus", vus);
            }
            if (duration != null) {
                opts.put("duration", duration);
            }
        }
        if (rps != null) {
            opts.put("rps", rps);
        }
        if (maxVus != null) {
            opts.put("maxVus", maxVus);
        }
        if (!thresholds.isEmpty()) {
            opts.put("thresholds", thresholds);
        }
        if (opts.isEmpty()) {
            opts.put("vus", 1);
            opts.put("duration", "30s");
        }
        try {
            return MAPPER.writeValueAsString(opts);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize options", e);
        }
    }

    private List<Map<String, Object>> stagesAsMaps() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Stage s : stages) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("duration", s.duration);
            if (s.targetRps > 0) {
                m.put("targetRps", s.targetRps);
            } else {
                m.put("target", s.target);
            }
            out.add(m);
        }
        return out;
    }

    private String requestJs(Req req) {
        String url;
        if (baseUrl != null && !req.path.startsWith("http")) {
            String path = req.path;
            if (!path.isEmpty() && !path.startsWith("/")) {
                path = "/" + path;
            }
            url = "`${BASE_URL}" + path + "`";
        } else {
            url = jsStr(req.path);
        }

        Map<String, String> headers = new TreeMap<>();
        if (req.headers != null) {
            headers.putAll(req.headers);
        }

        String bodyLit = null;
        boolean jsonBody = false;
        if (req.body != null) {
            if (req.body instanceof String) {
                bodyLit = jsStr((String) req.body);
            } else {
                try {
                    bodyLit = jsStr(MAPPER.writeValueAsString(req.body));
                } catch (Exception e) {
                    throw new IllegalStateException("failed to serialize request body", e);
                }
                jsonBody = true;
            }
        }
        if (jsonBody) {
            headers.putIfAbsent("Content-Type", "application/json");
        }

        String params = null;
        if (!headers.isEmpty()) {
            StringBuilder h = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (!first) {
                    h.append(", ");
                }
                h.append(jsStr(e.getKey())).append(": ").append(jsStr(e.getValue()));
                first = false;
            }
            params = "{headers: {" + h + "}}";
        }

        String method = req.method.toLowerCase();
        StringBuilder sb = new StringBuilder();
        if (req.body == null) {
            if (params != null) {
                sb.append("  r = http.").append(method).append("(").append(url)
                        .append(", null, ").append(params).append(");\n");
            } else {
                sb.append("  r = http.").append(method).append("(").append(url).append(");\n");
            }
        } else {
            if (params != null) {
                sb.append("  r = http.").append(method).append("(").append(url)
                        .append(", ").append(bodyLit).append(", ").append(params).append(");\n");
            } else {
                sb.append("  r = http.").append(method).append("(").append(url)
                        .append(", ").append(bodyLit).append(");\n");
            }
        }
        sb.append(checkJs(req.checks));
        return sb.toString();
    }

    /**
     * Emit the k6 {@code check(r, { ... })} line for a request. With no custom
     * checks it emits the default {@code status < 400} assertion (backward
     * compatible); otherwise every custom check in insertion order. Kept
     * byte-identical across the Go/Python/Java SDKs.
     */
    private static String checkJs(List<String[]> checks) {
        if (checks.isEmpty()) {
            return "  check(r, { 'status < 400': (res) => res.status < 400 });\n";
        }
        StringBuilder sb = new StringBuilder("  check(r, { ");
        boolean first = true;
        for (String[] c : checks) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(jsStr(c[0])).append(": (res) => ").append(c[1]);
            first = false;
        }
        sb.append(" });\n");
        return sb.toString();
    }

    /**
     * Emit the perf-config map consumed by the CLI {@code --from-config} flag
     * and the server {@code /api/v1/perf} endpoints. Carries the full profile so
     * a staged ramp survives the round-trip.
     */
    public Map<String, Object> toPerfConfig() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("name", name);
        cfg.put("script", toK6Script());
        if (!stages.isEmpty()) {
            cfg.put("stages", stagesAsMaps());
        } else {
            if (vus != null) {
                cfg.put("vus", vus);
            }
            if (duration != null) {
                cfg.put("duration", duration);
            }
        }
        if (rps != null) {
            cfg.put("rps", rps);
        }
        if (maxVus != null) {
            cfg.put("maxVus", maxVus);
        }
        if (!thresholds.isEmpty()) {
            cfg.put("thresholds", thresholds);
        }
        if (!env.isEmpty()) {
            cfg.put("environment", env);
        }
        return cfg;
    }

    /** Serialize {@link #toPerfConfig()} to an indented JSON string. */
    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(toPerfConfig());
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize perf config", e);
        }
    }

    /**
     * Write the perf-config JSON to {@code path}; returns the path. Run it with
     * {@code mockarty-cli perf run --from-config <path>}.
     */
    public String save(String path) throws IOException {
        Path p = Paths.get(path);
        Files.write(p, toJson().getBytes(StandardCharsets.UTF_8));
        return path;
    }
}
