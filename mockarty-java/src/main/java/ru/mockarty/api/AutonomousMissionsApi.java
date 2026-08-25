// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.AutonomousMission;
import ru.mockarty.model.AutonomousMissionFlow;
import ru.mockarty.model.AutonomousMissionListResponse;
import ru.mockarty.model.AutonomousMissionSubmitRequest;
import ru.mockarty.model.AutonomousMissionSubmitResponse;
import ru.mockarty.model.MissionEffectiveSettings;
import ru.mockarty.model.MissionEffectiveSettingsOptions;
import ru.mockarty.model.MissionStartRequest;
import ru.mockarty.model.MissionStartResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Submit and supervise durable autonomous testing missions. */
public class AutonomousMissionsApi {
    private static final String MISSIONS = "/api/v1/autotester/missions";
    private final MockartyClient client;

    public AutonomousMissionsApi(MockartyClient client) { this.client = client; }

    public AutonomousMissionSubmitResponse submit(AutonomousMissionSubmitRequest request) throws MockartyException {
        if (request == null || request.getGoal() == null || request.getGoal().trim().isEmpty()) {
            throw new IllegalArgumentException("goal is required");
        }
        String autonomy = request.getAutonomy();
        if (autonomy != null && !autonomy.isBlank()
                && !autonomy.equals("recon") && !autonomy.equals("propose") && !autonomy.equals("auto")) {
            throw new IllegalArgumentException("autonomy must be recon, propose, or auto");
        }
        return client.post("/api/v1/autotester/intents", request, AutonomousMissionSubmitResponse.class);
    }

    public AutonomousMissionListResponse list(String status, int limit) throws MockartyException {
        if (limit < 0 || limit > 200) throw new IllegalArgumentException("limit must be from 0 to 200");
        StringBuilder path = new StringBuilder(MISSIONS);
        String separator = "?";
        if (status != null && !status.isBlank()) {
            path.append(separator).append("status=").append(enc(status.trim()));
            separator = "&";
        }
        if (limit > 0) path.append(separator).append("limit=").append(limit);
        return client.get(path.toString(), AutonomousMissionListResponse.class);
    }

    public AutonomousMission get(String missionId) throws MockartyException {
        return client.get(missionPath(missionId), AutonomousMission.class);
    }

    public AutonomousMissionFlow getFlow(String missionId) throws MockartyException {
        return client.get(missionPath(missionId) + "/flow", AutonomousMissionFlow.class);
    }

    /** Preview the exact layered settings that a unified mission would snapshot. */
    public MissionEffectiveSettings getEffectiveSettings(MissionEffectiveSettingsOptions options) throws MockartyException {
        MissionEffectiveSettingsOptions selected = options == null ? new MissionEffectiveSettingsOptions() : options;
        Integer runWindow = selected.getRunWindowMinutes();
        if (runWindow != null && (runWindow < 1 || runWindow > 20160)) {
            throw new IllegalArgumentException("run window must be from 1 to 20160");
        }
        StringBuilder path = new StringBuilder("/api/v1/missions/settings/effective");
        String separator = "?";
        if (selected.getProductId() != null && !selected.getProductId().isBlank()) {
            path.append(separator).append("productId=").append(enc(selected.getProductId().trim()));
            separator = "&";
        }
        if (selected.getMissionId() != null && !selected.getMissionId().isBlank()) {
            path.append(separator).append("missionId=").append(enc(selected.getMissionId().trim()));
            separator = "&";
        }
        if (runWindow != null) path.append(separator).append("runWindowMinutes=").append(runWindow);
        return client.get(path.toString(), MissionEffectiveSettings.class);
    }

    /** Start a mission in the unified ledger, optionally fenced by a reviewed digest. */
    public MissionStartResponse start(MissionStartRequest request) throws MockartyException {
        if (request == null || request.getGoal() == null || request.getGoal().isBlank()) {
            throw new IllegalArgumentException("goal is required");
        }
        String digest = request.getExpectedSettingsDigest();
        if (digest != null && !digest.isBlank() && !digest.matches("sha256:[0-9a-f]{64}")) {
            throw new IllegalArgumentException("expected settings digest must be canonical sha256");
        }
        return client.post("/api/v1/missions", request, MissionStartResponse.class);
    }

    private static String missionPath(String missionId) {
        if (missionId == null || missionId.isBlank()) throw new IllegalArgumentException("mission id is required");
        return MISSIONS + "/" + enc(missionId.trim()).replace("+", "%20");
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
