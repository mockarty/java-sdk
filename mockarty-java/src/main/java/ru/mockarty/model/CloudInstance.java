package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudInstance {
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private String id;
    private String workspaceId;
    private String name;
    private String plan;
    private String status;
    private String provider;
    private String externalRef;
    private String connectUrl;
    private String lastError;
    private List<String> features = Collections.emptyList();

    public Instant getCreatedAt() { return createdAt; }
    @JsonProperty("created_at") public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    @JsonProperty("updated_at") public void setUpdatedAt(Instant value) { updatedAt = value; }
    public Instant getDeletedAt() { return deletedAt; }
    @JsonProperty("deleted_at") public void setDeletedAt(Instant value) { deletedAt = value; }
    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getWorkspaceId() { return workspaceId; }
    @JsonProperty("workspace_id") public void setWorkspaceId(String value) { workspaceId = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getPlan() { return plan; }
    public void setPlan(String value) { plan = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { provider = value; }
    public String getExternalRef() { return externalRef; }
    @JsonProperty("external_ref") public void setExternalRef(String value) { externalRef = value; }
    public String getConnectUrl() { return connectUrl; }
    @JsonProperty("connect_url") public void setConnectUrl(String value) { connectUrl = value; }
    public String getLastError() { return lastError; }
    @JsonProperty("last_error") public void setLastError(String value) { lastError = value; }
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> value) { features = value == null ? Collections.emptyList() : value; }
}
