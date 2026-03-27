// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.PerfConfig;
import ru.mockarty.model.PerfResult;
import ru.mockarty.model.PerfSchedule;
import ru.mockarty.model.PerfTask;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for performance testing (load testing) operations.
 */
public class PerfApi {

    private final MockartyClient client;

    public PerfApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Runs a performance test.
     *
     * @param config the performance test configuration
     * @return the run metadata
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> run(Map<String, Object> config) throws MockartyException {
        return client.post("/api/v1/perf/run", config, Map.class);
    }

    /**
     * Stops a running performance test.
     *
     * @param taskId the task ID to stop
     */
    public void stop(String taskId) throws MockartyException {
        client.post("/api/v1/perf/stop/" + encode(taskId), null);
    }

    /**
     * Gets the results of a performance test run.
     *
     * @param id the result ID
     * @return the run results
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResult(String id) throws MockartyException {
        return client.get("/api/v1/perf-results/" + encode(id), Map.class);
    }

    /**
     * Lists all performance test results.
     *
     * @return list of result metadata
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listResults() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Map.class);
        return client.get("/api/v1/perf-results", listType);
    }

    /**
     * Compares performance test results.
     *
     * @param resultIds the result IDs to compare (passed as query params)
     * @return the comparison data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> compare(List<String> resultIds) throws MockartyException {
        StringBuilder query = new StringBuilder("/api/v1/perf-results/compare?");
        for (int i = 0; i < resultIds.size(); i++) {
            if (i > 0) {
                query.append("&");
            }
            query.append("id=").append(encode(resultIds.get(i)));
        }
        return client.get(query.toString(), Map.class);
    }

    // ---- Perf Configs ----

    /**
     * Lists all performance test configurations.
     *
     * @return list of configurations
     */
    public List<PerfConfig> listConfigs() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, PerfConfig.class);
        return client.get("/api/v1/perf-configs", listType);
    }

    /**
     * Gets a performance test configuration by ID.
     *
     * @param id the configuration ID
     * @return the configuration
     */
    public PerfConfig getConfig(String id) throws MockartyException {
        return client.get("/api/v1/perf-configs/" + encode(id), PerfConfig.class);
    }

    /**
     * Creates a new performance test configuration.
     *
     * @param config the configuration to create
     * @return the created configuration
     */
    public PerfConfig createConfig(PerfConfig config) throws MockartyException {
        return client.post("/api/v1/perf-configs", config, PerfConfig.class);
    }

    /**
     * Updates a performance test configuration.
     *
     * @param id     the configuration ID
     * @param config the updated configuration
     * @return the updated configuration
     */
    public PerfConfig updateConfig(String id, PerfConfig config) throws MockartyException {
        return client.put("/api/v1/perf-configs/" + encode(id), config, PerfConfig.class);
    }

    /**
     * Deletes a performance test configuration.
     *
     * @param id the configuration ID to delete
     */
    public void deleteConfig(String id) throws MockartyException {
        client.delete("/api/v1/perf-configs/" + encode(id));
    }

    // ---- Perf Schedules ----

    /**
     * Lists all performance test schedules.
     *
     * @return list of schedules
     */
    public List<PerfSchedule> listSchedules() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, PerfSchedule.class);
        return client.get("/api/v1/perf-schedules", listType);
    }

    /**
     * Creates a new performance test schedule.
     *
     * @param schedule the schedule to create
     * @return the created schedule
     */
    public PerfSchedule createSchedule(PerfSchedule schedule) throws MockartyException {
        return client.post("/api/v1/perf-schedules", schedule, PerfSchedule.class);
    }

    /**
     * Updates a performance test schedule.
     *
     * @param id       the schedule ID
     * @param schedule the updated schedule
     * @return the updated schedule
     */
    public PerfSchedule updateSchedule(String id, PerfSchedule schedule) throws MockartyException {
        return client.put("/api/v1/perf-schedules/" + encode(id), schedule, PerfSchedule.class);
    }

    /**
     * Deletes a performance test schedule.
     *
     * @param id the schedule ID to delete
     */
    public void deleteSchedule(String id) throws MockartyException {
        client.delete("/api/v1/perf-schedules/" + encode(id));
    }

    // ---- Perf Results (extended) ----

    /**
     * Gets the result history for a specific configuration.
     *
     * @param configId the configuration ID
     * @return list of results
     */
    public List<PerfResult> getResultHistory(String configId) throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, PerfResult.class);
        return client.get("/api/v1/perf-results/history/" + encode(configId), listType);
    }

    /**
     * Gets the performance trend for a specific configuration.
     *
     * @param configId the configuration ID
     * @return trend data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResultTrend(String configId) throws MockartyException {
        return client.get("/api/v1/perf-results/trend/" + encode(configId), Map.class);
    }

    /**
     * Deletes a performance test result.
     *
     * @param id the result ID to delete
     */
    public void deleteResult(String id) throws MockartyException {
        client.delete("/api/v1/perf-results/" + encode(id));
    }

    /**
     * Runs a performance test from a collection.
     *
     * @param request the run collection request
     * @return the started performance task
     */
    public PerfTask runCollection(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/perf/run-collection", request, PerfTask.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
