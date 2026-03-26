// Copyright (c) 2024-2026 Mockarty. All rights reserved.
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
 * MCP (Model Context Protocol) mock examples covering tool calls,
 * argument conditions, error responses, and tool chaining.
 */
public class McpMocksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createSimpleToolMock(client);
            createToolWithConditions(client);
            createToolErrorMock(client);
            createDatabaseToolMock(client);
            createFileSystemToolMock(client);
            createChainedToolsMock(client);
        }
    }

    /**
     * Simple MCP tool mock for a search tool.
     */
    static void createSimpleToolMock(MockartyClient client) {
        Mock mock = MockBuilder.mcp("search_web")
                .id("mcp-search-web")
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "Search results for your query:\n1. Result one - description\n2. Result two - description\n3. Result three - description"
                                )
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created simple MCP tool mock");
    }

    /**
     * MCP tool with argument conditions.
     * Matches only when specific arguments are provided.
     */
    static void createToolWithConditions(MockartyClient client) {
        Mock mock = MockBuilder.mcp("get_weather")
                .id("mcp-get-weather")
                .condition("arguments.city", AssertAction.NOT_EMPTY, null)
                .condition("arguments.units", AssertAction.EQUALS, "metric")
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "Weather in $.req.arguments.city: 22C, Sunny, Humidity: 45%"
                                )
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created MCP tool with conditions");
    }

    /**
     * MCP tool returning an error response.
     * The isError flag tells the LLM that the tool call failed.
     */
    static void createToolErrorMock(MockartyClient client) {
        Mock mock = MockBuilder.mcp("execute_query")
                .id("mcp-query-error")
                .condition("arguments.query", AssertAction.CONTAINS, "DROP TABLE")
                .priority(100)
                .respond(new ContentResponse()
                        .statusCode(200)
                        .mcpIsError(true)
                        .payload(Map.of(
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", "Error: Destructive SQL operations are not allowed. Only SELECT queries are permitted."
                                        )
                                )
                        ))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created MCP error tool mock");
    }

    /**
     * MCP tool simulating a database query tool.
     * Returns structured data as JSON in the text content.
     */
    static void createDatabaseToolMock(MockartyClient client) {
        Mock mock = MockBuilder.mcp("execute_query")
                .id("mcp-db-query")
                .condition("arguments.query", AssertAction.CONTAINS, "SELECT")
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "{\"rows\": [{\"id\": 1, \"name\": \"Alice\", \"email\": \"alice@example.com\"}, {\"id\": 2, \"name\": \"Bob\", \"email\": \"bob@example.com\"}], \"rowCount\": 2, \"executionTime\": \"12ms\"}"
                                )
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created MCP database tool mock");
    }

    /**
     * MCP tool simulating file system operations.
     */
    static void createFileSystemToolMock(MockartyClient client) {
        // Read file tool
        Mock readFile = MockBuilder.mcp("read_file")
                .id("mcp-read-file")
                .condition("arguments.path", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "# Configuration File\n\nserver:\n  port: 8080\n  host: 0.0.0.0\n\ndatabase:\n  url: postgresql://localhost:5432/mydb\n  pool_size: 10"
                                )
                        )
                ))
                .build();

        // Write file tool
        Mock writeFile = MockBuilder.mcp("write_file")
                .id("mcp-write-file")
                .condition("arguments.path", AssertAction.NOT_EMPTY, null)
                .condition("arguments.content", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "File written successfully to $.req.arguments.path"
                                )
                        )
                ))
                .build();

        // List directory tool
        Mock listDir = MockBuilder.mcp("list_directory")
                .id("mcp-list-dir")
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "Directory listing:\n- src/\n- tests/\n- README.md\n- config.yaml\n- Makefile"
                                )
                        )
                ))
                .build();

        client.mocks().create(readFile);
        client.mocks().create(writeFile);
        client.mocks().create(listDir);
        System.out.println("Created MCP file system tool mocks");
    }

    /**
     * Chain of related MCP tools that share state.
     * Demonstrates a workflow: create project -> add files -> build.
     */
    static void createChainedToolsMock(MockartyClient client) {
        String chainId = "project-workflow";

        Mock createProject = MockBuilder.mcp("create_project")
                .id("mcp-chain-create-project")
                .chainId(chainId)
                .condition("arguments.name", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "Project '$.req.arguments.name' created successfully. Project ID: $.fake.UUID"
                                )
                        )
                ))
                .build();

        Mock addDependency = MockBuilder.mcp("add_dependency")
                .id("mcp-chain-add-dep")
                .chainId(chainId)
                .condition("arguments.package", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "Added dependency '$.req.arguments.package' to project. Resolving versions..."
                                )
                        )
                ))
                .build();

        Mock buildProject = MockBuilder.mcp("build_project")
                .id("mcp-chain-build")
                .chainId(chainId)
                .respond(200, Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "Build completed successfully.\n- Compiled: 42 files\n- Tests: 15 passed, 0 failed\n- Artifact: build/output.jar (2.3 MB)"
                                )
                        )
                ))
                .build();

        client.mocks().create(createProject);
        client.mocks().create(addDependency);
        client.mocks().create(buildProject);
        System.out.println("Created chained MCP tool mocks");
    }
}
