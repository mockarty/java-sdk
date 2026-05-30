// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an AI agent task.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentTask {

    @JsonProperty("id")
    private String id;

    /**
     * Title — required by the server's submitAgentTask handler
     * (binding:'required'). Older SDK builds didn't expose Title
     * and every Submit 400'd with 'title and prompt are required'.
     */
    @JsonProperty("title")
    private String title;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("createdAt")
    private String createdAt;

    public AgentTask() {
    }

    // Builder-style setters

    public AgentTask id(String id) {
        this.id = id;
        return this;
    }

    public AgentTask title(String title) {
        this.title = title;
        return this;
    }

    public AgentTask prompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public AgentTask status(String status) {
        this.status = status;
        return this;
    }

    public AgentTask result(Object result) {
        this.result = result;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getStatus() {
        return status;
    }

    public Object getResult() {
        return result;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "AgentTask{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", prompt='" + (prompt != null && prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt) + '\'' +
                '}';
    }
}
