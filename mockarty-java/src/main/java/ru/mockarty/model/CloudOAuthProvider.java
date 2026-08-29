package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudOAuthProvider {
    private Instant updatedAt;
    private String provider;
    private String clientId;
    private String source;
    private long configRevision;
    private boolean enabled;
    private boolean secretConfigured;

    public Instant getUpdatedAt() { return updatedAt; }
    @JsonProperty("updated_at") public void setUpdatedAt(Instant value) { updatedAt = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { provider = value; }
    public String getClientId() { return clientId; }
    @JsonProperty("client_id") public void setClientId(String value) { clientId = value; }
    public String getSource() { return source; }
    public void setSource(String value) { source = value; }
    public long getConfigRevision() { return configRevision; }
    @JsonProperty("config_revision") public void setConfigRevision(long value) { configRevision = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isSecretConfigured() { return secretConfigured; }
    @JsonProperty("secret_configured") public void setSecretConfigured(boolean value) { secretConfigured = value; }
}
