// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Admitted repositories, delivery targets, and deployment-bearing coder missions. */
public class CoderDeliveryApi {
    private static final int MAX_MATERIAL_BYTES = 16 * 1024 * 1024;
    private static final int MAX_TEXT_MATERIAL_BYTES = 64 * 1024;
    private static final int MAX_MATERIAL_NAME_BYTES = 128;
    private static final Set<String> TEXT_MATERIAL_TYPES = Set.of(
            "text/plain", "text/markdown", "text/css", "text/javascript", "application/json",
            "application/yaml", "application/xml", "text/html");
    private static final Set<String> BINARY_MATERIAL_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif", "application/pdf");

    private final MockartyClient client;

    public CoderDeliveryApi(MockartyClient client) { this.client = client; }

    /** Upload an original and retain the returned reference for mission artifacts. */
    public Map<String, Object> uploadMissionMaterial(String namespace, String productId, String filename,
                                                     byte[] content, String mediaType) throws MockartyException {
        if (blank(productId) || content == null || content.length == 0 || content.length > MAX_MATERIAL_BYTES) {
            throw new IllegalArgumentException("product, filename and a nonempty material up to 16 MiB are required");
        }
        requireValidMaterialName(filename);
        if (blank(namespace)) namespace = client.getConfig().getNamespace();
        String mt = mediaType == null ? "" : mediaType.trim();
        if (blank(mt) || mt.equalsIgnoreCase("application/octet-stream")) {
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
            switch (extension) {
                case "md": mt = "text/markdown"; break;
                case "yaml": case "yml": mt = "application/yaml"; break;
                case "js": mt = "text/javascript"; break;
                default: mt = java.net.URLConnection.guessContentTypeFromName(filename);
            }
            if (blank(mt)) throw new IllegalArgumentException("unknown material type; specify mediaType");
        }
        if (mt.codePoints().anyMatch(codePoint -> Character.getType(codePoint) == Character.CONTROL)) {
            throw new IllegalArgumentException("invalid media type");
        }
        String mediaBase = mt.split(";", 2)[0].trim().toLowerCase(java.util.Locale.ROOT);
        if (!TEXT_MATERIAL_TYPES.contains(mediaBase) && !BINARY_MATERIAL_TYPES.contains(mediaBase)) {
            throw new IllegalArgumentException("unsupported mission material type");
        }
        if (TEXT_MATERIAL_TYPES.contains(mediaBase) && content.length > MAX_TEXT_MATERIAL_BYTES) {
            throw new IllegalArgumentException("text mission materials must be at most 64 KiB combined");
        }
        String boundary = "Mockarty" + java.util.UUID.randomUUID().toString();
        String safeName = filename.replace("\\", "\\\\").replace("\"", "\\\"");
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        try {
            buf.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + safeName
                    + "\"\r\nContent-Type: " + mt + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            buf.write(content);
            buf.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) { throw new MockartyException("build mission material upload", e); }
        return client.postRawNoRetry("/api/v1/missions/materials?namespace=" + URLEncoder.encode(namespace, StandardCharsets.UTF_8)
                + "&productId=" + URLEncoder.encode(productId, StandardCharsets.UTF_8), buf.toByteArray(),
                "multipart/form-data; boundary=" + boundary, Map.class);
    }

    public Map<String, Object> getConfig(String productId) throws MockartyException {
        return client.get(configPath(productId), Map.class);
    }

    public Map<String, Object> putConfig(Map<String, Object> config) throws MockartyException {
        return client.put(configPath(""), config, Map.class);
    }

    public void deleteConfig(String productId) throws MockartyException {
        client.delete(configPath(productId));
    }

    public Map<String, Object> startMission(Map<String, Object> request) throws MockartyException {
        if (request == null || blank(request.get("goal")) || blank(request.get("repoUrl"))) {
            throw new IllegalArgumentException("goal and repoUrl are required");
        }
        return client.post(missionsPath(), request, Map.class);
    }

