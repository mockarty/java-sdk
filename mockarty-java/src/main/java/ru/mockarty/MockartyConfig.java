// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty;

import java.time.Duration;

/**
 * Configuration for the Mockarty client.
 * Supports configuration via builder, environment variables, and system properties.
 */
public class MockartyConfig {

    private static final String ENV_BASE_URL = "MOCKARTY_BASE_URL";
    private static final String ENV_API_KEY = "MOCKARTY_API_KEY";
    private static final String ENV_NAMESPACE = "MOCKARTY_NAMESPACE";

    private static final String DEFAULT_BASE_URL = "http://localhost:5770";
    private static final String DEFAULT_NAMESPACE = "sandbox";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_MAX_RETRIES = 2;

    private final String baseUrl;
    private final String apiKey;
    private final String namespace;
    private final Duration timeout;
    private final int maxRetries;

    private MockartyConfig(String baseUrl, String apiKey, String namespace, Duration timeout,
                           Integer maxRetries) {
        this.baseUrl = normalizeUrl(baseUrl != null ? baseUrl : resolveEnv(ENV_BASE_URL, DEFAULT_BASE_URL));
        this.apiKey = apiKey != null ? apiKey : resolveEnv(ENV_API_KEY, null);
        this.namespace = namespace != null ? namespace : resolveEnv(ENV_NAMESPACE, DEFAULT_NAMESPACE);
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
        this.maxRetries = (maxRetries != null && maxRetries >= 0) ? maxRetries : DEFAULT_MAX_RETRIES;
    }

    /**
     * Creates a default configuration using environment variables.
     */
    public static MockartyConfig defaults() {
        return new MockartyConfig(null, null, null, null, null);
    }

    /**
     * Creates a configuration with the given base URL.
     */
    public static MockartyConfig of(String baseUrl) {
        return new MockartyConfig(baseUrl, null, null, null, null);
    }

    /**
     * Creates a configuration with the given base URL and API key.
     */
    public static MockartyConfig of(String baseUrl, String apiKey) {
        return new MockartyConfig(baseUrl, apiKey, null, null, null);
    }

    static MockartyConfig create(String baseUrl, String apiKey, String namespace, Duration timeout,
                                 Integer maxRetries) {
        return new MockartyConfig(baseUrl, apiKey, namespace, timeout, maxRetries);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getNamespace() {
        return namespace;
    }

    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Maximum automatic retries on transient failures (network errors and
     * HTTP 429/502/503/504). {@code 0} disables retries. Defaults to 2.
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    private static String normalizeUrl(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String resolveEnv(String envVar, String defaultValue) {
        // Check system property first, then environment variable
        String value = System.getProperty(envVar);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return "MockartyConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", namespace='" + namespace + '\'' +
                ", timeout=" + timeout +
                ", apiKey=" + (apiKey != null ? "[set]" : "[not set]") +
                '}';
    }
}
