package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudInstancesPage {
    private List<CloudInstance> instances = Collections.emptyList();
    private Map<String, Object> capabilities = Collections.emptyMap();
    private String workspace;
    private String requestId;
    private int total;

    public List<CloudInstance> getInstances() { return instances; }
    public void setInstances(List<CloudInstance> value) { instances = value == null ? Collections.emptyList() : value; }
    public Map<String, Object> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Object> value) { capabilities = value == null ? Collections.emptyMap() : value; }
    public String getWorkspace() { return workspace; }
    public void setWorkspace(String value) { workspace = value; }
    public String getRequestId() { return requestId; }
    @JsonProperty("request_id") public void setRequestId(String value) { requestId = value; }
    public int getTotal() { return total; }
    public void setTotal(int value) { total = value; }
}
