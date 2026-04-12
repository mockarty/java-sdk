// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.ImportResult;
import ru.mockarty.model.RecorderEntry;
import ru.mockarty.model.RecorderSession;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for traffic recording operations.
 */
public class RecorderApi {

    private final MockartyClient client;

    public RecorderApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Starts a new recording session.
     *
     * @param config the recorder start configuration (name, targetUrl, namespace, etc.)
     * @return the created session
     */
    public RecorderSession start(Map<String, Object> config) throws MockartyException {
        return client.post("/api/v1/recorder/start", config, RecorderSession.class);
    }

    /**
     * Lists all recording sessions.
     *
     * @return list of sessions
     */
    public List<RecorderSession> listSessions() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, RecorderSession.class);
        return client.get("/api/v1/recorder/sessions", listType);
    }

    /**
     * Gets a recording session by ID.
     *
     * @param id the session ID
     * @return the session
     */
    public RecorderSession getSession(String id) throws MockartyException {
        return client.get("/api/v1/recorder/" + encode(id), RecorderSession.class);
    }

    /**
     * Stops recording on a session.
     *
     * @param id the session ID
     * @return the updated session
     */
    public RecorderSession stopRecording(String id) throws MockartyException {
        return client.post("/api/v1/recorder/" + encode(id) + "/stop", null, RecorderSession.class);
    }

    /**
     * Restarts a recording session (stops and starts it again).
     *
     * @param id the session ID
     * @return the restarted session
     */
    public RecorderSession restartRecording(String id) throws MockartyException {
        return client.post("/api/v1/recorder/" + encode(id) + "/restart", null, RecorderSession.class);
    }

    /**
     * Deletes a recording session and all its entries.
     *
     * @param id the session ID
     */
    public void deleteSession(String id) throws MockartyException {
        client.delete("/api/v1/recorder/" + encode(id));
    }

    /**
     * Gets all recorded entries for a session.
     *
     * @param id the session ID
     * @return list of recorded entries
     */
    public List<RecorderEntry> getEntries(String id) throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, RecorderEntry.class);
        return client.get("/api/v1/recorder/" + encode(id) + "/entries", listType);
    }

    /**
     * Creates mocks from recorded entries in a session.
     *
     * @param id      the session ID
     * @param options optional configuration for mock creation
     * @return the import result with created mock IDs
     */
    public ImportResult createMocks(String id, Map<String, Object> options) throws MockartyException {
        return client.post("/api/v1/recorder/" + encode(id) + "/mocks", options, ImportResult.class);
    }

    /**
     * Exports a recording session (e.g. as HAR).
     *
     * @param id the session ID
     * @return the exported data as bytes
     */
    public byte[] exportSession(String id) throws MockartyException {
        return client.postBytes("/api/v1/recorder/" + encode(id) + "/export", null);
    }

    /**
     * Creates mocks from recorded entries and exports with options.
     *
     * @param id      the session ID
     * @param options optional export configuration
     * @return the export result
     */
    public ImportResult export(String id, Map<String, Object> options) throws MockartyException {
        return client.post("/api/v1/recorder/" + encode(id) + "/export", options, ImportResult.class);
    }

    // ---- Recorder Configs ----

    /**
     * Lists all recorder configurations.
     *
     * @return list of config maps
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listConfigs() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Map.class);
        return client.get("/api/v1/recorder/configs", listType);
    }

    /**
     * Saves a recorder configuration.
     *
     * @param config the configuration to save
     */
    public void saveConfig(Map<String, Object> config) throws MockartyException {
        client.post("/api/v1/recorder/configs", config);
    }

    /**
     * Deletes a recorder configuration.
     *
     * @param id the configuration ID
     */
    public void deleteConfig(String id) throws MockartyException {
        client.delete("/api/v1/recorder/configs/" + encode(id));
    }

    /**
     * Exports a recorder configuration as bytes.
     *
     * @param id the configuration ID
     * @return the exported configuration data
     */
    public byte[] exportConfig(String id) throws MockartyException {
        return client.getBytes("/api/v1/recorder/configs/" + encode(id) + "/export");
    }

    // ---- CA (Certificate Authority) ----

    /**
     * Gets the CA certificate status.
     *
     * @return CA status
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCAStatus() throws MockartyException {
        return client.get("/api/v1/recorder/ca/status", Map.class);
    }

    /**
     * Generates a new CA certificate.
     *
     * @return generation result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateCA() throws MockartyException {
        return client.post("/api/v1/recorder/ca/generate", null, Map.class);
    }

    /**
     * Downloads the CA certificate.
     *
     * @return the CA certificate bytes
     */
    public byte[] downloadCA() throws MockartyException {
        return client.getBytes("/api/v1/recorder/ca/download");
    }

    // ---- Advanced Entry Operations ----

    /**
     * Annotates a recorded entry.
     *
     * @param sessionId  the session ID
     * @param entryId    the entry ID
     * @param annotation the annotation data
     */
    public void annotateEntry(String sessionId, String entryId, Map<String, Object> annotation) throws MockartyException {
        client.patch("/api/v1/recorder/" + encode(sessionId) + "/entries/" + encode(entryId), annotation);
    }

    /**
     * Replays a recorded entry.
     *
     * @param sessionId the session ID
     * @param entryId   the entry ID
     */
    public void replayEntry(String sessionId, String entryId) throws MockartyException {
        client.post("/api/v1/recorder/" + encode(sessionId) + "/entries/" + encode(entryId) + "/replay", null);
    }

    // ---- Session-level replay & correlation ----

    /**
     * Replays every (or selected) captured entry against a target.
     * <p>
     * Re-runs entries against either their original URL or a different
     * target (e.g. point a production capture at staging) and returns a
     * summary with match/mismatch/fail/skip counts.
     * <p>
     * Supported option keys (all optional):
     * <ul>
     *   <li>{@code targetUrl} (String) — base URL to replay against</li>
     *   <li>{@code concurrency} (int) — parallel replay workers</li>
     *   <li>{@code timeoutMs} (int) — per-request timeout in ms</li>
     *   <li>{@code entryIds} (List&lt;String&gt;) — replay only these entry IDs</li>
     *   <li>{@code includeNonHttp} (bool) — include WS/SSE entries</li>
     *   <li>{@code followRedirects} (bool) — follow HTTP redirects</li>
     * </ul>
     *
     * @param sessionId the session ID
     * @param options   replay options (may be null)
     * @return replay summary as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> replaySession(String sessionId, Map<String, Object> options) throws MockartyException {
        return client.post("/api/v1/recorder/" + encode(sessionId) + "/replay",
                options == null ? Map.of() : options, Map.class);
    }

    /**
     * Discovers dynamic-value flow between captured entries.
     * <p>
     * Runs the deterministic value-matching correlation engine: scans
     * each entry's response (JSON, headers, Set-Cookie) for values that
     * are then re-used by a later entry's request (URL, header, body,
     * form, cookie). The output highlights tokens, IDs and CSRF values
     * that need to be extracted at runtime.
     * <p>
     * Supported option keys (all optional):
     * <ul>
     *   <li>{@code minValueLength} (int)</li>
     *   <li>{@code maxValueLength} (int)</li>
     *   <li>{@code excludeNumeric} (bool)</li>
     *   <li>{@code maxCorrelationsPerSource} (int)</li>
     *   <li>{@code entryIds} (List&lt;String&gt;)</li>
     * </ul>
     *
     * @param sessionId the session ID
     * @param options   correlation options (may be null)
     * @return correlation report as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> correlateSession(String sessionId, Map<String, Object> options) throws MockartyException {
        return client.post("/api/v1/recorder/" + encode(sessionId) + "/correlate",
                options == null ? Map.of() : options, Map.class);
    }

    // ---- Modifications ----

    /**
     * Gets request/response modifications for a session.
     *
     * @param sessionId the session ID
     * @return modifications data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getModifications(String sessionId) throws MockartyException {
        return client.get("/api/v1/recorder/" + encode(sessionId) + "/modifications", Map.class);
    }

    /**
     * Updates request/response modifications for a session.
     *
     * @param sessionId     the session ID
     * @param modifications the modifications data
     */
    public void updateModifications(String sessionId, Map<String, Object> modifications) throws MockartyException {
        client.put("/api/v1/recorder/" + encode(sessionId) + "/modifications", modifications);
    }

    // ---- Ports ----

    /**
     * Gets the currently used recorder ports.
     *
     * @return ports data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPorts() throws MockartyException {
        return client.get("/api/v1/recorder/ports", Map.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