    public Map<String, Object> listMissions() throws MockartyException {
        return client.get(missionsPath(), Map.class);
    }

    public Map<String, Object> getMission(String missionId) throws MockartyException {
        return client.get(missionPath(missionId), Map.class);
    }

    public Map<String, Object> approveMission(String missionId, boolean approve) throws MockartyException {
        return client.post(missionPath(missionId, "/approve"), Map.of("approve", approve), Map.class);
    }

    public Map<String, Object> addToMission(String missionId, Map<String, Object> request) throws MockartyException {
        if (request == null || (!nonEmptyCollection(request.get("tasks")) && !nonEmptyCollection(request.get("prompts")))) {
            throw new IllegalArgumentException("at least one task or prompt is required");
        }
        return client.postNoRetry(missionPath(missionId, "/add"), request, Map.class);
    }

    public Map<String, Object> reconcileDeploy(String missionId, String outcome) throws MockartyException {
        String normalized = outcome == null ? "" : outcome.trim();
        if (!normalized.equals("applied") && !normalized.equals("not_applied")) {
            throw new IllegalArgumentException("outcome must be applied or not_applied");
        }
        return client.post(missionPath(missionId, "/deploy-outcome"), Map.of("outcome", normalized), Map.class);
    }

    /** Lists deployed-system observation sources bound to this client's namespace. */
    public Map<String, Object> observabilitySources() throws MockartyException {
        return client.get(observabilityPath("sources"), Map.class);
    }

    /** Runs one bounded read and returns frozen observation evidence. */
    public Map<String, Object> queryObservability(Map<String, Object> query) throws MockartyException {
        if (query == null || blank(query.get("source")) || blank(query.get("expression"))) {
            throw new IllegalArgumentException("source and expression are required");
        }
        return client.post(observabilityPath("query"), query, Map.class);
    }

    private String configPath(String productId) {
        String path = "/api/v1/coder/delivery-config?namespace=" + enc(client.getConfig().getNamespace());
        if (productId != null && !productId.isBlank()) path += "&productId=" + enc(productId.trim());
        return path;
    }

    private String missionsPath() {
        return "/api/v1/coder/missions?namespace=" + enc(client.getConfig().getNamespace());
    }

    private String observabilityPath(String suffix) {
        return "/api/v1/observability/" + suffix + "?namespace=" + enc(client.getConfig().getNamespace());
    }

    private String missionPath(String missionId) {
        return missionPath(missionId, "");
    }

    private String missionPath(String missionId, String suffix) {
        if (missionId == null || missionId.isBlank()) throw new IllegalArgumentException("mission id is required");
        return "/api/v1/coder/missions/" + enc(missionId.trim()) + suffix + "?namespace=" + enc(client.getConfig().getNamespace());
    }

    private static boolean blank(Object value) { return value == null || value.toString().isBlank(); }
    private static void requireValidMaterialName(String filename) {
        boolean invalid = blank(filename)
                || hasOuterUnicodeSpace(filename)
                || filename.equals(".")
                || filename.equals("..")
                || filename.indexOf('/') >= 0
                || filename.indexOf('\\') >= 0
                || filename.length() > MAX_MATERIAL_NAME_BYTES
                || !StandardCharsets.UTF_8.newEncoder().canEncode(filename)
                || filename.getBytes(StandardCharsets.UTF_8).length > MAX_MATERIAL_NAME_BYTES
                || filename.codePoints().anyMatch(codePoint -> Character.getType(codePoint) == Character.CONTROL);
        if (invalid) throw new IllegalArgumentException("invalid mission material filename");
    }
    private static boolean hasOuterUnicodeSpace(String value) {
        int first = value.codePointAt(0);
        int last = value.codePointBefore(value.length());
        return isUnicodeSpace(first) || isUnicodeSpace(last);
    }
    private static boolean isUnicodeSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || codePoint == 0x85;
    }
    private static boolean nonEmptyCollection(Object value) {
        return value instanceof Collection<?> && !((Collection<?>) value).isEmpty();
    }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
}
