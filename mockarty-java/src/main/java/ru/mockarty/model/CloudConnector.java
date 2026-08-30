package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Safe Cloud connector projection. Secret values are intentionally absent. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudConnector {
    private Instant updatedAt;
    private Instant lastTestedAt;
    private Map<String, String> config = Collections.emptyMap();
    private List<String> secretFields = Collections.emptyList();
    private String id;
    private String versionId;
    private String key;
    private String kind;
    private String provider;
    private String displayName;
    private String lastTestStatus;
    private String lastTestCode;
    private long revision;
    private boolean enabled;
    private boolean defaultConnector;
    private boolean secretConfigured;
    private boolean secretRevoked;

    public Instant getUpdatedAt() { return updatedAt; }
    @JsonProperty("updated_at") public void setUpdatedAt(Instant value) { updatedAt = value; }
    public Instant getLastTestedAt() { return lastTestedAt; }
    @JsonProperty("last_tested_at") public void setLastTestedAt(Instant value) { lastTestedAt = value; }
    public Map<String, String> getConfig() { return config; }
    public void setConfig(Map<String, String> value) { config = value; }
    public List<String> getSecretFields() { return secretFields; }
    @JsonProperty("secret_fields") public void setSecretFields(List<String> value) { secretFields = value; }
    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getVersionId() { return versionId; }
    @JsonProperty("version_id") public void setVersionId(String value) { versionId = value; }
    public String getKey() { return key; }
    public void setKey(String value) { key = value; }
    public String getKind() { return kind; }
    public void setKind(String value) { kind = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { provider = value; }
    public String getDisplayName() { return displayName; }
    @JsonProperty("display_name") public void setDisplayName(String value) { displayName = value; }
    public String getLastTestStatus() { return lastTestStatus; }
    @JsonProperty("last_test_status") public void setLastTestStatus(String value) { lastTestStatus = value; }
    public String getLastTestCode() { return lastTestCode; }
    @JsonProperty("last_test_code") public void setLastTestCode(String value) { lastTestCode = value; }
    public long getRevision() { return revision; }
    public void setRevision(long value) { revision = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isDefaultConnector() { return defaultConnector; }
    @JsonProperty("default") public void setDefaultConnector(boolean value) { defaultConnector = value; }
    public boolean isSecretConfigured() { return secretConfigured; }
    @JsonProperty("secret_configured") public void setSecretConfigured(boolean value) { secretConfigured = value; }
    public boolean isSecretRevoked() { return secretRevoked; }
    @JsonProperty("secret_revoked") public void setSecretRevoked(boolean value) { secretRevoked = value; }
}
