// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.FuzzingConfig;
import ru.mockarty.model.FuzzingFinding;
import ru.mockarty.model.FuzzingQuarantinePage;
import ru.mockarty.model.FuzzingResult;
import ru.mockarty.model.FuzzingRun;
import ru.mockarty.model.FuzzingSchedule;
import ru.mockarty.model.QuarantineEntry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for fuzzing (security and robustness testing) operations.
 */
public class FuzzingApi {

    private final MockartyClient client;

    public FuzzingApi(MockartyClient client) {
        this.client = client;
    }

    // ---- Fuzzing Configs ----

    /**
     * Creates a new fuzzing configuration.
     *
     * @param config the fuzzing configuration
     * @return the created configuration
     */
    public FuzzingConfig createConfig(FuzzingConfig config) throws MockartyException {
        return client.post("/api/v1/fuzzing/configs", config, FuzzingConfig.class);
    }

    /**
     * Gets a fuzzing configuration by ID.
     *
     * @param configId the configuration ID
     * @return the fuzzing configuration
     */
    public FuzzingConfig getConfig(String configId) throws MockartyException {
        return client.get("/api/v1/fuzzing/configs/" + encode(configId), FuzzingConfig.class);
    }

    /**
     * Lists all fuzzing configurations.
     *
     * @return list of fuzzing configurations
     */
    public List<FuzzingConfig> listConfigs() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, FuzzingConfig.class);
        return client.get("/api/v1/fuzzing/configs", listType);
    }

    /**
     * Updates a fuzzing configuration.
     *
     * @param configId the configuration ID to update
     * @param config   the updated configuration
     * @return the updated configuration
     */
    public FuzzingConfig updateConfig(String configId, FuzzingConfig config) throws MockartyException {
        return client.put("/api/v1/fuzzing/configs/" + encode(configId), config, FuzzingConfig.class);
    }

    /**
     * Deletes a fuzzing configuration.
     *
     * @param configId the configuration ID to delete
     */
    public void deleteConfig(String configId) throws MockartyException {
        client.delete("/api/v1/fuzzing/configs/" + encode(configId));
    }

    // ---- Fuzzing Runs ----

    /**
     * Starts a new fuzzing run using the given configuration ID.
     *
     * @param configId the fuzzing configuration ID
     * @return the started fuzzing run
     */
    public FuzzingRun start(String configId) throws MockartyException {
        return client.post("/api/v1/fuzzing/run", Map.of("configId", configId), FuzzingRun.class);
    }

    /**
     * Stops an active fuzzing run.
     *
     * @param runId the run ID to stop
     */
    public void stop(String runId) throws MockartyException {
        client.post("/api/v1/fuzzing/run/" + encode(runId) + "/stop", null);
    }

    // ---- Fuzzing Results ----

    /**
     * Gets the result of a fuzzing run.
     *
     * @param resultId the result ID
     * @return the fuzzing result
     */
    public FuzzingResult getResult(String resultId) throws MockartyException {
        return client.get("/api/v1/fuzzing/results/" + encode(resultId), FuzzingResult.class);
    }

    /**
     * Lists all fuzzing results.
     *
     * @return list of fuzzing results
     */
    public List<FuzzingResult> listResults() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, FuzzingResult.class);
        return client.get("/api/v1/fuzzing/results", listType);
    }

    /**
     * Deletes a fuzzing result.
     *
     * @param resultId the result ID to delete
     */
    public void deleteResult(String resultId) throws MockartyException {
        client.delete("/api/v1/fuzzing/results/" + encode(resultId));
    }

    // ---- Summary ----

    /**
     * Gets a summary of all fuzzing activity.
     *
     * @return summary data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSummary() throws MockartyException {
        return client.get("/api/v1/fuzzing/summary", Map.class);
    }

    /**
     * Runs a quick fuzz test without saving a configuration.
     *
     * @param request the quick fuzz request parameters
     * @return the started fuzzing run
     */
    public FuzzingRun quickFuzz(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/fuzzing/quick-fuzz", request, FuzzingRun.class);
    }

    // ---- Findings ----

    /**
     * Lists all fuzzing findings.
     *
     * @return list of findings
     */
    public List<FuzzingFinding> listFindings() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, FuzzingFinding.class);
        return client.get("/api/v1/fuzzing/findings", listType);
    }

    /**
     * Gets a specific fuzzing finding.
     *
     * @param id the finding ID
     * @return the finding
     */
    public FuzzingFinding getFinding(String id) throws MockartyException {
        return client.get("/api/v1/fuzzing/findings/" + encode(id), FuzzingFinding.class);
    }

    /**
     * Triages a fuzzing finding.
     *
     * @param id     the finding ID
     * @param status the triage status
     * @param notes  triage notes
     */
    public void triageFinding(String id, String status, String notes) throws MockartyException {
        Map<String, Object> body = Map.of(
                "status", status,
                "notes", notes
        );
        client.put("/api/v1/fuzzing/findings/" + encode(id) + "/triage", body);
    }

    /**
     * Replays a fuzzing finding.
     *
     * @param id the finding ID
     */
    public void replayFinding(String id) throws MockartyException {
        client.post("/api/v1/fuzzing/findings/" + encode(id) + "/replay", null);
    }

    /**
     * Analyzes a fuzzing finding using AI.
     *
     * @param id the finding ID
     * @return analysis results
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeFinding(String id) throws MockartyException {
        return client.post("/api/v1/fuzzing/findings/" + encode(id) + "/analyze", null, Map.class);
    }

    /**
     * Batch analyzes multiple fuzzing findings.
     *
     * @param ids the finding IDs to analyze
     */
    public void batchAnalyze(List<String> ids) throws MockartyException {
        client.post("/api/v1/fuzzing/findings/batch-analyze", Map.of("ids", ids));
    }

    /**
     * Batch triages multiple fuzzing findings.
     *
     * @param ids    the finding IDs
     * @param status the triage status to set
     */
    public void batchTriage(List<String> ids, String status) throws MockartyException {
        client.post("/api/v1/fuzzing/findings/batch-triage", Map.of("ids", ids, "status", status));
    }

    /**
     * Batch applies a manual triage status to multiple fuzzing findings.
     *
     * @param ids    the finding IDs to triage
     * @param status the triage status (new, confirmed, false_positive, fixed, accepted, quarantined)
     * @return a map containing the "updated" count
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> batchManualTriage(List<String> ids, String status) throws MockartyException {
        return batchManualTriage(ids, status, null);
    }

    /**
     * Batch applies a manual triage status to multiple fuzzing findings with an optional note.
     *
     * @param ids    the finding IDs to triage
     * @param status the triage status (new, confirmed, false_positive, fixed, accepted, quarantined)
     * @param note   optional note for the triage action
     * @return a map containing the "updated" count
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> batchManualTriage(List<String> ids, String status, String note) throws MockartyException {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("ids", ids);
        body.put("status", status);
        if (note != null) {
            body.put("note", note);
        }
        return client.post("/api/v1/fuzzing/findings/batch-manual-triage", body, Map.class);
    }

    /**
     * Batch deletes multiple fuzzing findings.
     *
     * @param ids the finding IDs to delete
     * @return a map containing the "deleted" count
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> batchDeleteFindings(List<String> ids) throws MockartyException {
        return client.delete("/api/v1/fuzzing/findings/batch", Map.of("ids", ids), Map.class);
    }

    /**
     * Exports fuzzing findings.
     *
     * @param request export parameters
     * @return exported data as bytes
     */
    public byte[] exportFindings(Map<String, Object> request) throws MockartyException {
        return client.postBytes("/api/v1/fuzzing/findings/export", request);
    }

    // ---- Imports ----

    /**
     * Imports a fuzzing target from a cURL command.
     *
     * @param curl the cURL command string
     */
    public void importFromCurl(String curl) throws MockartyException {
        client.post("/api/v1/fuzzing/import/curl", Map.of("curl", curl));
    }

    /**
     * Imports fuzzing targets from an OpenAPI specification.
     *
     * @param data the OpenAPI import data
     */
    public void importFromOpenAPI(Map<String, Object> data) throws MockartyException {
        client.post("/api/v1/fuzzing/import/openapi", data);
    }

    /**
     * Imports fuzzing targets from a test collection.
     *
     * @param data the collection import data
     */
    public void importFromCollection(Map<String, Object> data) throws MockartyException {
        client.post("/api/v1/fuzzing/import/collection", data);
    }

    /**
     * Imports fuzzing targets from a recorder session.
     *
     * @param data the recorder import data
     */
    public void importFromRecorder(Map<String, Object> data) throws MockartyException {
        client.post("/api/v1/fuzzing/import/recorder", data);
    }

    /**
     * Imports fuzzing targets from an existing mock.
     *
     * @param data the mock import data
     */
    public void importFromMock(Map<String, Object> data) throws MockartyException {
        client.post("/api/v1/fuzzing/import/mock", data);
    }

    // ---- Schedules ----

    /**
     * Lists all fuzzing schedules.
     *
     * @return list of schedules
     */
    public List<FuzzingSchedule> listSchedules() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, FuzzingSchedule.class);
        return client.get("/api/v1/fuzzing/schedules", listType);
    }

    /**
     * Gets a fuzzing schedule by ID.
     *
     * @param id the schedule ID
     * @return the fuzzing schedule
     */
    public FuzzingSchedule getSchedule(String id) throws MockartyException {
        return client.get("/api/v1/fuzzing/schedules/" + encode(id), FuzzingSchedule.class);
    }

    /**
     * Creates a new fuzzing schedule.
     *
     * @param schedule the schedule to create
     * @return the created schedule
     */
    public FuzzingSchedule createSchedule(FuzzingSchedule schedule) throws MockartyException {
        return client.post("/api/v1/fuzzing/schedules", schedule, FuzzingSchedule.class);
    }

    /**
     * Updates a fuzzing schedule.
     *
     * @param id       the schedule ID
     * @param schedule the updated schedule data
     * @return the updated schedule
     */
    public FuzzingSchedule updateSchedule(String id, FuzzingSchedule schedule) throws MockartyException {
        return client.put("/api/v1/fuzzing/schedules/" + encode(id), schedule, FuzzingSchedule.class);
    }

    /**
     * Deletes a fuzzing schedule.
     *
     * @param id the schedule ID to delete
     */
    public void deleteSchedule(String id) throws MockartyException {
        client.delete("/api/v1/fuzzing/schedules/" + encode(id));
    }

    // ---- Quarantine ----

    /**
     * Lists quarantine entries with pagination.
     *
     * @param limit  maximum number of entries to return
     * @param offset pagination offset
     * @return paginated quarantine entries
     */
    public FuzzingQuarantinePage listQuarantine(int limit, int offset) throws MockartyException {
        String query = "?limit=" + limit + "&offset=" + offset;
        return client.get("/api/v1/fuzzing/quarantine" + query, FuzzingQuarantinePage.class);
    }

    /**
     * Lists quarantine entries with default pagination (limit=50, offset=0).
     *
     * @return paginated quarantine entries
     */
    public FuzzingQuarantinePage listQuarantine() throws MockartyException {
        return listQuarantine(50, 0);
    }

    /**
     * Creates a new quarantine entry.
     *
     * @param entry the quarantine entry to create
     * @return the created quarantine entry
     */
    public QuarantineEntry createQuarantine(QuarantineEntry entry) throws MockartyException {
        return client.post("/api/v1/fuzzing/quarantine", entry, QuarantineEntry.class);
    }

    /**
     * Deletes a quarantine entry by ID.
     *
     * @param id the quarantine entry ID to delete
     */
    public void deleteQuarantine(String id) throws MockartyException {
        client.delete("/api/v1/fuzzing/quarantine/" + encode(id));
    }

    /**
     * Batch deletes multiple quarantine entries.
     *
     * @param ids the quarantine entry IDs to delete
     * @return a map containing the "deleted" count
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> batchDeleteQuarantine(List<String> ids) throws MockartyException {
        return client.post("/api/v1/fuzzing/quarantine/batch-delete", Map.of("ids", ids), Map.class);
    }

    /**
     * Creates a quarantine entry from an existing finding.
     * Computes the fingerprint from the finding and adds it to quarantine,
     * then triages the finding as "false_positive".
     *
     * @param findingId the finding ID to quarantine
     * @param reason    the reason for quarantining
     * @return the created quarantine entry
     */
    public QuarantineEntry quarantineFinding(String findingId, String reason) throws MockartyException {
        return client.post("/api/v1/fuzzing/quarantine/from-finding",
                Map.of("findingId", findingId, "reason", reason), QuarantineEntry.class);
    }

    /**
     * Batch creates quarantine entries from multiple findings.
     * Computes fingerprints from each finding and adds them to quarantine,
     * then triages the findings as "false_positive".
     *
     * @param findingIds the finding IDs to quarantine
     * @param reason     the reason for quarantining
     * @return a map containing "created", "triaged", and "failed" counts
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> batchQuarantineFindings(List<String> findingIds, String reason) throws MockartyException {
        return client.post("/api/v1/fuzzing/quarantine/from-findings",
                Map.of("findingIds", findingIds, "reason", reason), Map.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
