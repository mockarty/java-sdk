package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CloudSharedProject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Identity, authorization and Shared project workload API for one msa_ credential. */
public class CloudServiceAccountApi {
    private final MockartyClient client;

    public CloudServiceAccountApi(MockartyClient client) { this.client = client; }

    public Self self() throws MockartyException {
        return client.get("/api/v1/cloud/service-account/self", Self.class);
    }

    public Authorization authorize(String spaceId, String capability) throws MockartyException {
        if (capability == null || capability.isBlank()) throw new IllegalArgumentException("capability is required");
        return client.get(root(spaceId) + "/authorize?capability=" + encode(capability), Authorization.class);
    }

    public CloudSharedProjectsApi.Page listProjects(String spaceId, String cursor, int limit) throws MockartyException {
        int bounded = limit < 1 || limit > 100 ? 50 : limit;
        String path = projects(spaceId, "") + "?limit=" + bounded;
        if (cursor != null && !cursor.isBlank()) path += "&cursor=" + encode(cursor);
        CloudSharedProjectsApi.Page page = client.get(path, CloudSharedProjectsApi.Page.class);
        return page == null ? new CloudSharedProjectsApi.Page() : page;
    }

    public CloudSharedProject getProject(String spaceId, String projectId) throws MockartyException {
        return client.get(projects(spaceId, projectId), CloudSharedProject.class);
    }

    public CloudSharedProject createProject(String spaceId, String name, JsonNode body,
                                            String idempotencyKey) throws MockartyException {
        return client.postWithHeaders(projects(spaceId, ""), mutation(name, body, 0),
                CloudSharedProject.class, headers(idempotencyKey));
    }

    public CloudSharedProject updateProject(String spaceId, String projectId, String name, JsonNode body,
                                            long revision, String idempotencyKey) throws MockartyException {
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
        return client.putWithHeaders(projects(spaceId, projectId), mutation(name, body, revision),
                CloudSharedProject.class, headers(idempotencyKey));
    }

    public void deleteProject(String spaceId, String projectId, long revision,
                              String idempotencyKey) throws MockartyException {
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
        client.deleteWithHeaders(projects(spaceId, projectId) + "?revision=" + revision, headers(idempotencyKey));
    }

    /** Dispatches a run; operation_id is server-derived from Idempotency-Key and is never sent in the body. */
    public Run dispatchRun(String spaceId, String deadline, String capability, String runClass,
                           String projectId, JsonNode payload, String idempotencyKey) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deadline", deadline);
        body.put("capability", capability);
        body.put("class", runClass);
        body.put("project_id", projectId);
        if (payload != null) body.put("payload", payload);
        return client.postWithHeaders(runs(spaceId, "", false), body, Run.class, headers(idempotencyKey));
    }

    public Run getRun(String spaceId, String runId) throws MockartyException {
        return client.get(runs(spaceId, runId, false), Run.class);
    }

    public Run cancelRun(String spaceId, String runId, long revision,
                         String idempotencyKey) throws MockartyException {
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
        return client.postWithHeaders(runs(spaceId, runId, true), Map.of("revision", revision),
                Run.class, headers(idempotencyKey));
    }

    private static Map<String, Object> mutation(String name, JsonNode body, long revision) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", name);
        value.put("body", body);
        if (revision > 0) value.put("revision", revision);
        return value;
    }

    private static Map<String, String> headers(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        return Map.of("Idempotency-Key", idempotencyKey.trim());
    }

    private static String root(String spaceId) {
        if (spaceId == null || spaceId.isBlank()) throw new IllegalArgumentException("space id is required");
        return "/api/v1/cloud/service-account/spaces/" + encode(spaceId);
    }

    private static String projects(String spaceId, String projectId) {
        String path = root(spaceId) + "/shared/projects";
        return projectId == null || projectId.isBlank() ? path : path + "/" + encode(projectId);
    }

    private static String runs(String spaceId, String runId, boolean cancel) {
        String path = root(spaceId) + "/shared/runs";
        if (runId != null && !runId.isBlank()) path += "/" + encode(runId);
        if (cancel) {
            if (runId == null || runId.isBlank()) throw new IllegalArgumentException("run id is required");
            path += "/cancel";
        }
        return path;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Self {
        @JsonProperty("service_account") private JsonNode serviceAccount;
        private CloudServiceAccountsApi.Credential credential;
        private List<CloudServiceAccountsApi.Binding> bindings = Collections.emptyList();
        private List<String> spaces = Collections.emptyList();
        private List<String> capabilities = Collections.emptyList();
        @JsonProperty("delegatable_capabilities") private List<String> delegatableCapabilities = Collections.emptyList();
        @JsonProperty("authority_capabilities") private List<String> authorityCapabilities = Collections.emptyList();
        @JsonProperty("credential_kind") private String credentialKind;
        @JsonProperty("request_id") private String requestId;
        @JsonProperty("general_token") private boolean generalToken;
        public Self() { }
        public JsonNode getServiceAccount() { return serviceAccount; }
        public CloudServiceAccountsApi.Credential getCredential() { return credential; }
        public List<CloudServiceAccountsApi.Binding> getBindings() { return bindings; }
        public List<String> getSpaces() { return spaces; }
        public List<String> getCapabilities() { return capabilities; }
        public List<String> getDelegatableCapabilities() { return delegatableCapabilities; }
        public List<String> getAuthorityCapabilities() { return authorityCapabilities; }
        public String getCredentialKind() { return credentialKind; }
        public String getRequestId() { return requestId; }
        public boolean isGeneralToken() { return generalToken; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Run {
        @JsonProperty("created_at") private String createdAt;
        @JsonProperty("updated_at") private String updatedAt;
        private String deadline;
        private JsonNode result;
        private String id;
        @JsonProperty("operation_id") private String operationId;
        private String capability;
        @JsonProperty("class") private String runClass;
        private String state;
        @JsonProperty("error_code") private String errorCode;
        private long revision;
        public Run() { }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public String getDeadline() { return deadline; }
        public JsonNode getResult() { return result; }
        public String getId() { return id; }
        public String getOperationId() { return operationId; }
        public String getCapability() { return capability; }
        public String getRunClass() { return runClass; }
        public String getState() { return state; }
        public String getErrorCode() { return errorCode; }
        public long getRevision() { return revision; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Authorization {
        @JsonProperty("space_id") private String spaceId;
        private String capability;
        private List<String> granted = Collections.emptyList();
        @JsonProperty("request_id") private String requestId;
        private boolean allowed;
        public Authorization() { }
        public String getSpaceId() { return spaceId; }
        public String getCapability() { return capability; }
        public List<String> getGranted() { return granted; }
        public String getRequestId() { return requestId; }
        public boolean isAllowed() { return allowed; }
    }
}
