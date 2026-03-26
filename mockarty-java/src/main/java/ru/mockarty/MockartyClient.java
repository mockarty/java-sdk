// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.api.AgentTaskApi;
import ru.mockarty.api.CollectionApi;
import ru.mockarty.api.ContractApi;
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
import ru.mockarty.api.TestRunApi;
import ru.mockarty.api.UndefinedApi;
import ru.mockarty.exception.MockartyApiException;
import ru.mockarty.exception.MockartyConnectionException;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.exception.MockartyForbiddenException;
import ru.mockarty.exception.MockartyNotFoundException;
import ru.mockarty.exception.MockartyUnauthorizedException;
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

    private void handleErrorResponse(int statusCode, String responseBody) throws MockartyApiException {
        String errorMessage = extractErrorMessage(responseBody);

        switch (statusCode) {
            case 401:
                throw new MockartyUnauthorizedException(errorMessage, responseBody);
            case 403:
                throw new MockartyForbiddenException(errorMessage, responseBody);
            case 404:
                throw new MockartyNotFoundException(errorMessage, responseBody);
            default:
                throw new MockartyApiException(statusCode, errorMessage, responseBody);
        }
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "Unknown error";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
            Object error = errorMap.get("error");
            if (error != null) {
                return error.toString();
            }
            Object message = errorMap.get("message");
            if (message != null) {
                return message.toString();
            }
        } catch (JsonProcessingException e) {
            // Response is not JSON, return as-is
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
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
