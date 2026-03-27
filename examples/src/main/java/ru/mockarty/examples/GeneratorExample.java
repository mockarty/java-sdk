// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.GeneratorPreview;
import ru.mockarty.model.GeneratorRequest;
import ru.mockarty.model.GeneratorResponse;
import ru.mockarty.model.ImportResult;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mock generation examples showing how to generate mocks from
 * OpenAPI, WSDL, Proto, GraphQL schemas, and HAR files.
 *
 * <p>Also demonstrates importing from Postman and other formats.</p>
 */
public class GeneratorExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            generateFromOpenAPI(client);
            generateFromProto(client);
            generateFromGraphQL(client);
            generateFromWSDL(client);
            previewBeforeGenerating(client);
            importFromPostman(client);
        }
    }

    /**
     * Generate mocks from an OpenAPI/Swagger specification.
     */
    static void generateFromOpenAPI(MockartyClient client) {
        String openApiSpec = """
                openapi: '3.0.0'
                info:
                  title: Pet Store API
                  version: '1.0.0'
                servers:
                  - url: http://localhost:8080
                paths:
                  /pets:
                    get:
                      summary: List all pets
                      operationId: listPets
                      responses:
                        '200':
                          description: A list of pets
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
                                  $ref: '#/components/schemas/Pet'
                    post:
                      summary: Create a pet
                      operationId: createPet
                      requestBody:
                        content:
                          application/json:
                            schema:
                              $ref: '#/components/schemas/Pet'
                      responses:
                        '201':
                          description: Pet created
                  /pets/{petId}:
                    get:
                      summary: Get a pet by ID
                      operationId: getPetById
                      parameters:
                        - name: petId
                          in: path
                          required: true
                          schema:
                            type: string
                      responses:
                        '200':
                          description: A pet
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Pet'
                components:
                  schemas:
                    Pet:
                      type: object
                      required:
                        - id
                        - name
                      properties:
                        id:
                          type: string
                        name:
                          type: string
                        species:
                          type: string
                        age:
                          type: integer
                """;

        GeneratorRequest request = new GeneratorRequest()
                .spec(openApiSpec)
                .namespace("sandbox")
                .pathPrefix("/petstore");

        GeneratorResponse response = client.generator().fromOpenAPI(request);
        System.out.println("Generated mocks from OpenAPI:");
        System.out.println("  Total mocks: " + response.getMockCount());
        System.out.println("  Mock IDs: " + response.getMockIds());
    }

    /**
     * Generate mocks from a Protocol Buffers (.proto) definition.
     */
    static void generateFromProto(MockartyClient client) {
        String protoContent = """
                syntax = "proto3";

                option go_package = "/example/userservice";

                service UserService {
                    rpc GetUser (GetUserRequest) returns (User);
                    rpc ListUsers (ListUsersRequest) returns (ListUsersResponse);
                    rpc CreateUser (CreateUserRequest) returns (User);
                    rpc DeleteUser (DeleteUserRequest) returns (DeleteUserResponse);
                }

                message GetUserRequest {
                    string user_id = 1;
                }

                message ListUsersRequest {
                    int32 page_size = 1;
                    string page_token = 2;
                }

                message ListUsersResponse {
                    repeated User users = 1;
                    string next_page_token = 2;
                }

                message CreateUserRequest {
                    string name = 1;
                    string email = 2;
                    string role = 3;
                }

                message DeleteUserRequest {
                    string user_id = 1;
                }

                message DeleteUserResponse {
                    bool success = 1;
                }

                message User {
                    string user_id = 1;
                    string name = 2;
                    string email = 3;
                    string role = 4;
                    int64 created_at = 5;
                }
                """;

        GeneratorRequest request = new GeneratorRequest()
                .protoContent(protoContent)
                .namespace("sandbox");

        GeneratorResponse response = client.generator().fromProto(request);
        System.out.println("Generated mocks from Proto:");
        System.out.println("  Total mocks: " + response.getMockCount());
    }

    /**
     * Generate mocks from a GraphQL schema.
     */
    static void generateFromGraphQL(MockartyClient client) {
        String graphqlSchema = """
                type Query {
                    user(id: ID!): User
                    users(limit: Int, offset: Int): [User!]!
                    product(id: ID!): Product
                    products(category: String): [Product!]!
                }

                type Mutation {
                    createUser(input: CreateUserInput!): User!
                    updateUser(id: ID!, input: UpdateUserInput!): User!
                    deleteUser(id: ID!): Boolean!
                }

                type User {
                    id: ID!
                    name: String!
                    email: String!
                    orders: [Order!]!
                }

                type Product {
                    id: ID!
                    name: String!
                    price: Float!
                    inStock: Boolean!
                }

                type Order {
                    id: ID!
                    total: Float!
                    status: OrderStatus!
                }

                enum OrderStatus {
                    PENDING
                    PROCESSING
                    SHIPPED
                    DELIVERED
                }

                input CreateUserInput {
                    name: String!
                    email: String!
                }

                input UpdateUserInput {
                    name: String
                    email: String
                }
                """;

        GeneratorRequest request = new GeneratorRequest()
                .spec(graphqlSchema)
                .namespace("sandbox");

        GeneratorResponse response = client.generator().fromGraphQL(request);
        System.out.println("Generated mocks from GraphQL schema:");
        System.out.println("  Total mocks: " + response.getMockCount());
    }

    /**
     * Generate mocks from a WSDL specification.
     */
    static void generateFromWSDL(MockartyClient client) {
        String wsdlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions name="CalculatorService"
                    xmlns="http://schemas.xmlsoap.org/wsdl/"
                    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
                    xmlns:tns="http://example.com/calculator"
                    targetNamespace="http://example.com/calculator">

                    <types>
                        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                            <xs:element name="AddRequest">
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name="a" type="xs:double"/>
                                        <xs:element name="b" type="xs:double"/>
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                            <xs:element name="AddResponse">
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name="result" type="xs:double"/>
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:schema>
                    </types>

                    <message name="AddInput">
                        <part name="parameters" element="tns:AddRequest"/>
                    </message>
                    <message name="AddOutput">
                        <part name="parameters" element="tns:AddResponse"/>
                    </message>

                    <portType name="CalculatorPortType">
                        <operation name="Add">
                            <input message="tns:AddInput"/>
                            <output message="tns:AddOutput"/>
                        </operation>
                    </portType>

                    <binding name="CalculatorBinding" type="tns:CalculatorPortType">
                        <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
                        <operation name="Add">
                            <soap:operation soapAction="http://example.com/calculator/Add"/>
                        </operation>
                    </binding>
                </definitions>
                """;

        GeneratorRequest request = new GeneratorRequest()
                .wsdlContent(wsdlContent)
                .namespace("sandbox");

        GeneratorResponse response = client.generator().fromWSDL(request);
        System.out.println("Generated mocks from WSDL:");
        System.out.println("  Total mocks: " + response.getMockCount());
    }

    /**
     * Preview mock generation without actually creating mocks.
     * Useful for dry-run inspection before committing.
     */
    static void previewBeforeGenerating(MockartyClient client) {
        String spec = """
                openapi: '3.0.0'
                info:
                  title: Preview API
                  version: '1.0.0'
                paths:
                  /items:
                    get:
                      responses:
                        '200':
                          description: List items
                """;

        GeneratorRequest request = new GeneratorRequest()
                .spec(spec)
                .namespace("sandbox");

        // Preview shows what would be created without actually creating mocks
        GeneratorPreview preview = client.generator().previewOpenAPI(request);
        System.out.println("Preview result:");
        System.out.println("  Mocks that would be created: " + preview.getMockCount());
        System.out.println("  Mock details: " + preview.getMocks());
    }

    /**
     * Import mocks from a Postman collection.
     */
    static void importFromPostman(MockartyClient client) {
        String postmanCollection = """
                {
                    "info": {
                        "name": "My API Collection",
                        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
                    },
                    "item": [
                        {
                            "name": "Get Users",
                            "request": {
                                "method": "GET",
                                "url": "http://localhost:8080/api/users",
                                "header": [
                                    {"key": "Accept", "value": "application/json"}
                                ]
                            },
                            "response": [
                                {
                                    "name": "Success",
                                    "status": "OK",
                                    "code": 200,
                                    "body": "[{\\"id\\": 1, \\"name\\": \\"John\\"}]"
                                }
                            ]
                        },
                        {
                            "name": "Create User",
                            "request": {
                                "method": "POST",
                                "url": "http://localhost:8080/api/users",
                                "header": [
                                    {"key": "Content-Type", "value": "application/json"}
                                ],
                                "body": {
                                    "mode": "raw",
                                    "raw": "{\\"name\\": \\"Jane\\", \\"email\\": \\"jane@example.com\\"}"
                                }
                            },
                            "response": [
                                {
                                    "name": "Created",
                                    "status": "Created",
                                    "code": 201,
                                    "body": "{\\"id\\": 2, \\"name\\": \\"Jane\\"}"
                                }
                            ]
                        }
                    ]
                }
                """;

        ImportResult result = client.imports().postman(postmanCollection, "sandbox");
        System.out.println("Imported from Postman:");
        System.out.println("  Total: " + result.getTotal());
        System.out.println("  Created: " + result.getCreated());
    }
}
