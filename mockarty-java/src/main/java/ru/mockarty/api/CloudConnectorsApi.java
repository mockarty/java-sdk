package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CloudConnector;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Operator-only SMTP, OAuth and payment connector lifecycle. */
public class CloudConnectorsApi {
    private static final String BASE = "/api/v1/cloud/operator/connectors";
    private static final Set<String> KEYS = Set.of(
            "smtp/smtp", "oauth/yandex", "oauth/vk", "oauth/github",
            "payment/yookassa/main", "payment/stripe/main");
    private final MockartyClient client;

    public CloudConnectorsApi(MockartyClient client) { this.client = client; }

    public List<CloudConnector> list() throws MockartyException {
        ConnectorEnvelope response = client.get(BASE, ConnectorEnvelope.class);
        return response == null || response.connectors == null ? Collections.emptyList() : response.connectors;
    }

    /** Secrets are accepted on write and intentionally absent from CloudConnector. */
    public CloudConnector update(String kind, String provider, String slot,
                                 Map<String, String> config, Map<String, String> secrets,
                                 List<String> clearSecrets, long expectedRevision,
                                 boolean enabled, boolean defaultConnector,
                                 String idempotencyKey) throws MockartyException {
        if (config == null || expectedRevision < 1) {
            throw new IllegalArgumentException("config and a positive expected revision are required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("config", config);
        body.put("secrets", secrets == null ? Collections.emptyMap() : secrets);
        body.put("clear_secrets", clearSecrets == null ? Collections.emptyList() : clearSecrets);
        body.put("expected_revision", expectedRevision);
        body.put("enabled", enabled);
        body.put("default", defaultConnector);
        return client.putWithHeaders(path(kind, provider, slot), body, CloudConnector.class, headers(idempotencyKey));
    }

    public Map<?, ?> test(String kind, String provider, String slot, String idempotencyKey) throws MockartyException {
        return client.postWithHeaders(path(kind, provider, slot) + "/test", Collections.emptyMap(),
                Map.class, headers(idempotencyKey));
    }

    public void revoke(String versionId, String idempotencyKey) throws MockartyException {
        client.postWithHeaders("/api/v1/cloud/operator/connector-versions/" +
                        encode(require("version id", versionId)) + "/revoke",
                Collections.emptyMap(), Map.class, headers(idempotencyKey));
    }

    private static String path(String kind, String provider, String slot) {
        kind = normalize(kind);
        provider = normalize(provider);
        slot = slot == null ? "" : slot.trim().toLowerCase(java.util.Locale.ROOT);
        String key = kind + "/" + provider + (slot.isEmpty() ? "" : "/" + slot);
        if (!KEYS.contains(key)) throw new IllegalArgumentException("unsupported Cloud connector key");
        return BASE + "/" + encode(kind) + "/" + encode(provider) + (slot.isEmpty() ? "" : "/" + encode(slot));
    }

    private static Map<String, String> headers(String idempotencyKey) {
        return Map.of("Idempotency-Key", require("idempotency key", idempotencyKey));
    }

    private static String normalize(String value) {
        return require("connector key", value).trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String require(String label, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class ConnectorEnvelope {
        private List<CloudConnector> connectors;
        public void setConnectors(List<CloudConnector> value) { connectors = value; }
    }
}
