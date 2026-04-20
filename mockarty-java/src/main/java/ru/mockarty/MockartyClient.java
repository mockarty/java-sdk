// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.api.AgentTaskApi;
import ru.mockarty.api.ChaosApi;
import ru.mockarty.api.CollectionApi;
import ru.mockarty.api.ContractApi;
import ru.mockarty.api.EntitySearchApi;
import ru.mockarty.api.EnvironmentApi;
import ru.mockarty.api.FolderApi;
import ru.mockarty.api.FuzzingApi;
import ru.mockarty.api.GeneratorApi;
import ru.mockarty.api.HealthApi;
import ru.mockarty.api.ImportApi;
import ru.mockarty.api.MockApi;
import ru.mockarty.api.NamespaceApi;
import ru.mockarty.api.NamespaceSettingsApi;
import ru.mockarty.api.PerfApi;
import ru.mockarty.api.ProxyApi;
import ru.mockarty.api.RecorderApi;
import ru.mockarty.api.StatsApi;
import ru.mockarty.api.StoreApi;
import ru.mockarty.api.TagApi;
import ru.mockarty.api.TemplateApi;
import ru.mockarty.api.TestPlanApi;
import ru.mockarty.api.TestRunApi;
import ru.mockarty.api.TrashApi;
import ru.mockarty.api.UndefinedApi;
import ru.mockarty.exception.MockartyApiException;
import ru.mockarty.exception.MockartyConflictException;
import ru.mockarty.exception.MockartyConnectionException;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.exception.MockartyExternalException;
import ru.mockarty.exception.MockartyForbiddenException;
import ru.mockarty.exception.MockartyNotFoundException;
import ru.mockarty.exception.MockartyRateLimitException;
import ru.mockarty.exception.MockartyServerException;
import ru.mockarty.exception.MockartyUnauthorizedException;
import ru.mockarty.exception.MockartyUnavailableException;
import ru.mockarty.exception.MockartyValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Main entry point for interacting with Mockarty server.
 * Thread-safe and reusable across multiple requests.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * try (MockartyClient client = MockartyClient.builder()
 *         .baseUrl("http://localhost:5770")
 *         .apiKey("your-api-key")
 *         .build()) {
 *
 *     Mock mock = MockBuilder.http("/api/users/:id", "GET")
 *         .respond(200, Map.of("name", "John"))
 *         .build();
 *     client.mocks().create(mock);
 * }
 * }</pre>
 */
