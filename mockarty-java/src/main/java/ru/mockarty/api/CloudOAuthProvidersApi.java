package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CloudOAuthProvider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Operator-only Mockarty Cloud cabinet OAuth provider registry. */
public class CloudOAuthProvidersApi {
    private static final String BASE = "/api/v1/cloud/operator/oauth/providers";
    private final MockartyClient client;

    public CloudOAuthProvidersApi(MockartyClient client) { this.client = client; }

    public List<CloudOAuthProvider> list() throws MockartyException {
        ProviderEnvelope response = client.get(BASE, ProviderEnvelope.class);
        return response == null || response.providers == null ? Collections.emptyList() : response.providers;
    }

    /** clientSecretRef is accepted on write and is intentionally absent from the response model. */
    public CloudOAuthProvider update(String provider, String clientId, String clientSecretRef,
                                     long expectedRevision, boolean enabled, String idempotencyKey) throws MockartyException {
        if (expectedRevision < 0) throw new IllegalArgumentException("expected revision must be non-negative");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_id", require("client id", clientId));
        body.put("client_secret_ref", clientSecretRef == null ? "" : clientSecretRef);
        body.put("expected_revision", expectedRevision);
        body.put("enabled", enabled);
        return client.putWithHeaders(BASE + "/" + encode(require("provider", provider)), body,
                CloudOAuthProvider.class, Map.of("Idempotency-Key", require("idempotency key", idempotencyKey)));
    }

    private static String require(String label, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class ProviderEnvelope {
        private List<CloudOAuthProvider> providers;
        public void setProviders(List<CloudOAuthProvider> value) { providers = value; }
    }
}
