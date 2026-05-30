// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.broker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pact Broker HTTP client.
 *
 * <p>Mirrors the surface of the Go SDK {@code pact.BrokerClient} and the
 * Python SDK {@code mockarty.pact.BrokerClient}, so the three SDKs are
 * interoperable from any CI step.</p>
 *
 * <p>Auth precedence (matches pact-foundation tooling):</p>
 * <ol>
 *   <li>Bearer token if non-empty (env {@code PACT_BROKER_TOKEN}).</li>
 *   <li>Basic auth fallback (env {@code PACT_BROKER_USERNAME} +
 *       {@code PACT_BROKER_PASSWORD}).</li>
 *   <li>Anonymous if neither is set.</li>
 * </ol>
 *
 * <pre>{@code
 * BrokerClient broker = BrokerClient.fromEnv();
 * byte[] pactBytes = pact.toJson().getBytes(StandardCharsets.UTF_8);
 * broker.publish(pactBytes, "1.2.3", "main", List.of("ci"));
 *
 * CanIDeployResult res = broker.canIDeploy("OrderClient", "1.2.3", "prod");
 * if (!res.deployable()) throw new IllegalStateException(res.reason());
 * }</pre>
 */
public final class BrokerClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String token;
    private final String username;
    private final String password;
    private final HttpClient http;
    private final Duration timeout;

    private BrokerClient(Builder b) {
        if (b.baseUrl == null || b.baseUrl.isBlank()) {
            throw new IllegalArgumentException(
                "BrokerClient requires baseUrl or PACT_BROKER_BASE_URL env var");
        }
        this.baseUrl = b.baseUrl.replaceAll("/+$", "");
        this.token = nz(b.token);
        this.username = nz(b.username);
        this.password = nz(b.password);
        this.timeout = b.timeout != null ? b.timeout : Duration.ofSeconds(30);
        this.http = b.http != null ? b.http
            : HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    // ------------------------------------------------------------------
    // factories
    // ------------------------------------------------------------------

    public static BrokerClient fromEnv() {
        return new Builder()
            .baseUrl(System.getenv("PACT_BROKER_BASE_URL"))
            .token(System.getenv("PACT_BROKER_TOKEN"))
            .basicAuth(System.getenv("PACT_BROKER_USERNAME"),
                       System.getenv("PACT_BROKER_PASSWORD"))
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String baseUrl;
        private String token;
        private String username;
        private String password;
        private Duration timeout;
        private HttpClient http;

        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder token(String v) { this.token = v; return this; }
        public Builder basicAuth(String u, String p) {
            this.username = u; this.password = p; return this;
        }
        public Builder timeout(Duration v) { this.timeout = v; return this; }
        public Builder httpClient(HttpClient v) { this.http = v; return this; }

        public BrokerClient build() { return new BrokerClient(this); }
    }

    // ------------------------------------------------------------------
    // publish / fetch / can-i-deploy
    // ------------------------------------------------------------------

    /**
     * Publish a pact JSON document to the broker.
     *
     * @param pactBytes       raw pact.json bytes
     * @param consumerVersion semver of the consumer build
     * @param branch          consumer branch (empty to omit)
     * @param tags            consumer tags (null/empty to omit)
     */
    public void publish(byte[] pactBytes, String consumerVersion,
                        String branch, List<String> tags)
            throws IOException, InterruptedException {
        Objects.requireNonNull(pactBytes, "pactBytes");
        if (consumerVersion == null || consumerVersion.isBlank()) {
            throw new IllegalArgumentException("consumerVersion is required");
        }
        ConsumerProvider cp = extractConsumerProvider(pactBytes);
        String path = "/pacts/provider/" + enc(cp.provider)
            + "/consumer/" + enc(cp.consumer)
            + "/version/" + enc(consumerVersion);
        Map<String, String> headers = new LinkedHashMap<>();
        if (branch != null && !branch.isBlank()) {
            headers.put("X-Pact-Consumer-Branch", branch);
        }
        send("PUT", path, pactBytes, headers, false);
        if (tags != null) {
            for (String tag : tags) {
                if (tag == null || tag.isBlank()) continue;
                String tagPath = "/pacticipants/" + enc(cp.consumer)
                    + "/versions/" + enc(consumerVersion)
                    + "/tags/" + enc(tag);
                send("PUT", tagPath, new byte[0], Map.of(), false);
            }
        }
    }

    /** Fetch a specific consumer pact version. */
    public byte[] fetch(String consumer, String provider, String version)
            throws IOException, InterruptedException {
        String path = "/pacts/provider/" + enc(provider)
            + "/consumer/" + enc(consumer)
            + "/version/" + enc(version);
        HttpResponse<byte[]> resp = send("GET", path, null, Map.of(), true);
        if (resp.statusCode() == 404) {
            throw new PactNotFoundException(consumer + "/" + provider + "/" + version);
        }
        return resp.body();
    }

    /** Fetch the latest published pact for a (consumer, provider) pair. */
    public byte[] fetchLatest(String consumer, String provider)
            throws IOException, InterruptedException {
        return fetch(consumer, provider, "latest");
    }

    /** GET /can-i-deploy — deployment gate. */
    public CanIDeployResult canIDeploy(String pacticipant, String version,
                                       String toEnvironment)
            throws IOException, InterruptedException {
        if (pacticipant == null || pacticipant.isBlank()) {
            throw new IllegalArgumentException("pacticipant is required");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version is required");
        }
        StringBuilder q = new StringBuilder("?pacticipant=").append(enc(pacticipant))
            .append("&version=").append(enc(version));
        if (toEnvironment != null && !toEnvironment.isBlank()) {
            q.append("&environment=").append(enc(toEnvironment));
        }
        HttpResponse<byte[]> resp = send("GET", "/can-i-deploy" + q, null, Map.of(), false);
        JsonNode tree;
        try {
            tree = MAPPER.readTree(resp.body());
        } catch (IOException e) {
            throw new BrokerException(resp.statusCode(),
                new String(resp.body(), StandardCharsets.UTF_8),
                "can-i-deploy: unparsable JSON: " + e.getMessage());
        }
        JsonNode summary = tree.path("summary");
        boolean deployable = summary.path("deployable").asBoolean(false);
        String reason = summary.path("reason").asText("");
        return new CanIDeployResult(deployable, reason, resp.body());
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private HttpResponse<byte[]> send(
            String method, String path, byte[] body,
            Map<String, String> extraHeaders, boolean accept404)
            throws IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(timeout)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json");
        applyAuth(rb);
        for (Map.Entry<String, String> h : extraHeaders.entrySet()) {
            rb.header(h.getKey(), h.getValue());
        }
        if (body == null) {
            rb.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            rb.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        }
        HttpResponse<byte[]> resp = http.send(rb.build(),
            HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() == 404 && accept404) return resp;
        if (resp.statusCode() >= 400) {
            throw new BrokerException(resp.statusCode(),
                new String(resp.body(), StandardCharsets.UTF_8), null);
        }
        return resp;
    }

    /**
     * Broker base URL (trailing slash stripped). Exposed so the
     * verifier in a sibling package can construct extra paths off
     * the same root without re-parsing env.
     */
    public String baseUrl() { return baseUrl; }

    /**
     * Stamp the broker's Authorization header onto an outbound
     * request builder. Used by the verifier when publishing
     * verification results back to the same broker.
     */
    public void applyAuth(HttpRequest.Builder rb) {
        // Bearer wins — matches Go + Python SDK + pact-foundation precedence.
        if (!token.isBlank()) {
            rb.header("Authorization", "Bearer " + token);
            return;
        }
        if (!username.isBlank() || !password.isBlank()) {
            String cred = username + ":" + password;
            rb.header("Authorization", "Basic "
                + Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8)));
        }
    }

    static ConsumerProvider extractConsumerProvider(byte[] pactBytes) {
        JsonNode root;
        try {
            root = MAPPER.readTree(pactBytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("pact is not valid JSON: " + e.getMessage(), e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("pact root must be a JSON object");
        }
        String consumer = nameOf(root.path("consumer"));
        String provider = nameOf(root.path("provider"));
        if (consumer.isBlank()) {
            throw new IllegalArgumentException("pact.consumer.name is required");
        }
        if (provider.isBlank()) {
            throw new IllegalArgumentException("pact.provider.name is required");
        }
        return new ConsumerProvider(consumer, provider);
    }

    private static String nameOf(JsonNode n) {
        if (n == null || !n.isObject()) return "";
        JsonNode name = n.get("name");
        return (name != null && name.isTextual()) ? name.asText().trim() : "";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    static final class ConsumerProvider {
        final String consumer;
        final String provider;
        ConsumerProvider(String c, String p) { consumer = c; provider = p; }
    }
}
