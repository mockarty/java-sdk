// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.CanIDeployResult;
import ru.mockarty.model.Contract;
import ru.mockarty.model.ContractValidationResult;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Pact;
import ru.mockarty.model.PactVerificationResult;

import java.util.List;
import java.util.Map;

/**
 * Contract testing examples showing mock validation against API specifications,
 * provider verification, compatibility checking, and pact-based contract testing.
 */
public class ContractsExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            validateMocksAgainstSpec(client);
            verifyProvider(client);
            checkCompatibility(client);
            validatePayload(client);
            manageContractConfigs(client);
            pactWorkflow(client);
            driftDetection(client);
        }
    }

    /**
     * Validate that existing mocks conform to an OpenAPI specification.
     * Checks that mock responses match the declared schema.
     */
    static void validateMocksAgainstSpec(MockartyClient client) {
        String openApiSpec = """
                openapi: '3.0.0'
                info:
                  title: User Service API
                  version: '1.0.0'
                paths:
                  /api/users/{id}:
                    get:
                      parameters:
                        - name: id
                          in: path
                          required: true
                          schema:
                            type: string
                      responses:
                        '200':
                          description: User found
                          content:
                            application/json:
                              schema:
                                type: object
                                required:
                                  - id
                                  - name
                                  - email
                                properties:
                                  id:
                                    type: string
                                  name:
                                    type: string
                                  email:
                                    type: string
                                    format: email
                """;

        Map<String, Object> result = client.contracts().validateMocks(Map.of(
                "spec", openApiSpec,
                "namespace", "sandbox",
                "mockIds", List.of("http-get-user", "http-faker-demo")
        ));

        System.out.println("Mock validation result: " + result);
        System.out.println("  Valid: " + result.get("valid"));
        System.out.println("  Violations: " + result.get("violations"));
    }

    /**
     * Verify that a real provider implementation matches a contract.
     * Sends requests to the provider and validates responses against the spec.
     */
    static void verifyProvider(MockartyClient client) {
        Map<String, Object> result = client.contracts().verifyProvider(Map.of(
                "providerUrl", "http://localhost:8080",
                "spec", Map.of(
                        "openapi", "3.0.0",
                        "info", Map.of("title", "User Service", "version", "1.0.0"),
                        "paths", Map.of(
                                "/api/users", Map.of(
                                        "get", Map.of(
                                                "responses", Map.of(
                                                        "200", Map.of(
                                                                "description", "List users",
                                                                "content", Map.of(
                                                                        "application/json", Map.of(
                                                                                "schema", Map.of(
                                                                                        "type", "array",
                                                                                        "items", Map.of(
                                                                                                "type", "object",
                                                                                                "properties", Map.of(
                                                                                                        "id", Map.of("type", "string"),
                                                                                                        "name", Map.of("type", "string")
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                "headers", Map.of("Authorization", "Bearer test-token")
        ));

        System.out.println("Provider verification: " + result);
    }

    /**
     * Check compatibility between consumer expectations and provider capabilities.
     */
    static void checkCompatibility(MockartyClient client) {
        Map<String, Object> result = client.contracts().checkCompatibility(Map.of(
                "consumerSpec", Map.of(
                        "service", "order-service",
                        "expectations", List.of(
                                Map.of(
                                        "method", "GET",
                                        "path", "/api/users/{id}",
                                        "expectedFields", List.of("id", "name", "email")
                                )
                        )
                ),
                "providerSpec", Map.of(
                        "service", "user-service",
                        "capabilities", List.of(
                                Map.of(
                                        "method", "GET",
                                        "path", "/api/users/{id}",
                                        "providedFields", List.of("id", "name", "email", "phone", "address")
                                )
                        )
                )
        ));

        System.out.println("Compatibility check: " + result);
        System.out.println("  Compatible: " + result.get("compatible"));
    }

    /**
     * Validate a specific payload against a contract schema.
     */
    static void validatePayload(MockartyClient client) {
        Map<String, Object> result = client.contracts().validatePayload(Map.of(
                "schema", Map.of(
                        "type", "object",
                        "required", List.of("id", "name", "email"),
                        "properties", Map.of(
                                "id", Map.of("type", "string"),
                                "name", Map.of("type", "string", "minLength", 1),
                                "email", Map.of("type", "string", "format", "email"),
                                "age", Map.of("type", "integer", "minimum", 0, "maximum", 200)
                        )
                ),
                "payload", Map.of(
                        "id", "user-123",
                        "name", "John Doe",
                        "email", "john@example.com",
                        "age", 30
                )
        ));

        System.out.println("Payload validation: " + result);
        System.out.println("  Valid: " + result.get("valid"));
    }

    /**
     * Manage contract configurations and results.
     */
    static void manageContractConfigs(MockartyClient client) {
        // List all contract configs
        List<Contract> contracts = client.contracts().listConfigs();
        System.out.println("Contract configs: " + contracts.size());

        // List all validation results
        List<ContractValidationResult> results = client.contracts().listResults();
        System.out.println("Validation results: " + results.size());

        for (ContractValidationResult result : results) {
            System.out.println("  Result: " + result.getId());
            System.out.println("    Status: " + result.getStatus());
        }

        // Get a specific result
        if (!results.isEmpty()) {
            ContractValidationResult detail = client.contracts().getResult(results.get(0).getId());
            System.out.println("Detailed result: " + detail);
        }
    }

    /**
     * Full pact-based consumer-driven contract testing workflow.
     * Publish pacts, verify against providers, check deployment safety,
     * and generate mocks from pacts.
     */
    static void pactWorkflow(MockartyClient client) {
        System.out.println("\n=== Pact Workflow ===");

        // 1. Publish a consumer pact
        Pact pact = new Pact()
                .consumer("order-service")
                .provider("user-service")
                .version("1.2.0")
                .interactions(List.of(
                        Map.of(
                                "description", "get user by id",
                                "request", Map.of(
                                        "method", "GET",
                                        "path", "/api/users/user-123"
                                ),
                                "response", Map.of(
                                        "status", 200,
                                        "headers", Map.of("Content-Type", "application/json"),
                                        "body", Map.of(
                                                "id", "user-123",
                                                "name", "John Doe",
                                                "email", "john@example.com"
                                        )
                                )
                        ),
                        Map.of(
                                "description", "user not found",
                                "request", Map.of(
                                        "method", "GET",
                                        "path", "/api/users/nonexistent"
                                ),
                                "response", Map.of(
                                        "status", 404,
                                        "body", Map.of("error", "User not found")
                                )
                        )
                ));

        Pact published = client.contracts().publishPact(pact);
        System.out.println("Published pact: " + published.getId());
        System.out.println("  Consumer: " + published.getConsumer());
        System.out.println("  Provider: " + published.getProvider());

        // 2. List all pacts
        List<Pact> pacts = client.contracts().listPacts();
        System.out.println("Total pacts: " + pacts.size());
        for (Pact p : pacts) {
            System.out.println("  " + p.getConsumer() + " -> " + p.getProvider() +
                    " (v" + p.getVersion() + ")");
        }

        // 3. Get pact details
        Pact detail = client.contracts().getPact(published.getId());
        System.out.println("Pact details: " + detail.getInteractions().size() + " interactions");

        // 4. Verify the pact against the provider
        PactVerificationResult verification = client.contracts().verifyPact(Map.of(
                "pactId", published.getId(),
                "providerUrl", "http://localhost:8080",
                "providerVersion", "2.0.0"
        ));
        System.out.println("Pact verification: " + verification.getStatus());
        System.out.println("  Success: " + verification.isSuccess());

        // 5. List all verifications
        List<PactVerificationResult> verifications = client.contracts().listVerifications();
        System.out.println("Total verifications: " + verifications.size());

        // 6. Can I deploy? -- check if it's safe to deploy a service version
        CanIDeployResult deployCheck = client.contracts().canIDeploy(Map.of(
                "application", "order-service",
                "version", "1.2.0",
                "to", "production"
        ));
        System.out.println("Can I deploy? " + deployCheck.isDeployable());
        System.out.println("  Reason: " + deployCheck.getReason());

        // 7. Generate mocks from pact interactions
        List<Mock> generatedMocks = client.contracts().generateMocksFromPact(published.getId());
        System.out.println("Generated " + generatedMocks.size() + " mocks from pact:");
        for (Mock mock : generatedMocks) {
            System.out.println("  - " + mock.getId() + " (" + mock.getHttp().getRoute() + ")");
        }

        // 8. Cleanup
        // client.contracts().deletePact(published.getId());
        // System.out.println("Deleted pact");
    }

    /**
     * Detect drift between contracts and current API implementations.
     */
    static void driftDetection(MockartyClient client) {
        System.out.println("\n=== Drift Detection ===");

        Map<String, Object> driftResult = client.contracts().detectDrift(Map.of(
                "providerUrl", "http://localhost:8080",
                "namespace", "sandbox",
                "includeDeprecated", false
        ));

        System.out.println("Drift detection result:");
        System.out.println("  Drifted: " + driftResult.get("hasDrift"));
        System.out.println("  Details: " + driftResult.get("driftDetails"));
    }
}
