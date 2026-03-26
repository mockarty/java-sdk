// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.ImportResult;

import java.util.Map;

/**
 * API for importing mocks from various formats (Postman, OpenAPI, WSDL, HAR, Proto, GraphQL, MCP, Mockarty).
 */
public class ImportApi {

    private final MockartyClient client;

    public ImportApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Imports mocks from a Postman collection.
     *
     * @param content   the Postman collection JSON content
     * @param namespace the target namespace (null for default)
     * @return the import result
     */
    public ImportResult postman(String content, String namespace) throws MockartyException {
        return doImport("/api/v1/api-tester/import/postman", content, namespace);
    }

    /**
     * Imports mocks from an OpenAPI/Swagger specification.
     *
     * @param content   the OpenAPI spec content
     * @param namespace the target namespace (null for default)
     * @return the import result
     */
    public ImportResult openAPI(String content, String namespace) throws MockartyException {
        return doImport("/api/v1/api-tester/import/openapi", content, namespace);
    }

    /**
     * Imports mocks from a WSDL specification.
     *
     * @param content   the WSDL content
     * @param namespace the target namespace (null for default)
     * @return the import result
     */
    public ImportResult wsdl(String content, String namespace) throws MockartyException {
        return doImport("/api/v1/api-tester/import/wsdl", content, namespace);
    }

    /**
     * Imports mocks from a HAR (HTTP Archive) file.
     *
     * @param content   the HAR file content
     * @param namespace the target namespace (null for default)
     * @return the import result
     */
    public ImportResult har(String content, String namespace) throws MockartyException {
        return doImport("/api/v1/api-tester/import/har", content, namespace);
    }

    /**
     * Imports mocks from a Protocol Buffers (.proto) file.
     *
     * @param content   the proto file content
     * @param namespace the target namespace (null for default)
     * @return the import result
     */
    public ImportResult grpcProto(String content, String namespace) throws MockartyException {
        return doImport("/api/v1/api-tester/import/grpc", content, namespace);
    }

    /**
     * Imports mocks from a GraphQL schema.
     *
     * @param content   the GraphQL schema content
     * @param namespace the target namespace (null for default)
     * @return the import result
     */
    public ImportResult graphQL(String content, String namespace) throws MockartyException {
        return doImport("/api/v1/api-tester/import/graphql", content, namespace);
    }

    /**
     * Imports mocks from an MCP definition.
     *
     * @param content   the MCP definition content
     * @param namespace the target namespace (null for default)
     * @return the import result
     */
    public ImportResult mcp(String content, String namespace) throws MockartyException {
        return doImport("/api/v1/api-tester/import/mcp", content, namespace);
    }

    /**
     * Imports mocks from a Mockarty export file.
     *
     * @param content   the Mockarty export JSON content
     * @param namespace the target namespace (null for default)
     * @return the import result
     */
    public ImportResult mockarty(String content, String namespace) throws MockartyException {
        return doImport("/api/v1/api-tester/import/mockarty", content, namespace);
    }

    private ImportResult doImport(String path, String content, String namespace) throws MockartyException {
        String ns = namespace != null ? namespace : client.getConfig().getNamespace();
        Map<String, Object> body = Map.of(
                "content", content,
                "namespace", ns
        );
        return client.post(path, body, ImportResult.class);
    }
}
