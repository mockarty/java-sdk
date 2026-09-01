// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import ru.mockarty.api.AgentTaskApi;
import ru.mockarty.api.AutonomousMissionsApi;
import ru.mockarty.api.McpApi;
import ru.mockarty.api.IssueTrackerApi;
import ru.mockarty.api.TcmApi;
import ru.mockarty.api.ChaosApi;
import ru.mockarty.api.CloudWebhooksApi;
import ru.mockarty.api.CloudInstancesApi;
import ru.mockarty.api.CloudConnectorsApi;
import ru.mockarty.api.CloudOAuthProvidersApi;
import ru.mockarty.api.CloudRiskApi;
import ru.mockarty.api.CloudRefundsApi;
import ru.mockarty.api.CloudIdentityApi;
import ru.mockarty.api.CloudSpacesApi;
import ru.mockarty.api.CloudEntitlementsApi;
import ru.mockarty.api.CloudSharedProjectsApi;
import ru.mockarty.api.CloudCustomerApi;
import ru.mockarty.api.CloudOperationsApi;
import ru.mockarty.api.DeliveryPolicyApi;
import ru.mockarty.api.MediaDeliveryApi;
import ru.mockarty.api.EffectReconciliationApi;
import ru.mockarty.api.PageAnalyzerApi;
import ru.mockarty.api.CoderDeliveryApi;
import ru.mockarty.api.CollectionApi;
import ru.mockarty.api.ContractApi;
import ru.mockarty.api.EntitySearchApi;
import ru.mockarty.api.ExperienceApi;
import ru.mockarty.api.EconomicsApi;
import ru.mockarty.api.LLMSecurityApi;
import ru.mockarty.api.EnvironmentApi;
import ru.mockarty.api.FolderApi;
import ru.mockarty.api.FuzzingApi;
import ru.mockarty.api.GeneratorApi;
import ru.mockarty.api.HealthApi;
import ru.mockarty.api.MeApi;
import ru.mockarty.api.ImportApi;
import ru.mockarty.api.MockApi;
import ru.mockarty.api.NamespaceApi;
import ru.mockarty.api.NamespaceSettingsApi;
import ru.mockarty.api.PerfApi;
import ru.mockarty.api.PromptsApi;
import ru.mockarty.api.ProxyApi;
import ru.mockarty.api.SecretsApi;
import ru.mockarty.api.SecurityApi;
import ru.mockarty.api.ExternalRunsApi;
import ru.mockarty.api.FlowRunsApi;
import ru.mockarty.api.RecorderApi;
import ru.mockarty.api.StatsApi;
import ru.mockarty.api.StoreApi;
import ru.mockarty.api.TagApi;
import ru.mockarty.api.TemplateApi;
import ru.mockarty.api.TestPlanApi;
import ru.mockarty.api.TestRunApi;
import ru.mockarty.api.UndefinedApi;
import ru.mockarty.api.WorkflowDefinitionsApi;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
            CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(config.getTimeout())
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .cookieHandler(cookies)
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

    /** Returns autonomous mission intake and supervision operations. */
    public AutonomousMissionsApi autonomousMissions() {
        return new AutonomousMissionsApi(this);
    }

    /** Returns admitted coder repositories, delivery targets, and deploy missions. */
    public CoderDeliveryApi coderDelivery() {
        return new CoderDeliveryApi(this);
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
     * Returns the Me API for per-caller endpoints ({@code /api/v1/me/*}),
     * e.g. the manual-action queue. Parity with Go {@code Me()} / Python {@code me}.
     */
    public MeApi me() {
        return new MeApi(this);
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
     * Returns the recorded-UI-test API (save / run / poll / export).
     */
    public ru.mockarty.api.UITestApi uiTests() {
        return new ru.mockarty.api.UITestApi(this);
    }

    /**
     * Returns the git-sync API — bind a repo, pull/push autotest collections.
     */
    public ru.mockarty.api.GitSyncApi gitSync() {
        return new ru.mockarty.api.GitSyncApi(this);
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
     * Returns the MCP client — list/call the server's agent-facing tool surface
     * over the streamable-HTTP {@code /mcp} endpoint.
     */
    public McpApi mcp() {
        return new McpApi(this);
    }

    /**
     * Returns the issue-tracker task-automation API (issues/comments/projects/sprints).
     */
    public IssueTrackerApi issueTracker() {
        return new IssueTrackerApi(this);
    }

    /**
     * Returns the Test Case Management automation API (cases/case-runs/defects).
     */
    public TcmApi tcm() {
        return new TcmApi(this);
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
     * Returns the CI Triggers API — list saved triggers and
     * poll the linked CI run state. CRUD is intentionally NOT in the
     * SDK (admin UI concern); use {@code list()} to find an id to pass
     * as {@code ciTriggerId} on perf/fuzz launches.
     */
    public ru.mockarty.api.CITriggersApi ciTriggers() {
        return new ru.mockarty.api.CITriggersApi(this);
    }

    /**
     * Returns the unified entity-search API — resolve names → IDs across
     * mocks, test plans, perf configs, fuzz configs, chaos experiments and
     * contract pacts in one call.
     */
    public EntitySearchApi entitySearch() {
        return new EntitySearchApi(this);
    }

    /** Returns the reusable AutoTester run-experience API. */
    public ExperienceApi experience() {
        return new ExperienceApi(this);
    }

    /** Returns the administrator LLM usage and immutable price-book API. */
    public EconomicsApi economics() {
        return new EconomicsApi(this);
    }

    /** Returns the layered prompt-security management API. */
    public LLMSecurityApi llmSecurity() {
        return new LLMSecurityApi(this);
    }

    /** Returns the versioned workflow draft, dry-run and publish API. */
    public WorkflowDefinitionsApi workflowDefinitions() {
        return new WorkflowDefinitionsApi(this);
    }

    /** Returns the workspace webhook automation API for Mockarty Cloud. */
    public CloudWebhooksApi cloudWebhooks() {
        return new CloudWebhooksApi(this);
    }

    /** Returns the dedicated Cloud contour lifecycle API. */
    public CloudInstancesApi cloudInstances() {
        return new CloudInstancesApi(this);
    }

    /** Returns the operator-only Cloud platform connector lifecycle. */
    public CloudConnectorsApi cloudConnectors() {
        return new CloudConnectorsApi(this);
    }

    /** Returns the operator-only Cloud cabinet sign-in provider registry. */
    public CloudOAuthProvidersApi cloudOAuthProviders() {
        return new CloudOAuthProvidersApi(this);
    }

    /** Returns the operator-only Cloud risk case and enforcement API. */
    public CloudRiskApi cloudRisk() {
        return new CloudRiskApi(this);
    }

    /** Returns the operator-only durable Cloud refund recovery API. */
    public CloudRefundsApi cloudRefunds() {
        return new CloudRefundsApi(this);
    }

    /** Returns the current Cloud account sign-in-method and step-up API. */
    public CloudIdentityApi cloudIdentity() {
        return new CloudIdentityApi(this);
    }

    /** Returns the canonical explicit-Space collaboration API. */
    public CloudSpacesApi cloudSpaces() {
        return new CloudSpacesApi(this);
    }

    /** Returns customer-authorized Cloud loyalty, support and risk-appeal APIs. */
    public CloudCustomerApi cloudCustomer() {
        return new CloudCustomerApi(this);
    }

    /** Returns least-privilege operator support and product analytics APIs. */
    public CloudOperationsApi cloudOperations() {
        return new CloudOperationsApi(this);
    }

    /** Returns administrator delivery-policy environment management. */
    public DeliveryPolicyApi deliveryPolicy() {
        return new DeliveryPolicyApi(this);
    }

    /** Returns operator reconciliation for ambiguous media runner deliveries. */
    public MediaDeliveryApi mediaDelivery() {
        return new MediaDeliveryApi(this);
    }

    /** Returns the admin queue for unresolved external effects. */
    public EffectReconciliationApi effectReconciliation() {
        return new EffectReconciliationApi(this);
    }

    /** Returns the HTTP-level Page Analyzer lifecycle API. */
    public PageAnalyzerApi pageAnalyzer() {
        return new PageAnalyzerApi(this);
    }

    /** Returns the committed unsigned Cloud entitlement projection API. */
    public CloudEntitlementsApi cloudEntitlements() {
        return new CloudEntitlementsApi(this);
    }

    /** Returns the public Shared SaaS project CRUD API. */
    public CloudSharedProjectsApi cloudSharedProjects() {
        return new CloudSharedProjectsApi(this);
    }

    /**
     * Returns the external-run upload API — used by the JUnit 5
     * adapter (and direct callers) to ship per-test outcomes from an
     * external test framework into TCM as a synthetic case run.
     */
    public ExternalRunsApi externalRuns() {
        return new ExternalRunsApi(this);
    }

    /**
     * Returns the test-discovery sync API — used by SDK/CI adapters (and
     * the JUnit 5 launcher listener) to sync a manifest of the full test
     * inventory into TCM so the catalogue mirrors the source tree.
     *
     * @see ru.mockarty.api.DiscoveryApi
     */
    public ru.mockarty.api.DiscoveryApi discovery() {
        return new ru.mockarty.api.DiscoveryApi(this);
    }

    /**
     * Returns the server-side IR runner API
     * ({@code POST /api/v1/api-tester/flow-runs}).
     *
     * <p>Pairs with the canonical Mockarty IR ({@code internal/iruir}).
     * Lets a caller ship a Flow document at the server and receive an
     * aggregated RunResult without a local goja runtime.</p>
     */
    public FlowRunsApi flowRuns() {
        return new FlowRunsApi(this);
    }

    /**
     * Returns the Secrets Storage API — namespace-scoped encrypted
     * key/value stores with optional Vault backend (Phase A0).
     */
    public SecretsApi secrets() {
        return new SecretsApi(this);
    }

    /**
     * Returns the Prompts Storage API — managed AI prompts with FIFO-20
     * version history and rollback.
     */
    public PromptsApi prompts() {
        return new PromptsApi(this);
    }

    /**
     * Returns the Security Agent API (CI/CD-useful subset).
     *
     * <p>Start scans, poll status, list findings, download SARIF, list
     * scanners, cancel scans. Gated by the {@code security_agent}
     * licence feature; admin operations live in the UI.</p>
     */
    public SecurityApi security() {
        return new SecurityApi(this);
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

    /**
     * Returns the underlying HTTP client. Used by the MCP client, which needs
     * low-level control over the request (streaming Accept header, session id)
     * that the typed {@code get}/{@code post} helpers do not expose.
     */
    public HttpClient getHttpClient() {
        return httpClient;
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

    /** Performs a POST with narrow caller-supplied idempotency or conditional headers. */
    public <T> T postWithHeaders(String path, Object body, Class<T> responseType,
                                 Map<String, String> headers) throws MockartyException {
        HttpRequest.Builder builder = buildRequest(path)
                .POST(jsonBody(body))
                .header("Content-Type", "application/json");
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (value != null && !value.isBlank()) {
                    builder.header(name, value);
                }
            });
        }
        return execute(builder.build(), responseType);
    }

    /** Performs an in-memory multipart file upload with conditional headers. */
    public <T> T postMultipartFileWithHeaders(String path, String fieldName, String fileName,
                                               byte[] data, Class<T> responseType,
                                               Map<String, String> headers) throws MockartyException {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("multipart field name is required");
        }
        if (fileName == null || fileName.isBlank() || fileName.indexOf('\r') >= 0 || fileName.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("multipart file name must be non-empty and single-line");
        }
        if (data == null) {
            throw new IllegalArgumentException("multipart file data is required");
        }
        String boundary = "mockarty-" + UUID.randomUUID().toString().replace("-", "");
        String escapedName = fileName.replace("\\", "\\\\").replace("\"", "\\\"");
        String prefix = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + escapedName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";
        byte[] multipartBody;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(prefix.length() + data.length + suffix.length());
            out.write(prefix.getBytes(StandardCharsets.UTF_8));
            out.write(data);
            out.write(suffix.getBytes(StandardCharsets.US_ASCII));
            multipartBody = out.toByteArray();
        } catch (IOException e) {
            throw new MockartyException("encode multipart request", e);
        }
        HttpRequest.Builder builder = buildRequest(path)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (value != null && !value.isBlank()) {
                    builder.header(name, value);
                }
            });
        }
        return execute(builder.build(), responseType);
    }

    /** Performs a PATCH with narrow caller-supplied conditional headers. */
    public <T> T patchWithHeaders(String path, Object body, Class<T> responseType,
                                  Map<String, String> headers) throws MockartyException {
        HttpRequest.Builder builder = buildRequest(path)
                .method("PATCH", jsonBody(body))
                .header("Content-Type", "application/json");
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (value != null && !value.isBlank()) builder.header(name, value);
            });
        }
        return execute(builder.build(), responseType);
    }

    /** Performs a DELETE with narrow caller-supplied conditional headers. */
    public <T> T deleteWithHeaders(String path, Class<T> responseType,
                                   Map<String, String> headers) throws MockartyException {
        HttpRequest.Builder builder = buildRequest(path).DELETE();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (value != null && !value.isBlank()) builder.header(name, value);
            });
        }
        return execute(builder.build(), responseType);
    }

    /** Performs a conditional DELETE without expecting a response body. */
    public void deleteWithHeaders(String path, Map<String, String> headers) throws MockartyException {
        HttpRequest.Builder builder = buildRequest(path).DELETE();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (value != null && !value.isBlank()) builder.header(name, value);
            });
        }
        executeVoid(builder.build());
    }

    /**
     * Performs a POST with a raw byte body and an explicit Content-Type (e.g.
     * multipart/form-data), deserializing the response. Used for attachment
     * uploads where the body is not JSON.
     */
    public <T> T postRaw(String path, byte[] body, String contentType, Class<T> responseType)
            throws MockartyException {
        HttpRequest request = buildRequest(path)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body == null ? new byte[0] : body))
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
            HttpResponse<byte[]> response = sendWithRetry(request,
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

    /** Performs a PUT with narrow caller-supplied conditional/idempotency headers. */
    public <T> T putWithHeaders(String path, Object body, Class<T> responseType,
                                 Map<String, String> headers) throws MockartyException {
        HttpRequest.Builder builder = buildRequest(path)
                .PUT(jsonBody(body))
                .header("Content-Type", "application/json");
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (value != null && !value.isBlank()) {
                    builder.header(name, value);
                }
            });
        }
        return execute(builder.build(), responseType);
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
            HttpResponse<byte[]> response = sendWithRetry(request,
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

    // sendWithRetry centralises every httpClient.send call so transient
    // failures (network IOExceptions and HTTP 429/502/503/504) are retried
    // up to config.getMaxRetries() times with exponential backoff. The
    // BodyPublishers used here (ofString / ofByteArray) are re-subscribable,
    // so re-sending the same HttpRequest is safe.
    private <T> HttpResponse<T> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        int attempts = Math.max(1, config.getMaxRetries() + 1);
        IOException lastIO = null;
        for (int i = 0; i < attempts; i++) {
            try {
                HttpResponse<T> resp = httpClient.send(request, handler);
                if (i < attempts - 1 && isRetryableStatus(resp.statusCode())) {
                    log.debug("retrying {} {} after HTTP {} (attempt {}/{})",
                            request.method(), request.uri(), resp.statusCode(), i + 1, attempts);
                    sleepBackoff(i);
                    continue;
                }
                return resp;
            } catch (IOException e) {
                lastIO = e;
                if (i >= attempts - 1) {
                    throw e;
                }
                log.debug("retrying {} {} after I/O error: {} (attempt {}/{})",
                        request.method(), request.uri(), e.getMessage(), i + 1, attempts);
                sleepBackoff(i);
            }
        }
        // Unreachable: the loop either returns a response or throws lastIO.
        throw lastIO != null ? lastIO : new IOException("retry budget exhausted");
    }

    private static boolean isRetryableStatus(int status) {
        return status == 429 || status == 502 || status == 503 || status == 504;
    }

    private static void sleepBackoff(int attempt) throws InterruptedException {
        // 200ms, 400ms, 800ms, … capped at 2s.
        long ms = Math.min(2000L, 200L * (1L << Math.min(attempt, 10)));
        Thread.sleep(ms);
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
            HttpResponse<String> response = sendWithRetry(request,
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
        SimpleModule timeModule = new SimpleModule();
        timeModule.addDeserializer(Instant.class, new JsonDeserializer<>() {
            @Override
            public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String value = parser.getValueAsString();
                try {
                    return Instant.parse(value);
                } catch (RuntimeException ex) {
                    return (Instant) context.handleWeirdStringValue(Instant.class, value,
                            "expected an RFC 3339 timestamp");
                }
            }
        });
        mapper.registerModule(timeModule);
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
        private Integer maxRetries;
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
         * Sets the maximum number of automatic retries on transient failures
         * (network errors and HTTP 429/502/503/504). {@code 0} disables
         * retries. Defaults to 2.
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Builds the MockartyClient with the configured settings.
         */
        public MockartyClient build() {
            MockartyConfig config = MockartyConfig.create(baseUrl, apiKey, namespace, timeout, maxRetries);
            return new MockartyClient(config, httpClient);
        }
    }
}
