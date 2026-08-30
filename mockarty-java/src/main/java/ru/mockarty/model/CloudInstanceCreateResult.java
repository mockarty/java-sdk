package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudInstanceCreateResult {
    private CloudInstance instance;
    private CloudInstanceBootstrap bootstrap;
    private String requestId;

    public CloudInstance getInstance() { return instance; }
    public void setInstance(CloudInstance value) { instance = value; }
    public CloudInstanceBootstrap getBootstrap() { return bootstrap; }
    public void setBootstrap(CloudInstanceBootstrap value) { bootstrap = value; }
    public String getRequestId() { return requestId; }
    @JsonProperty("request_id") public void setRequestId(String value) { requestId = value; }
}
