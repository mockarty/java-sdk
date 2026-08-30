package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CloudInstance;
import ru.mockarty.model.CloudInstanceCreateResult;
import ru.mockarty.model.CloudInstancesPage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dedicated Mockarty Cloud contour lifecycle. */
public class CloudInstancesApi {
    private final MockartyClient client;

    public CloudInstancesApi(MockartyClient client) { this.client = client; }

    public CloudInstancesPage list(String workspaceId) throws MockartyException {
        return client.get("/api/v1/cloud/instances?workspace_id=" + encode(require("workspace id", workspaceId)), CloudInstancesPage.class);
    }

    public CloudInstance get(String instanceId) throws MockartyException {
        InstanceEnvelope response = client.get(instancePath(instanceId), InstanceEnvelope.class);
        return response == null ? null : response.instance;
    }

    /** The bootstrap password is returned by the server on first admission only. */
    public CloudInstanceCreateResult create(String workspaceId, String name, String idempotencyKey) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("workspace_id", require("workspace id", workspaceId));
        body.put("name", require("name", name));
        return client.postWithHeaders("/api/v1/cloud/instances", body, CloudInstanceCreateResult.class,
                headers(idempotencyKey));
    }

    public void delete(String instanceId, String idempotencyKey) throws MockartyException {
        client.deleteWithHeaders(instancePath(instanceId), headers(idempotencyKey));
    }

    public void start(String instanceId, String idempotencyKey) throws MockartyException {
        mutate(instanceId, "start", idempotencyKey);
    }

    public void stop(String instanceId, String idempotencyKey) throws MockartyException {
        mutate(instanceId, "stop", idempotencyKey);
    }

    private void mutate(String instanceId, String action, String idempotencyKey) throws MockartyException {
        client.postWithHeaders(instancePath(instanceId) + "/" + action, Collections.emptyMap(), Map.class,
                headers(idempotencyKey));
    }

    private static String instancePath(String instanceId) {
        return "/api/v1/cloud/instances/" + encode(require("instance id", instanceId));
    }

    private static Map<String, String> headers(String idempotencyKey) {
        return Map.of("Idempotency-Key", require("idempotency key", idempotencyKey));
    }

    private static String require(String label, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class InstanceEnvelope {
        private CloudInstance instance;
        public void setInstance(CloudInstance value) { instance = value; }
    }
}
