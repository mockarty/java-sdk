// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CanIDeployResult;
import ru.mockarty.model.CheckCompatibilityRequest;
import ru.mockarty.model.Contract;
import ru.mockarty.model.ContractValidationResult;
import ru.mockarty.model.DriftDetectionRequest;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Pact;
import ru.mockarty.model.PactVerificationResult;
import ru.mockarty.model.PactVerifyRequest;
import ru.mockarty.model.ValidatePayloadRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for contract testing operations.
 *
 * <p>The contract API provides mock validation, provider verification,
 * compatibility checking, payload validation, and contract config management.</p>
 */
public class ContractApi {

    private final MockartyClient client;

    public ContractApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Validates mocks against a contract specification.
     *
     * @param request the validation request (mockIds, spec, etc.)
     * @return the validation result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validateMocks(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/validate-mocks", request, Map.class);
    }

    /**
     * Verifies a provider against a contract.
     *
     * @param request the verification request (providerUrl, spec, etc.)
     * @return the verification result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyProvider(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/verify-provider", request, Map.class);
    }

    /**
     * Checks backward compatibility between two spec versions.
     *
     * @param request the compatibility check request
     * @return the compatibility result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkCompatibility(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/check-compatibility", request, Map.class);
    }

    /**
     * Checks backward compatibility between two spec versions (typed).
     *
     * @param request the typed compatibility check request
     * @return the compatibility result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkCompatibility(CheckCompatibilityRequest request) throws MockartyException {
        return client.post("/api/v1/contract/check-compatibility", request, Map.class);
    }

    /**
     * Validates a payload against a contract specification.
     *
     * @param request the payload validation request
     * @return the validation result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validatePayload(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/validate-payload", request, Map.class);
    }

    /**
     * Validates a payload against a contract specification (typed).
     *
     * @param request the typed payload validation request
     * @return the validation result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validatePayload(ValidatePayloadRequest request) throws MockartyException {
        return client.post("/api/v1/contract/validate-payload", request, Map.class);
    }

    // ---- Contract Configs ----

    /**
     * Lists all contract configurations.
     *
     * @return list of contract configs
     */
    public List<Contract> listConfigs() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Contract.class);
        return client.get("/api/v1/contract/configs", listType);
    }

    /**
     * Creates a new contract configuration.
     *
     * @param config the contract config to create
     * @return the created config
     */
    public Contract createConfig(Contract config) throws MockartyException {
        return client.post("/api/v1/contract/configs", config, Contract.class);
    }

    /**
     * Deletes a contract configuration.
     *
     * @param id the config ID to delete
     */
    public void deleteConfig(String id) throws MockartyException {
        client.delete("/api/v1/contract/configs/" + encode(id));
    }

    // ---- Contract Results ----

    /**
     * Lists all contract validation results.
     *
     * @return list of validation results
     */
    public List<ContractValidationResult> listResults() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, ContractValidationResult.class);
        return client.get("/api/v1/contract/results", listType);
    }

    /**
     * Gets a specific contract validation result by ID.
     *
     * @param id the result ID
     * @return the validation result
     */
    public ContractValidationResult getResult(String id) throws MockartyException {
        return client.get("/api/v1/contract/results/" + encode(id), ContractValidationResult.class);
    }

    // ---- Pacts ----

    /**
     * Lists all pacts.
     *
     * @return list of pacts
     */
    public List<Pact> listPacts() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Pact.class);
        return client.get("/api/v1/contract/pacts", listType);
    }

    /**
     * Gets a specific pact by ID.
     *
     * @param id the pact ID
     * @return the pact
     */
    public Pact getPact(String id) throws MockartyException {
        return client.get("/api/v1/contract/pacts/" + encode(id), Pact.class);
    }

    /**
     * Publishes a new pact.
     *
     * @param pact the pact to publish
     * @return the published pact
     */
    public Pact publishPact(Pact pact) throws MockartyException {
        return client.post("/api/v1/contract/pacts", pact, Pact.class);
    }

    /**
     * Verifies a pact against a provider.
     *
     * @param request the verification request
     * @return the verification result
     */
    public PactVerificationResult verifyPact(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/pacts/verify", request, PactVerificationResult.class);
    }

    /**
     * Verifies a pact against a provider (typed).
     *
     * @param request the typed pact verification request with message callback support
     * @return the verification result
     */
    public PactVerificationResult verifyPact(PactVerifyRequest request) throws MockartyException {
        return client.post("/api/v1/contract/pacts/verify", request, PactVerificationResult.class);
    }

    /**
     * Checks if it is safe to deploy based on pact verification status.
     *
     * @param request the can-I-deploy request
     * @return the deployment check result
     */
    public CanIDeployResult canIDeploy(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/pacts/can-i-deploy", request, CanIDeployResult.class);
    }

    /**
     * Deletes a pact.
     *
     * @param id the pact ID to delete
     */
    public void deletePact(String id) throws MockartyException {
        client.delete("/api/v1/contract/pacts/" + encode(id));
    }

    /**
     * Generates mocks from a pact.
     *
     * @param pactId the pact ID
     * @return list of generated mocks
     */
    public List<Mock> generateMocksFromPact(String pactId) throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Mock.class);
        return client.post("/api/v1/contract/pacts/" + encode(pactId) + "/mocks", null, listType);
    }

    /**
     * Lists all pact verifications.
     *
     * @return list of verification results
     */
    public List<PactVerificationResult> listVerifications() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, PactVerificationResult.class);
        return client.get("/api/v1/contract/pacts/verifications", listType);
    }

    /**
     * Detects drift between mocks and the live service.
     *
     * @param request the drift detection request
     * @return drift detection results
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectDrift(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift", request, Map.class);
    }

    /**
     * Detects drift between mocks and the live service (typed).
     *
     * @param request the typed drift detection request
     * @return drift detection results
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectDrift(DriftDetectionRequest request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift", request, Map.class);
    }

    /** Detect GraphQL schema drift via introspection. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectGraphQLDrift(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift/graphql", request, Map.class);
    }

    /** Detect GraphQL schema drift via introspection (typed). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectGraphQLDrift(DriftDetectionRequest request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift/graphql", request, Map.class);
    }

    /** Detect gRPC service drift via reflection. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectGRPCDrift(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift/grpc", request, Map.class);
    }

    /** Detect gRPC service drift via reflection (typed). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectGRPCDrift(DriftDetectionRequest request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift/grpc", request, Map.class);
    }

    /** Detect SOAP/WSDL service drift. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectWSDLDrift(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift/wsdl", request, Map.class);
    }

    /** Detect SOAP/WSDL service drift (typed). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectWSDLDrift(DriftDetectionRequest request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift/wsdl", request, Map.class);
    }

    /** Detect MCP server drift. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectMCPDrift(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift/mcp", request, Map.class);
    }

    /** Detect MCP server drift (typed). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectMCPDrift(DriftDetectionRequest request) throws MockartyException {
        return client.post("/api/v1/contract/detect-drift/mcp", request, Map.class);
    }

    // ─── API Registry ─────────────────────────────────────────────

    /**
     * List published APIs in the registry.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listRegistry(String query) throws MockartyException {
        String path = "/api/v1/contract/registry";
        if (query != null && !query.isEmpty()) {
            path += "?q=" + encode(query);
        }
        return client.get(path, List.class);
    }

    /**
     * Get a single registry entry by ID.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRegistryEntry(String id) throws MockartyException {
        return client.get("/api/v1/contract/registry/" + id, Map.class);
    }

    /**
     * Publish an API specification to the internal registry.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> publishToRegistry(Map<String, Object> entry) throws MockartyException {
        return client.post("/api/v1/contract/registry", entry, Map.class);
    }

    /**
     * Update an existing registry entry.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateRegistryEntry(String id, Map<String, Object> update) throws MockartyException {
        return client.put("/api/v1/contract/registry/" + id, update, Map.class);
    }

    /**
     * Delete a registry entry.
     */
    public void deleteRegistryEntry(String id) throws MockartyException {
        client.delete("/api/v1/contract/registry/" + id);
    }

    /**
     * Generate mocks from a registry entry specification.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateMocksFromRegistry(String entryId) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + entryId + "/generate-mocks", null, Map.class);
    }

    /**
     * Check which subscribers would be affected by a spec change.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkImpact(String entryId, String newSpecContent) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + entryId + "/check-impact",
            Map.of("newSpecContent", newSpecContent), Map.class);
    }

    // ─── Subscriptions ────────────────────────────────────────────

    /**
     * List current namespace's subscriptions.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listSubscriptions() throws MockartyException {
        return client.get("/api/v1/contract/subscriptions", List.class);
    }

    /**
     * Subscribe to a registry API.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> subscribe(String registryEntryId, Map<String, Object> subscription) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + registryEntryId + "/subscribe", subscription, Map.class);
    }

    /**
     * Remove a subscription.
     */
    public void unsubscribe(String subscriptionId) throws MockartyException {
        client.delete("/api/v1/contract/subscriptions/" + subscriptionId);
    }

    /**
     * List subscribers of a specific API.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listSubscribers(String registryEntryId) throws MockartyException {
        return client.get("/api/v1/contract/registry/" + registryEntryId + "/subscribers", List.class);
    }

    // ─── Change Requests ──────────────────────────────────────

    /**
     * Submit a spec change for review.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createChangeRequest(String registryEntryId, String newSpecContent, String newVersion) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + registryEntryId + "/change-requests",
            Map.of("newSpecContent", newSpecContent, "newVersion", newVersion != null ? newVersion : ""), Map.class);
    }

    /**
     * List change requests for a registry entry.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listChangeRequests(String registryEntryId) throws MockartyException {
        return client.get("/api/v1/contract/registry/" + registryEntryId + "/change-requests", List.class);
    }

    /**
     * Approve a change request.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> approveChangeRequest(String crId, String comment) throws MockartyException {
        return client.post("/api/v1/contract/change-requests/" + crId + "/approve",
            Map.of("comment", comment != null ? comment : ""), Map.class);
    }

    /**
     * Reject a change request.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rejectChangeRequest(String crId, String comment) throws MockartyException {
        return client.post("/api/v1/contract/change-requests/" + crId + "/reject",
            Map.of("comment", comment != null ? comment : ""), Map.class);
    }

    /**
     * List change requests awaiting my team's approval.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> pendingChangeRequests() throws MockartyException {
        return client.get("/api/v1/contract/change-requests/pending", List.class);
    }

        /**
     * Get validation trend data for the past N days.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTrends(int days) throws MockartyException {
        return client.get("/api/v1/contract/trends?days=" + days, List.class);
    }

        /**
     * Get unique consumer/provider names from pacts.
     */
    @SuppressWarnings("unchecked")
    public List<String> getParticipants() throws MockartyException {
        return client.get("/api/v1/contract/pacts/participants", List.class);
    }

        /**
     * Validate mocks against a registry entry specification.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validateFromRegistry(String entryId) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + entryId + "/validate", null, Map.class);
    }

        @SuppressWarnings("unchecked")
    public Map<String, Object> submitForReview(String entryId, String reviewerId) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + entryId + "/submit-review", Map.of("reviewerId", reviewerId != null ? reviewerId : ""), Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> approveReview(String entryId, String comment) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + entryId + "/approve-review", Map.of("comment", comment != null ? comment : ""), Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> rejectReview(String entryId, String comment) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + entryId + "/reject-review", Map.of("comment", comment != null ? comment : ""), Map.class);
    }

    /** Assign a reviewer to a registry entry. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> assignReviewer(String entryId, String reviewerId) throws MockartyException {
        return client.put("/api/v1/contract/registry/" + entryId + "/reviewer", Map.of("reviewerId", reviewerId), Map.class);
    }

    // ── Consumer Contracts (Dependency Bundles) ────────────────────

    /** List all consumer contracts in the current namespace. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listConsumerContracts() throws MockartyException {
        return client.get("/api/v1/contract/consumer-contracts", List.class);
    }

    /** Get a consumer contract by ID. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConsumerContract(String contractId) throws MockartyException {
        return client.get("/api/v1/contract/consumer-contracts/" + encode(contractId), Map.class);
    }

    /** Create or update a consumer contract. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createConsumerContract(Map<String, Object> contract) throws MockartyException {
        return client.post("/api/v1/contract/consumer-contracts", contract, Map.class);
    }

    /** Delete a consumer contract. */
    public void deleteConsumerContract(String contractId) throws MockartyException {
        client.delete("/api/v1/contract/consumer-contracts/" + encode(contractId));
    }

    // ── Can I Deploy V2 (Bidirectional) ─────────────────────────────

    /** Bidirectional deployment readiness check. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> canIDeployV2(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/can-i-deploy", request, Map.class);
    }

    // ── Spec Parsing (Wizard Support) ───────────────────────────────

    /** Parse endpoints from a registry entry specification. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseEndpoints(String entryId) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + encode(entryId) + "/parse-endpoints", Map.of(), Map.class);
    }

    /** Parse response fields for a specific endpoint. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseFields(String entryId, String route, int statusCode) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + encode(entryId) + "/parse-fields",
            Map.of("route", route, "statusCode", statusCode), Map.class);
    }

    // ── Versioning ──────────────────────────────────────────────────

    /** List version history for a registry entry. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listRegistryVersions(String entryId) throws MockartyException {
        return client.get("/api/v1/contract/registry/" + encode(entryId) + "/versions", List.class);
    }

    /** Get a specific version of a registry entry. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRegistryVersion(String entryId, int version) throws MockartyException {
        return client.get("/api/v1/contract/registry/" + encode(entryId) + "/versions/" + version, Map.class);
    }

    /** Rollback a registry entry to a previous version. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rollbackRegistryVersion(String entryId, int version) throws MockartyException {
        return client.post("/api/v1/contract/registry/" + encode(entryId) + "/versions/" + version + "/rollback", Map.of(), Map.class);
    }

    /** Compute diff between two versions of a registry entry. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> diffRegistryVersions(String entryId, int v1, int v2) throws MockartyException {
        return client.get("/api/v1/contract/registry/" + encode(entryId) + "/versions/" + v1 + "/diff/" + v2, Map.class);
    }

    /** List version history for a consumer contract. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listConsumerContractVersions(String contractId) throws MockartyException {
        return client.get("/api/v1/contract/consumer-contracts/" + encode(contractId) + "/versions", List.class);
    }

    /** Get a specific version of a consumer contract. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConsumerContractVersion(String contractId, int version) throws MockartyException {
        return client.get("/api/v1/contract/consumer-contracts/" + encode(contractId) + "/versions/" + version, Map.class);
    }

    /** Rollback a consumer contract to a previous version. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rollbackConsumerContractVersion(String contractId, int version) throws MockartyException {
        return client.post("/api/v1/contract/consumer-contracts/" + encode(contractId) + "/versions/" + version + "/rollback", Map.of(), Map.class);
    }

    // ── Health ──────────────────────────────────────────────────────

    /** Get contract health status for the current namespace. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> health() throws MockartyException {
        return client.get("/api/v1/contract/health", Map.class);
    }

    // ── BDCT & Advanced Endpoints ─────────────────────────────────

    /** Run bidirectional contract testing (Pact vs provider spec). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> bdctVerify(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/bdct/verify", request, Map.class);
    }

    /** Parse field trees from inline spec content. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseJsonFields(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/parse-json-fields", request, Map.class);
    }

    /** Compare a consumer contract against another. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> diffConsumerContract(String contractId, Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/consumer-contracts/" + encode(contractId) + "/diff", request, Map.class);
    }

    /** Diff two versions of a consumer contract. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> diffConsumerContractVersions(String contractId, int v1, int v2) throws MockartyException {
        return client.get("/api/v1/contract/consumer-contracts/" + encode(contractId) + "/versions/" + v1 + "/diff/" + v2, Map.class);
    }

    /** List namespaces that have registry entries. */
    @SuppressWarnings("unchecked")
    public List<String> listRegistryNamespaces() throws MockartyException {
        return client.get("/api/v1/contract/registry/namespaces", List.class);
    }

    /** AI-assisted analysis of contract findings. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeFindings(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/findings/analyze", request, Map.class);
    }

    /** Auto-triage contract findings by severity. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> autoTriageFindings(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/findings/auto-triage", request, Map.class);
    }

    /** Export contract findings. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> exportFindings(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/contract/findings/export", request, Map.class);
    }

    /** Trigger immediate execution of a contract schedule. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> runConfig(String configId) throws MockartyException {
        return client.post("/api/v1/contract/configs/" + encode(configId) + "/run", null, Map.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
