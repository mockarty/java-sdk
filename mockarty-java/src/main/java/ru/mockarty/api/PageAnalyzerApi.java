// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** HTTP-level Page Analyzer lifecycle. */
public final class PageAnalyzerApi {
    private static final String BASE = "/api/v1/page-analyzer";
    private final MockartyClient client;

    public PageAnalyzerApi(MockartyClient client) { this.client = client; }

    public Map<String, Object> listConfigs() throws MockartyException {
        return map(client.get(path("configs"), Map.class));
    }

    public Map<String, Object> saveConfig(Map<String, Object> body) throws MockartyException {
        return map(client.post(path("configs"), body, Map.class));
    }

    public Map<String, Object> updateConfig(String configId, Map<String, Object> body) throws MockartyException {
        return map(client.put(path("configs/" + encode(require(configId, "config id"))), body, Map.class));
    }

    public void deleteConfig(String configId) throws MockartyException {
        client.delete(path("configs/" + encode(require(configId, "config id"))));
    }

    public Map<String, Object> run(Map<String, Object> body) throws MockartyException {
        if (body == null || (blank(body.get("targetUrl")) && blank(body.get("configId")))) {
            throw new IllegalArgumentException("page analyzer targetUrl or configId is required");
        }
        return map(client.post(path("run"), body, Map.class));
    }

    public Map<String, Object> listResults(int limit, int offset) throws MockartyException {
        StringBuilder suffix = new StringBuilder("results");
        appendQuery(suffix, "limit", limit > 0 ? Integer.toString(limit) : "");
        appendQuery(suffix, "offset", offset > 0 ? Integer.toString(offset) : "");
        return map(client.get(pathWithExistingQuery(suffix.toString()), Map.class));
    }

    public Map<String, Object> getResult(String resultId) throws MockartyException {
        return map(client.get(path("results/" + encode(require(resultId, "result id"))), Map.class));
    }

    public void deleteResult(String resultId) throws MockartyException {
        client.delete(path("results/" + encode(require(resultId, "result id"))));
    }

    public Map<String, Object> analyzeWithAI(String resultId, Map<String, Object> body) throws MockartyException {
        return map(client.post(path("results/" + encode(require(resultId, "result id")) + "/ai-analyze"),
                body == null ? Map.of() : new LinkedHashMap<>(body), Map.class));
    }

    private String path(String suffix) {
        return BASE + "/" + suffix + "?namespace=" + encode(client.getConfig().getNamespace());
    }

    private String pathWithExistingQuery(String suffix) {
        return BASE + "/" + suffix + (suffix.contains("?") ? "&" : "?")
                + "namespace=" + encode(client.getConfig().getNamespace());
    }

    private static void appendQuery(StringBuilder value, String key, String item) {
        if (item == null || item.isBlank()) return;
        value.append(value.indexOf("?") < 0 ? '?' : '&').append(key).append('=').append(encode(item));
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("page analyzer " + name + " is required");
        return value;
    }

    private static boolean blank(Object value) {
        return value == null || value.toString().isBlank();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map value) { return value == null ? Map.of() : value; }
}
