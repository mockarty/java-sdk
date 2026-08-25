// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One-time response returned when a Cloud webhook signing secret is issued. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudWebhookCredential {
    private CloudWebhook webhook;
    private String secret;

    public CloudWebhookCredential() {
    }

    public CloudWebhook getWebhook() {
        return webhook;
    }

    public String getSecret() {
        return secret;
    }
}
