// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Security Agent API — CI/CD-useful subset of {@code /api/v1/security/*}.
 *
 * <p>Exposes the operator-friendly surface: start a scan, poll status,
 * list findings, download SARIF, list scanners, cancel scans. Admin
 * operations (LLM profile CRUD, remote-agent on/off, scanner-template
 * editing) live in the admin UI and are intentionally NOT in the SDK.</p>
 *
 * <p>The server gates every route behind the {@code security_agent}
 * feature flag; a 403 from any of these calls means the namespace
 * lacks the licence feature.</p>
 */
public class SecurityApi {

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "sarif", "vex", "cyclonedx", "cyclonedx-vex", "html", "pdf", "allure");

    private final MockartyClient client;

    public SecurityApi(MockartyClient client) {
        this.client = client;
    }

    // ---- Scans ----

    /**
     * Start an orchestrated security scan. Returns the freshly-created
     * report row (including server-assigned {@code id} and initial
     * {@code status}). Poll {@link #getReport(String)} until status is
     * one of {@code done}, {@code failed}, {@code cancelled}.
     *
     * @param namespace target Mockarty namespace.
     * @param target    URL or host:port to scan.
     * @param persona   pentest persona (currently informational); use
     *                  {@code "web_pentester"} for HTTP scans.
     * @param intensity one of {@code passive | safe-active | intrusive |
     *                  destructive}. Use {@code passive} for routine CI.
     * @param title     human label for the report (nullable).
     * @return the created report row (keys mirror server JSON).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> startScan(
            String namespace,
            String target,
            String persona,
            String intensity,
            String title) throws MockartyException {
        Map<String, Object> profile = new HashMap<>();
        profile.put("intensity", intensity == null || intensity.isEmpty() ? "passive" : intensity);
        profile.put("scopeDescription", target);
        profile.put("targets", List.of(Map.of("url", target, "method", "GET")));
        profile.put("redactTokensInReport", true);

        Map<String, Object> body = new HashMap<>();
        body.put("title", title == null || title.isEmpty()
                ? "sdk-" + (persona == null ? "scan" : persona) + "-" + target
                : title);
        body.put("namespace", namespace);
        body.put("profile", profile);

        Map<String, Object> resp = client.post("/api/v1/security/scans", body, Map.class);
        Object rep = resp == null ? null : resp.get("report");
        return rep instanceof Map ? (Map<String, Object>) rep : resp;
    }

    /**
     * Returns the current state of a scan report.
     *
     * <p>GET /api/v1/security/reports/:id</p>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getReport(String reportId) throws MockartyException {
        Map<String, Object> resp = client.get(
                "/api/v1/security/reports/" + encode(reportId), Map.class);
        Object rep = resp == null ? null : resp.get("report");
        return rep instanceof Map ? (Map<String, Object>) rep : resp;
    }

    /**
     * Signals an in-flight scan to wind down. Idempotent on terminal
     * reports: the server returns 409 Conflict (mapped to a
     * {@link MockartyException} subclass) when the report is already
     * {@code done}/{@code failed}/{@code cancelled}.
     *
     * <p>POST /api/v1/security/reports/:id/cancel</p>
     */
    public void cancelScan(String reportId) throws MockartyException {
        client.post("/api/v1/security/reports/" + encode(reportId) + "/cancel",
                Map.of(), Map.class);
    }

    // ---- Findings ----

    /**
     * Returns every finding recorded against the report. Optional
     * {@code severity} filter is applied client-side to keep the SDK
     * shape stable; the server doesn't (yet) support a
     * {@code ?severity=} query param.
     *
     * <p>GET /api/v1/security/reports/:id/findings</p>
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listFindings(String reportId, String severity)
            throws MockartyException {
        Map<String, Object> resp = client.get(
                "/api/v1/security/reports/" + encode(reportId) + "/findings",
                Map.class);
        List<Map<String, Object>> findings = new ArrayList<>();
        Object raw = resp == null ? null : resp.get("findings");
        if (raw instanceof List) {
            List<?> rawList = (List<?>) raw;
            for (Object f : rawList) {
                if (f instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) f;
                    findings.add((Map<String, Object>) m);
                }
            }
        }
        if (severity == null || severity.isEmpty()) {
            return findings;
        }
        String wanted = severity.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> f : findings) {
            Object s = f.get("severity");
            if (s != null && wanted.equals(s.toString().toLowerCase(Locale.ROOT))) {
                filtered.add(f);
            }
        }
        return filtered;
    }

    // ---- Exports ----

    /**
     * Download the report serialised in {@code format} — one of
     * {@code sarif | vex | html | pdf | allure}. Returns raw bytes.
     *
     * <p>GET /api/v1/security/reports/:id/export?format=&lt;format&gt;</p>
     */
    public byte[] exportReport(String reportId, String format) throws MockartyException {
        String fmt = (format == null || format.isEmpty() ? "sarif" : format)
                .toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FORMATS.contains(fmt)) {
            throw new IllegalArgumentException(
                    "unsupported export format: " + format
                            + " (want one of sarif|vex|html|pdf|allure)");
        }
        // The export endpoint returns raw bytes (SARIF JSON, HTML, PDF,
        // Allure JSON or CycloneDX-VEX), so route through {@code getBytes}
        // rather than the JSON-decoding {@code get} path.
        return client.getBytes(
                "/api/v1/security/reports/" + encode(reportId) + "/export?format=" + encode(fmt));
    }

    // ---- Catalogue ----

    /**
     * Enumerate registered scan providers (key, persona, intensity).
     *
     * <p>GET /api/v1/security/scanners</p>
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listScanners() throws MockartyException {
        Map<String, Object> resp = client.get("/api/v1/security/scanners", Map.class);
        List<Map<String, Object>> out = new ArrayList<>();
        Object raw = resp == null ? null : resp.get("scanners");
        if (raw instanceof List) {
            List<?> rawList = (List<?>) raw;
            for (Object s : rawList) {
                if (s instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) s;
                    out.add((Map<String, Object>) m);
                }
            }
        }
        return out;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
