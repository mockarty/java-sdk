// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One persisted attempt to deliver a Mockarty Cloud webhook event. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudWebhookDelivery {
    private String id;

    @JsonProperty("webhook_id")
    private String webhookId;

    @JsonProperty("workspace_id")
    private String workspaceId;

    private String event;
    private String status;

    @JsonProperty("response_body")
    private String responseBody;

    @JsonProperty("last_attempt_at")
    private String lastAttemptAt;

    @JsonProperty("next_retry_at")
    private String nextRetryAt;

    @JsonProperty("delivered_at")
    private String deliveredAt;

    private int attempt;

    @JsonProperty("status_code")
    private Integer statusCode;

    public CloudWebhookDelivery() {
    }

    public String getId() {
        return id;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getEvent() {
        return event;
    }

    public String getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getLastAttemptAt() {
        return lastAttemptAt;
    }

    public String getNextRetryAt() {
        return nextRetryAt;
    }

    public String getDeliveredAt() {
        return deliveredAt;
    }

    public int getAttempt() {
        return attempt;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
