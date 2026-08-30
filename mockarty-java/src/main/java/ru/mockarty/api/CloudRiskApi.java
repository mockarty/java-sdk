package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Operator-only Cloud risk case and reversible enforcement API. */
public class CloudRiskApi {
    private static final String BASE = "/api/v1/cloud/operator/risk/cases";
    private final MockartyClient client;

    public CloudRiskApi(MockartyClient client) { this.client = client; }

    public List<JsonNode> listCases(String status, int limit) throws MockartyException {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        String path = BASE + "?limit=" + limit;
        if (status != null && !status.isBlank()) path += "&status=" + encode(status);
        JsonNode response = client.get(path, JsonNode.class);
        JsonNode cases = response == null ? null : response.path("cases");
        if (cases == null || !cases.isArray()) return Collections.emptyList();
        List<JsonNode> out = new ArrayList<>();
        cases.forEach(out::add);
        return out;
    }

    public JsonNode getCase(String caseId) throws MockartyException {
        return client.get(BASE + "/" + encode(require("case id", caseId)), JsonNode.class);
    }

    public JsonNode releaseEnforcement(String caseId, String enforcementId, long revision, String reason) throws MockartyException {
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
        if (reason == null) throw new IllegalArgumentException("release reason is required");
        String trimmedReason = reason.trim();
        int reasonLength = trimmedReason.codePointCount(0, trimmedReason.length());
        if (reasonLength < 3 || reasonLength > 512) throw new IllegalArgumentException("release reason must be 3-512 characters");
        String path = BASE + "/" + encode(require("case id", caseId)) + "/enforcements/"
                + encode(require("enforcement id", enforcementId)) + "/release";
        return client.postWithHeaders(path, Map.of("revision", revision, "reason", trimmedReason), JsonNode.class,
                Map.of("Idempotency-Key", releaseIdempotencyKey(caseId, enforcementId, revision, trimmedReason)));
    }

    private static String releaseIdempotencyKey(String caseId, String enforcementId, long revision, String reason) {
        String canonical = caseId + "\0" + enforcementId + "\0" + revision + "\0" + reason;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return "risk-release:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String require(String label, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