public class MockartyClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MockartyClient.class);
    private static final String USER_AGENT = "mockarty-java-sdk/0.1.0";

    private final MockartyConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final boolean ownsHttpClient;

    private MockartyClient(MockartyConfig config, HttpClient httpClient) {
        this.config = config;
        if (httpClient != null) {
            this.httpClient = httpClient;
            this.ownsHttpClient = false;
        } else {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(config.getTimeout())
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            this.ownsHttpClient = true;
        }
        this.objectMapper = createObjectMapper();
        log.debug("MockartyClient initialized: {}", config);
    }

    /**
     * Creates a new builder for configuring the client.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a client with default configuration (reads from env vars).
     */
    public static MockartyClient create() {
        return new MockartyClient(MockartyConfig.defaults(), null);
    }

    /**
     * Creates a client pointing to the given base URL.
     */
    public static MockartyClient create(String baseUrl) {
        return new MockartyClient(MockartyConfig.of(baseUrl), null);
    }

    /**
     * Creates a client with the given base URL and API key.
     */
    public static MockartyClient create(String baseUrl, String apiKey) {
        return new MockartyClient(MockartyConfig.of(baseUrl, apiKey), null);
    }

    // API access methods

    /**
     * Returns the Mock API for CRUD operations on mocks.
     */
    public MockApi mocks() {
        return new MockApi(this);
    }

    /**
     * Returns the Namespace API for namespace management.
     */
    public NamespaceApi namespaces() {
        return new NamespaceApi(this);
    }

    /**
     * Returns the Store API for Global and Chain store operations.
     */
    public StoreApi stores() {
        return new StoreApi(this);
    }

    /**
     * Returns the Collection API for test collection management.
     */
    public CollectionApi collections() {
        return new CollectionApi(this);
    }

    /**
     * Returns the Performance API for load testing operations.
     */
    public PerfApi perf() {
        return new PerfApi(this);
    }

    /**
     * Returns the Health API for health check operations.
     */
    public HealthApi health() {
        return new HealthApi(this);
    }

    /**
     * Returns the Generator API for generating mocks from API specifications.
     */
    public GeneratorApi generator() {
        return new GeneratorApi(this);
    }

    /**
     * Returns the Fuzzing API for security and robustness testing.
     */
    public FuzzingApi fuzzing() {
        return new FuzzingApi(this);
    }

    /**
     * Returns the Contract API for contract testing operations.
     */
    public ContractApi contracts() {
        return new ContractApi(this);
    }

    /**
     * Returns the Recorder API for traffic recording operations.
     */
    public RecorderApi recorder() {
        return new RecorderApi(this);
    }

    /**
     * Returns the Template API for response template management.
     */
    public TemplateApi templates() {
        return new TemplateApi(this);
    }

    /**
     * Returns the Import API for importing mocks from various formats.
     */
    public ImportApi imports() {
        return new ImportApi(this);
    }

    /**
     * Returns the Test Run API for test run management.
     */
    public TestRunApi testRuns() {
        return new TestRunApi(this);
    }

    /**
     * Returns the Test Plan API — master orchestrator for heterogeneous
     * (functional / load / fuzz / chaos / contract) runs.
     */
    public TestPlanApi testPlans() {
        return new TestPlanApi(this);
    }

    /**
     * Returns the Tag API for tag management.
     */
    public TagApi tags() {
        return new TagApi(this);
    }

    /**
     * Returns the Folder API for mock folder management.
     */
    public FolderApi folders() {
        return new FolderApi(this);
    }

    /**
     * Returns the Undefined Requests API for managing unmatched requests.
     */
    public UndefinedApi undefined() {
        return new UndefinedApi(this);
    }

    /**
     * Returns the Stats API for system statistics.
     */
    public StatsApi stats() {
        return new StatsApi(this);
    }

    /**
     * Returns the Agent Task API for AI agent task management.
     */
    public AgentTaskApi agentTasks() {
        return new AgentTaskApi(this);
    }

    /**
     * Returns the Namespace Settings API for namespace-level settings.
     */
    public NamespaceSettingsApi namespaceSettings() {
        return new NamespaceSettingsApi(this);
    }

    /**
     * Returns the Proxy API for proxying requests.
     */
    public ProxyApi proxy() {
        return new ProxyApi(this);
    }

    /**
     * Returns the Environment API for API tester environment management.
     */
    public EnvironmentApi environments() {
        return new EnvironmentApi(this);
    }

    /**
     * Returns the Chaos API for chaos engineering operations.
     */
    public ChaosApi chaos() {
        return new ChaosApi(this);
    }

    /**
     * Returns the unified entity-search API — resolve names → IDs across
     * mocks, test plans, perf configs, fuzz configs, chaos experiments and
     * contract pacts in one call.
     */
    public EntitySearchApi entitySearch() {
        return new EntitySearchApi(this);
    }

    /**
     * Returns the Trash / Recycle Bin API (list, restore, purge).
     */
    public TrashApi trash() {
        return new TrashApi(this);
    }

    /**
     * Returns the current client configuration.
     */
    public MockartyConfig getConfig() {
        return config;
    }

    /**
     * Returns the ObjectMapper used for JSON serialization.
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // Internal HTTP methods used by API classes

    /**
     * Performs a GET request and deserializes the response.
     */
    public <T> T get(String path, Class<T> responseType) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .GET()
                .build();
        return execute(request, responseType);
    }

    /**
     * Performs a GET request and deserializes the response to a parameterized type.
     */
    public <T> T get(String path, JavaType responseType) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .GET()
                .build();
        return execute(request, responseType);
    }

    /**
     * Performs a POST request with a JSON body and deserializes the response.
     */
    public <T> T post(String path, Object body, Class<T> responseType) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .POST(jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        return execute(request, responseType);
    }

    /**
     * Performs a POST request with a JSON body and deserializes the response to a parameterized type.
     */
    public <T> T post(String path, Object body, JavaType responseType) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .POST(jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        return execute(request, responseType);
    }

    /**
     * Performs a POST request and returns the raw response bytes.
     */
    public byte[] postBytes(String path, Object body) throws MockartyException {
        HttpRequest.Builder builder = buildRequest(path)
                .header("Content-Type", "application/json");
        if (body != null) {
            builder.POST(jsonBody(body));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        HttpRequest request = builder.build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                handleErrorResponse(response.statusCode(), new String(response.body()));
            }
            return response.body();
        } catch (MockartyException e) {
            throw e;
        } catch (IOException e) {
            throw new MockartyConnectionException("Failed to connect to Mockarty: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MockartyConnectionException("Request interrupted", e);
        }
    }

    /**
     * Performs a POST request with a JSON body without expecting a response body.
     */
    public void post(String path, Object body) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .POST(jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        executeVoid(request);
    }

    /**
     * Performs a PUT request with a JSON body and deserializes the response.
     */
    public <T> T put(String path, Object body, Class<T> responseType) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .PUT(jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        return execute(request, responseType);
    }

    /**
     * Performs a PATCH request with a JSON body and deserializes the response.
     */
    public <T> T patch(String path, Object body, Class<T> responseType) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .method("PATCH", jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        return execute(request, responseType);
    }

    /**
     * Performs a PATCH request with a JSON body without expecting a response body.
     */
    public void patch(String path, Object body) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .method("PATCH", jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        executeVoid(request);
    }

    /**
     * Performs a PUT request with a JSON body without expecting a response body.
     */
    public void put(String path, Object body) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .PUT(jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        executeVoid(request);
    }

    /**
     * Performs a DELETE request.
     */
    public void delete(String path) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .DELETE()
                .build();
        executeVoid(request);
    }

    /**
     * Performs a DELETE request with a JSON body.
     */
    public void delete(String path, Object body) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .method("DELETE", jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        executeVoid(request);
    }

    /**
     * Performs a DELETE request with a JSON body and deserializes the response.
     */
    public <T> T delete(String path, Object body, Class<T> responseType) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .method("DELETE", jsonBody(body))
                .header("Content-Type", "application/json")
                .build();
        return execute(request, responseType);
    }

    /**
     * Performs a GET request and returns the raw response bytes.
     */
    public byte[] getBytes(String path) throws MockartyException {
        HttpRequest request = buildRequest(path)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                handleErrorResponse(response.statusCode(), new String(response.body()));
            }
            return response.body();
        } catch (MockartyException e) {
            throw e;
        } catch (IOException e) {
            throw new MockartyConnectionException("Failed to connect to Mockarty: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MockartyConnectionException("Request interrupted", e);
        }
    }

    @Override
    public void close() {
        log.debug("MockartyClient closed");
        // HttpClient in Java 11 doesn't have a close method.
        // Resources will be garbage collected.
    }

    // Private methods

    private HttpRequest.Builder buildRequest(String path) {
        String url = config.getBaseUrl() + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(config.getTimeout())
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json");

        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            builder.header("Authorization", "Bearer " + config.getApiKey());
        }

        return builder;
    }

    private HttpRequest.BodyPublisher jsonBody(Object body) {
        if (body == null) {
            return HttpRequest.BodyPublishers.noBody();
        }
        try {
            String json = objectMapper.writeValueAsString(body);
            log.trace("Request body: {}", json);
            return HttpRequest.BodyPublishers.ofString(json);
        } catch (JsonProcessingException e) {
            throw new MockartyException("Failed to serialize request body", e);
        }
    }

    private <T> T execute(HttpRequest request, Class<T> responseType) throws MockartyException {
        String responseBody = executeRaw(request);
        if (responseType == String.class) {
            @SuppressWarnings("unchecked")
            T result = (T) responseBody;
            return result;
        }
        if (responseType == Void.class || responseType == void.class) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JsonProcessingException e) {
            throw new MockartyException("Failed to deserialize response: " + e.getMessage(), e);
        }
    }

    private <T> T execute(HttpRequest request, JavaType responseType) throws MockartyException {
        String responseBody = executeRaw(request);
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JsonProcessingException e) {
            throw new MockartyException("Failed to deserialize response: " + e.getMessage(), e);
        }
    }

    private void executeVoid(HttpRequest request) throws MockartyException {
        executeRaw(request);
    }

    private String executeRaw(HttpRequest request) throws MockartyException {
        log.debug("{} {}", request.method(), request.uri());
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.debug("Response: {} ({} chars)", response.statusCode(),
                    response.body() != null ? response.body().length() : 0);

            if (response.statusCode() >= 400) {
                handleErrorResponse(response.statusCode(), response.body());
            }

            return response.body();
        } catch (MockartyException e) {
            throw e;
        } catch (IOException e) {
            throw new MockartyConnectionException(
                    "Failed to connect to Mockarty at " + config.getBaseUrl() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MockartyConnectionException("Request to Mockarty was interrupted", e);
        }
    }

    /**
     * Parsed representation of the Mockarty uniform error envelope:
     * <pre>{"error": "...", "code": "...", "request_id": "..."}</pre>
     */
    private static final class ParsedError {
        final String message;
        final String code;
        final String requestId;

        ParsedError(String message, String code, String requestId) {
            this.message = message;
            this.code = code;
            this.requestId = requestId;
        }
    }

    private void handleErrorResponse(int statusCode, String responseBody) throws MockartyApiException {
        ParsedError parsed = parseErrorEnvelope(responseBody);
        String errorMessage = parsed.message;
        String code = parsed.code;
        String requestId = parsed.requestId;

        // Primary dispatch: by stable code field (preferred for new servers).
        if (code != null && !code.isEmpty()) {
            switch (code) {
                case "validation":
                    throw new MockartyValidationException(errorMessage, responseBody, code, requestId);
                case "unauthorized":
                    throw new MockartyUnauthorizedException(errorMessage, responseBody, code, requestId);
                case "forbidden":
                    throw new MockartyForbiddenException(errorMessage, responseBody, code, requestId);
                case "not_found":
                    throw new MockartyNotFoundException(errorMessage, responseBody, code, requestId);
                case "conflict":
                    throw new MockartyConflictException(errorMessage, responseBody, code, requestId);
                case "rate_limit":
                    throw new MockartyRateLimitException(errorMessage, responseBody, code, requestId);
                case "unavailable":
                    throw new MockartyUnavailableException(errorMessage, responseBody, code, requestId);
                case "external":
                    throw new MockartyExternalException(errorMessage, responseBody, code, requestId);
                case "internal":
                    throw new MockartyServerException(statusCode, errorMessage, responseBody, code, requestId);
                default:
                    // Unknown code from a newer server — fall through to status-based dispatch.
                    break;
            }
        }

        // Fallback: dispatch by HTTP status (legacy servers or unknown codes).
        switch (statusCode) {
            case 400:
                throw new MockartyValidationException(errorMessage, responseBody, code, requestId);
            case 401:
                throw new MockartyUnauthorizedException(errorMessage, responseBody, code, requestId);
            case 403:
                throw new MockartyForbiddenException(errorMessage, responseBody, code, requestId);
            case 404:
                throw new MockartyNotFoundException(errorMessage, responseBody, code, requestId);
            case 409:
                throw new MockartyConflictException(errorMessage, responseBody, code, requestId);
            case 429:
                throw new MockartyRateLimitException(errorMessage, responseBody, code, requestId);
            case 502:
                throw new MockartyExternalException(errorMessage, responseBody, code, requestId);
            case 503:
                throw new MockartyUnavailableException(errorMessage, responseBody, code, requestId);
            default:
                if (statusCode >= 500) {
                    throw new MockartyServerException(statusCode, errorMessage, responseBody, code, requestId);
                }
                throw new MockartyApiException(statusCode, errorMessage, responseBody, code, requestId);
        }
    }

    /**
     * Parses the uniform Mockarty error envelope:
     * <pre>{"error": "...", "code": "...", "request_id": "..."}</pre>
     * Falls back to raw text for non-JSON or empty bodies. The {@code message}
     * field (legacy fallback) is accepted if {@code error} is missing.
     */
    private ParsedError parseErrorEnvelope(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return new ParsedError("Unknown error", null, null);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);

            String message = null;
            Object error = errorMap.get("error");
            if (error != null) {
                message = error.toString();
            } else {
                Object legacy = errorMap.get("message");
                if (legacy != null) {
                    message = legacy.toString();
                }
            }
            if (message == null) {
                message = truncate(responseBody);
            }

            String code = null;
            Object rawCode = errorMap.get("code");
            if (rawCode != null) {
                String s = rawCode.toString();
                if (!s.isEmpty()) {
                    code = s;
                }
            }

            String requestId = null;
            Object rawReqId = errorMap.get("request_id");
            if (rawReqId != null) {
                String s = rawReqId.toString();
                if (!s.isEmpty()) {
                    requestId = s;
                }
            }

            return new ParsedError(message, code, requestId);
        } catch (JsonProcessingException e) {
            // Response is not JSON — return raw text as the message.
            return new ParsedError(truncate(responseBody), null, null);
        }
    }

    private static String truncate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Builder for constructing a MockartyClient with custom configuration.
     */
    public static class Builder {
        private String baseUrl;
        private String apiKey;
        private String namespace;
        private Duration timeout;
        private HttpClient httpClient;

        Builder() {
        }

        /**
         * Sets the base URL of the Mockarty server.
         * Defaults to MOCKARTY_BASE_URL env var or http://localhost:5770.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the API key for authentication.
         * Defaults to MOCKARTY_API_KEY env var.
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the default namespace for mock operations.
         * Defaults to MOCKARTY_NAMESPACE env var or "sandbox".
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Sets the request timeout duration.
         * Defaults to 30 seconds.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets a custom HttpClient to use.
         * If not set, a default HttpClient will be created.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Builds the MockartyClient with the configured settings.
         */
        public MockartyClient build() {
            MockartyConfig config = MockartyConfig.create(baseUrl, apiKey, namespace, timeout);
            return new MockartyClient(config, httpClient);
        }
    }
}
