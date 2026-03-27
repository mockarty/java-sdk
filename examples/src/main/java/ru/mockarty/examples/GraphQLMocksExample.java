// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.ContentResponse;
import ru.mockarty.model.Mock;

import java.util.List;
import java.util.Map;

/**
 * GraphQL mock examples covering queries, mutations, subscriptions,
 * variables-based conditions, partial errors, and nested resolvers.
 */
public class GraphQLMocksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createQueryMock(client);
            createQueryWithVariables(client);
            createMutationMock(client);
            createGraphQLWithErrors(client);
            createGraphQLPartialError(client);
            createNestedQueryMock(client);
            createPaginatedQueryMock(client);
        }
    }

    /**
     * Simple GraphQL query mock.
     * Matches: query { user { ... } }
     */
    static void createQueryMock(MockartyClient client) {
        Mock mock = MockBuilder.graphql("query", "user")
                .id("graphql-query-user")
                .respond(200, Map.of(
                        "data", Map.of(
                                "user", Map.of(
                                        "id", "$.fake.UUID",
                                        "name", "$.fake.FirstName",
                                        "email", "$.fake.Email",
                                        "avatar", "$.fake.URL",
                                        "createdAt", "$.fake.DateISO"
                                )
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created GraphQL query mock");
    }

    /**
     * GraphQL query with variables-based conditions.
     * Matches when variables.id is present.
     */
    static void createQueryWithVariables(MockartyClient client) {
        Mock mock = MockBuilder.graphql("query", "userById")
                .id("graphql-query-user-by-id")
                .condition("variables.id", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "data", Map.of(
                                "userById", Map.of(
                                        "id", "$.req.variables.id",
                                        "name", "$.fake.FirstName",
                                        "email", "$.fake.Email",
                                        "department", "Engineering",
                                        "permissions", List.of("read", "write", "admin")
                                )
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created GraphQL query with variables condition");
    }

    /**
     * GraphQL mutation mock for creating a user.
     * Matches: mutation { createUser(input: { ... }) { ... } }
     */
    static void createMutationMock(MockartyClient client) {
        Mock mock = MockBuilder.graphql("mutation", "createUser")
                .id("graphql-mutation-create-user")
                .condition("variables.input.email", AssertAction.NOT_EMPTY, null)
                .condition("variables.input.name", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "data", Map.of(
                                "createUser", Map.of(
                                        "id", "$.fake.UUID",
                                        "name", "$.req.variables.input.name",
                                        "email", "$.req.variables.input.email",
                                        "createdAt", "$.fake.DateISO",
                                        "success", true
                                )
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created GraphQL mutation mock");
    }

    /**
     * GraphQL mock returning errors (e.g., authentication failure).
     */
    static void createGraphQLWithErrors(MockartyClient client) {
        Mock mock = MockBuilder.graphql("query", "adminDashboard")
                .id("graphql-auth-error")
                .respond(new ContentResponse()
                        .statusCode(200)
                        .payload(Map.of("data", Map.of()))
                        .graphqlErrors(List.of(
                                Map.of(
                                        "message", "Not authenticated",
                                        "extensions", Map.of(
                                                "code", "UNAUTHENTICATED",
                                                "statusCode", 401
                                        ),
                                        "path", List.of("adminDashboard")
                                )
                        ))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created GraphQL error mock");
    }

    /**
     * GraphQL mock with partial data and errors.
     * Returns some fields successfully while others fail.
     */
    static void createGraphQLPartialError(MockartyClient client) {
        Mock mock = MockBuilder.graphql("query", "userWithOrders")
                .id("graphql-partial-error")
                .respond(new ContentResponse()
                        .statusCode(200)
                        .payload(Map.of(
                                "data", Map.of(
                                        "user", Map.of(
                                                "id", "$.fake.UUID",
                                                "name", "$.fake.FirstName"
                                        ),
                                        "orders", Map.of()  // null due to error
                                )
                        ))
                        .graphqlErrors(List.of(
                                Map.of(
                                        "message", "Failed to fetch orders: service timeout",
                                        "path", List.of("orders"),
                                        "extensions", Map.of("code", "DOWNSTREAM_ERROR")
                                )
                        ))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created GraphQL partial error mock");
    }

    /**
     * GraphQL mock with deeply nested response data.
     */
    static void createNestedQueryMock(MockartyClient client) {
        Mock mock = MockBuilder.graphql("query", "organization")
                .id("graphql-nested-org")
                .respond(200, Map.of(
                        "data", Map.of(
                                "organization", Map.of(
                                        "id", "$.fake.UUID",
                                        "name", "$.fake.Company",
                                        "teams", List.of(
                                                Map.of(
                                                        "id", "$.fake.UUID",
                                                        "name", "Backend",
                                                        "members", List.of(
                                                                Map.of("id", "$.fake.UUID", "name", "$.fake.FirstName", "role", "lead"),
                                                                Map.of("id", "$.fake.UUID", "name", "$.fake.FirstName", "role", "developer")
                                                        )
                                                ),
                                                Map.of(
                                                        "id", "$.fake.UUID",
                                                        "name", "Frontend",
                                                        "members", List.of(
                                                                Map.of("id", "$.fake.UUID", "name", "$.fake.FirstName", "role", "lead"),
                                                                Map.of("id", "$.fake.UUID", "name", "$.fake.FirstName", "role", "designer")
                                                        )
                                                )
                                        ),
                                        "totalMembers", 4
                                )
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created nested GraphQL query mock");
    }

    /**
     * GraphQL paginated query mock with cursor-based pagination.
     */
    static void createPaginatedQueryMock(MockartyClient client) {
        Mock mock = MockBuilder.graphql("query", "products")
                .id("graphql-paginated-products")
                .respond(200, Map.of(
                        "data", Map.of(
                                "products", Map.of(
                                        "edges", List.of(
                                                Map.of(
                                                        "cursor", "$.fake.UUID",
                                                        "node", Map.of(
                                                                "id", "$.fake.UUID",
                                                                "name", "$.fake.Word",
                                                                "price", "$.fake.FloatRange(1.00,100.00)"
                                                        )
                                                ),
                                                Map.of(
                                                        "cursor", "$.fake.UUID",
                                                        "node", Map.of(
                                                                "id", "$.fake.UUID",
                                                                "name", "$.fake.Word",
                                                                "price", "$.fake.FloatRange(1.00,100.00)"
                                                        )
                                                )
                                        ),
                                        "pageInfo", Map.of(
                                                "hasNextPage", true,
                                                "hasPreviousPage", false,
                                                "endCursor", "$.fake.UUID"
                                        ),
                                        "totalCount", 50
                                )
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created paginated GraphQL mock");
    }
}
