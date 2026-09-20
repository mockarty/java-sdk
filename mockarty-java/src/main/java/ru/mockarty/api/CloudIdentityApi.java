// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CloudOAuthIdentity;
import ru.mockarty.model.CloudStepUpResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Current Cloud account sign-in methods and step-up verification. */
public class CloudIdentityApi {
    private static final String BASE = "/api/v1/cloud/auth/oauth/identities";
    private final MockartyClient client;

    public CloudIdentityApi(MockartyClient client) { this.client = client; }

    public List<CloudOAuthIdentity> list() throws MockartyException {
        IdentityEnvelope response = client.get(BASE, IdentityEnvelope.class);
        return response == null || response.identities == null ? Collections.emptyList() : response.identities;
    }

    public CloudStepUpResult stepUp(String action, String credential, boolean forceCredential) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", require("action", action));
        body.put("credential", credential == null ? "" : credential);
        body.put("force_credential", forceCredential);
        return client.post("/api/v1/cloud/auth/step-up", body, CloudStepUpResult.class);
    }

    public void unlink(String provider, String idempotencyKey) throws MockartyException {
        client.deleteWithHeaders(BASE + "/" + encode(require("provider", provider)),
                Map.of("Idempotency-Key", require("idempotency key", idempotencyKey)));
    }

    public String linkUrl(String provider) {
        return client.getConfig().getBaseUrl() + "/api/v1/cloud/auth/oauth/" + encode(require("provider", provider)) + "/link";
    }

    private static String require(String label, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class IdentityEnvelope {
        private List<CloudOAuthIdentity> identities;
        public void setIdentities(List<CloudOAuthIdentity> value) { identities = value; }
    }
}
