// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.GeneratorPreview;
import ru.mockarty.model.GeneratorRequest;
import ru.mockarty.model.GeneratorResponse;

/**
 * API for generating mocks from API specifications (OpenAPI, WSDL, Proto, GraphQL, HAR).
 */
public class GeneratorApi {

    private final MockartyClient client;

    public GeneratorApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Generates mocks from an OpenAPI/Swagger specification.
     *
     * @param request the generator request containing the spec
     * @return the generation result
     */
    public GeneratorResponse fromOpenAPI(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/openapi", request, GeneratorResponse.class);
    }

    /**
     * Generates mocks from a WSDL/SOAP specification.
     *
     * @param request the generator request containing the WSDL
     * @return the generation result
     */
    public GeneratorResponse fromWSDL(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/soap", request, GeneratorResponse.class);
    }

    /**
     * Generates mocks from a Protocol Buffers (.proto) definition.
     *
     * @param request the generator request containing the proto
     * @return the generation result
     */
    public GeneratorResponse fromProto(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/grpc", request, GeneratorResponse.class);
    }

    /**
     * Generates mocks from a GraphQL schema or introspection endpoint.
     *
     * @param request the generator request containing the GraphQL schema
     * @return the generation result
     */
    public GeneratorResponse fromGraphQL(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/graphql", request, GeneratorResponse.class);
    }

    /**
     * Generates mocks from a HAR (HTTP Archive) file.
     *
     * @param request the generator request containing the HAR content
     * @return the generation result
     */
    public GeneratorResponse fromHAR(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/har", request, GeneratorResponse.class);
    }

    /**
     * Previews mock generation from an OpenAPI/Swagger specification without creating mocks.
     *
     * @param request the generator request containing the spec
     * @return the preview result
     */
    public GeneratorPreview previewOpenAPI(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/openapi/preview", request, GeneratorPreview.class);
    }

    /**
     * Previews mock generation from a WSDL/SOAP specification without creating mocks.
     *
     * @param request the generator request containing the WSDL
     * @return the preview result
     */
    public GeneratorPreview previewWSDL(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/soap/preview", request, GeneratorPreview.class);
    }

    /**
     * Previews mock generation from a Protocol Buffers (.proto) definition without creating mocks.
     *
     * @param request the generator request containing the proto
     * @return the preview result
     */
    public GeneratorPreview previewProto(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/grpc/preview", request, GeneratorPreview.class);
    }

    /**
     * Previews mock generation from a GraphQL schema without creating mocks.
     *
     * @param request the generator request containing the GraphQL schema
     * @return the preview result
     */
    public GeneratorPreview previewGraphQL(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/graphql/preview", request, GeneratorPreview.class);
    }

    /**
     * Previews mock generation from a HAR file without creating mocks.
     *
     * @param request the generator request containing the HAR content
     * @return the preview result
     */
    public GeneratorPreview previewHAR(GeneratorRequest request) throws MockartyException {
        return client.post("/api/v1/generators/har/preview", request, GeneratorPreview.class);
    }
}
