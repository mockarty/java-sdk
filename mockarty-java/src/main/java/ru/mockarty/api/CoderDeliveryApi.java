// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Admitted repositories, delivery targets, and deployment-bearing coder missions. */
public class CoderDeliveryApi {
    private final MockartyClient client;

    public CoderDeliveryApi(MockartyClient client) { this.client = client; }

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

    public Map<String, Object> reconcileDeploy(String missionId, String outcome) throws MockartyException {
        String normalized = outcome == null ? "" : outcome.trim();
        if (!normalized.equals("applied") && !normalized.equals("not_applied")) {
            throw new IllegalArgumentException("outcome must be applied or not_applied");
        }
        return client.post(missionPath(missionId, "/deploy-outcome"), Map.of("outcome", normalized), Map.class);
    }

    private String configPath(String productId) {
        String path = "/api/v1/coder/delivery-config?namespace=" + enc(client.getConfig().getNamespace());
        if (productId != null && !productId.isBlank()) path += "&productId=" + enc(productId.trim());
        return path;
    }

    private String missionsPath() {
        return "/api/v1/coder/missions?namespace=" + enc(client.getConfig().getNamespace());
    }

    private String missionPath(String missionId) {
        return missionPath(missionId, "");
    }

    private String missionPath(String missionId, String suffix) {
        if (missionId == null || missionId.isBlank()) throw new IllegalArgumentException("mission id is required");
        return "/api/v1/coder/missions/" + enc(missionId.trim()) + suffix + "?namespace=" + enc(client.getConfig().getNamespace());
    }

    private static boolean blank(Object value) { return value == null || value.toString().isBlank(); }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
}
