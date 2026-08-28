package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/** A project stored in one explicit Shared SaaS Space. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudSharedProject {
    private String id;
    private String name;
    private JsonNode body;
    private long revision;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    public CloudSharedProject() { }
    public String getId() { return id; }
    public String getName() { return name; }
    public JsonNode getBody() { return body; }
    public long getRevision() { return revision; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
