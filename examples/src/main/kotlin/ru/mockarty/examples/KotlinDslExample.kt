// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples

import ru.mockarty.MockartyClient
import ru.mockarty.dsl.*
import ru.mockarty.model.AssertAction
import ru.mockarty.model.Environment
import ru.mockarty.model.FuzzingSchedule
import ru.mockarty.model.MockFolder
import ru.mockarty.model.Pact

/**
 * Kotlin DSL examples demonstrating the idiomatic Kotlin API for Mockarty.
 *
 * The Kotlin DSL provides:
 * - `mock {}` builder for constructing Mock objects
 * - Protocol-specific DSL blocks: `http {}`, `grpc {}`, `graphql {}`, etc.
 * - Extension functions on MockartyClient for concise operations
 * - Type-safe condition and response builders
 *
 * This example also covers the new APIs: agent tasks, pacts, environments,
 * tags, folders, undefined requests, stats, namespace settings, and proxy.
 */
fun main() {
    val client = MockartyClient.builder()
        .baseUrl("http://localhost:5770")
        .apiKey("your-api-key")
        .namespace("sandbox")
        .build()

    client.use {
        basicMockDsl(it)
        httpMockDsl(it)
        grpcMockDsl(it)
        graphqlMockDsl(it)
        soapMockDsl(it)
        mcpMockDsl(it)
        sseMockDsl(it)
        kafkaMockDsl(it)
        rabbitmqMockDsl(it)
        smtpMockDsl(it)
        advancedPatterns(it)
        extensionFunctions(it)

        // New APIs
        agentTasksExample(it)
        pactContractExample(it)
        environmentsExample(it)
        tagsAndFoldersExample(it)
        undefinedRequestsExample(it)
        statsAndMonitoring(it)
        namespaceSettingsExample(it)
        proxyApiExample(it)
    }
}

// ---- Basic Mock DSL ----

/**
 * Basic mock creation using the `mock {}` DSL.
 */
fun basicMockDsl(client: MockartyClient) {
    // Simple mock using the top-level `mock {}` function
    val simpleMock = mock {
        id = "kt-basic-hello"
        namespace = "sandbox"

        http {
            route = "/api/hello"
            method = "GET"
        }

        respond {
            statusCode = 200
            body = mapOf("message" to "Hello from Kotlin DSL!")
        }
    }

    client.mocks().create(simpleMock)
    println("Created basic Kotlin DSL mock")

    // Inline respond shorthand
    val shorthand = mock {
        id = "kt-basic-shorthand"
        http {
            route = "/api/ping"
            method = "GET"
        }
        respond(200, mapOf("pong" to true))
    }

    client.mocks().create(shorthand)
    println("Created shorthand mock")
}

// ---- HTTP Mock DSL ----

/**
 * HTTP mocks with conditions, headers, query params, and various response types.
 */
fun httpMockDsl(client: MockartyClient) {
    // GET with query and header conditions
    val getUserMock = mock {
        id = "kt-http-get-user"
        tags = listOf("users", "v2", "kotlin")
        priority = 10

        http {
            route = "/api/users/:id"
            method = "GET"
            headerCondition("Authorization", AssertAction.NOT_EMPTY)
            queryCondition("fields", AssertAction.CONTAINS, "email")
        }

        respond {
            statusCode = 200
            body = mapOf(
                "id" to "$.pathParam.id",
                "name" to "$.fake.FirstName",
                "email" to "$.fake.Email",
                "avatar" to "$.fake.URL"
            )
            headers = mapOf("X-Request-ID" to listOf("$.fake.UUID"))
        }
    }

    client.mocks().create(getUserMock)

    // POST with body conditions
    val createUserMock = mock {
        id = "kt-http-create-user"

        http {
            route = "/api/users"
            method = "POST"
            condition("email", AssertAction.MATCHES, "^[\\w.-]+@[\\w.-]+\\.\\w+\$")
            condition("name", AssertAction.NOT_EMPTY)
        }

        respond {
            statusCode = 201
            body = mapOf(
                "id" to "$.fake.UUID",
                "name" to "$.req.name",
                "email" to "$.req.email",
                "createdAt" to "$.fake.DateISO"
            )
        }

        ttl = 3600
    }

    client.mocks().create(createUserMock)

    // OneOf ordered responses
    val orderStatusMock = mock {
        id = "kt-http-order-status"

        http {
            route = "/api/orders/:id/status"
            method = "GET"
        }

        oneOfOrdered {
            respond { statusCode = 200; body = mapOf("status" to "pending") }
            respond { statusCode = 200; body = mapOf("status" to "processing") }
            respond { statusCode = 200; body = mapOf("status" to "shipped") }
            respond { statusCode = 200; body = mapOf("status" to "delivered") }
        }
    }

    client.mocks().create(orderStatusMock)

    // OneOf random (flaky service simulation)
    val flakyMock = mock {
        id = "kt-http-flaky-service"

        http {
            route = "/api/external/health"
            method = "GET"
        }

        oneOfRandom {
            respond(200, mapOf("status" to "healthy"))
            respond(200, mapOf("status" to "healthy"))
            respond(503, mapOf("error" to "Service unavailable"))
        }
    }

    client.mocks().create(flakyMock)

    // Mock with delay
    val slowMock = mock {
        id = "kt-http-slow"

        http {
            route = "/api/reports/heavy"
            method = "GET"
        }

        respond {
            statusCode = 200
            body = mapOf("reportId" to "$.fake.UUID", "rows" to 50000)
            delay = 3000
        }
    }

    client.mocks().create(slowMock)

    println("Created HTTP Kotlin DSL mocks")
}

// ---- gRPC Mock DSL ----

/**
 * gRPC mocks using the Kotlin DSL.
 */
fun grpcMockDsl(client: MockartyClient) {
    val grpcMock = mock {
        id = "kt-grpc-get-user"
        tags = listOf("grpc", "users")

        grpc {
            service = "user.UserService"
            method = "GetUser"
            condition("userId", AssertAction.NOT_EMPTY)
            metaCondition("authorization", AssertAction.MATCHES, "^Bearer .+$")
        }

        respond {
            statusCode = 200
            body = mapOf(
                "userId" to "$.req.userId",
                "name" to "$.fake.FirstName",
                "email" to "$.fake.Email",
                "role" to "ADMIN"
            )
        }
    }

    client.mocks().create(grpcMock)

    // gRPC error response
    val grpcError = mock {
        id = "kt-grpc-not-found"

        grpc {
            service = "user.UserService"
            method = "GetUser"
            condition("userId", AssertAction.EQUALS, "nonexistent")
        }

        respond {
            statusCode = 404
            error = "User not found"
        }

        priority = 100
    }

    client.mocks().create(grpcError)
    println("Created gRPC Kotlin DSL mocks")
}

// ---- GraphQL Mock DSL ----

/**
 * GraphQL mocks using the Kotlin DSL.
 */
fun graphqlMockDsl(client: MockartyClient) {
    // Query mock
    val queryMock = mock {
        id = "kt-graphql-users"

        graphql {
            operation = "query"
            field = "users"
            condition("variables.limit", AssertAction.NOT_EMPTY)
        }

        respond {
            statusCode = 200
            body = mapOf(
                "data" to mapOf(
                    "users" to listOf(
                        mapOf("id" to "$.fake.UUID", "name" to "$.fake.FirstName"),
                        mapOf("id" to "$.fake.UUID", "name" to "$.fake.FirstName")
                    )
                )
            )
        }
    }

    client.mocks().create(queryMock)

    // Mutation mock
    val mutationMock = mock {
        id = "kt-graphql-create-post"

        graphql {
            operation = "mutation"
            field = "createPost"
            condition("variables.input.title", AssertAction.NOT_EMPTY)
        }

        respond {
            statusCode = 200
            body = mapOf(
                "data" to mapOf(
                    "createPost" to mapOf(
                        "id" to "$.fake.UUID",
                        "title" to "$.req.variables.input.title",
                        "publishedAt" to "$.fake.DateISO"
                    )
                )
            )
        }
    }

    client.mocks().create(mutationMock)
    println("Created GraphQL Kotlin DSL mocks")
}

// ---- SOAP Mock DSL ----

/**
 * SOAP mocks using the Kotlin DSL.
 */
fun soapMockDsl(client: MockartyClient) {
    val soapMock = mock {
        id = "kt-soap-weather"

        soap {
            service = "WeatherService"
            method = "GetForecast"
            condition("City", AssertAction.NOT_EMPTY)
            headerCondition("SOAPAction", AssertAction.CONTAINS, "GetForecast")
        }

        respond {
            statusCode = 200
            body = mapOf(
                "GetForecastResponse" to mapOf(
                    "City" to "$.req.City",
                    "Temperature" to "$.fake.IntRange(0,40)",
                    "Condition" to "Partly Cloudy"
                )
            )
        }
    }

    client.mocks().create(soapMock)
    println("Created SOAP Kotlin DSL mock")
}

// ---- MCP Mock DSL ----

/**
 * MCP (Model Context Protocol) mocks using the Kotlin DSL.
 */
fun mcpMockDsl(client: MockartyClient) {
    val mcpMock = mock {
        id = "kt-mcp-search"

        mcp {
            tool = "search_code"
            condition("arguments.query", AssertAction.NOT_EMPTY)
            condition("arguments.language", AssertAction.EQUALS, "kotlin")
        }

        respond {
            statusCode = 200
            body = mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to "Found 3 matching Kotlin files:\n1. Main.kt\n2. Service.kt\n3. Repository.kt"
                    )
                )
            )
        }
    }

    client.mocks().create(mcpMock)
    println("Created MCP Kotlin DSL mock")
}

// ---- SSE Mock DSL ----

/**
 * SSE (Server-Sent Events) mocks using the Kotlin DSL.
 */
fun sseMockDsl(client: MockartyClient) {
    val sseMock = mock {
        id = "kt-sse-notifications"

        sse {
            eventPath = "/events/notifications"
            eventName = "notification"
            headerCondition("Authorization", AssertAction.NOT_EMPTY)
        }

        respond {
            statusCode = 200
            body = mapOf(
                "id" to "$.fake.UUID",
                "type" to "info",
                "message" to "New notification",
                "timestamp" to "$.fake.DateISO"
            )
        }
    }

    client.mocks().create(sseMock)
    println("Created SSE Kotlin DSL mock")
}

// ---- Kafka Mock DSL ----

/**
 * Kafka mocks using the Kotlin DSL.
 */
fun kafkaMockDsl(client: MockartyClient) {
    val kafkaMock = mock {
        id = "kt-kafka-orders"

        kafka {
            topic = "orders"
            serverName = "order-processor"
            condition("orderId", AssertAction.NOT_EMPTY)
            headerCondition("X-Source", AssertAction.EQUALS, "web")
        }

        respond {
            statusCode = 200
            body = mapOf(
                "status" to "processed",
                "orderId" to "$.req.orderId",
                "processedAt" to "$.fake.DateISO"
            )
        }
    }

    client.mocks().create(kafkaMock)
    println("Created Kafka Kotlin DSL mock")
}

// ---- RabbitMQ Mock DSL ----

/**
 * RabbitMQ mocks using the Kotlin DSL.
 */
fun rabbitmqMockDsl(client: MockartyClient) {
    val rabbitMock = mock {
        id = "kt-rabbitmq-notifications"

        rabbitmq {
            queue = "notifications"
            serverName = "notification-service"
            exchange = "events"
            routingKey = "user.notification"
            condition("type", AssertAction.EQUALS, "email")
        }

        respond {
            statusCode = 200
            body = mapOf(
                "notificationId" to "$.fake.UUID",
                "status" to "delivered",
                "channel" to "email"
            )
        }
    }

    client.mocks().create(rabbitMock)
    println("Created RabbitMQ Kotlin DSL mock")
}

// ---- SMTP Mock DSL ----

/**
 * SMTP mocks using the Kotlin DSL.
 */
fun smtpMockDsl(client: MockartyClient) {
    val smtpMock = mock {
        id = "kt-smtp-welcome"

        smtp {
            serverName = "mail-server"
            subjectCondition("subject", AssertAction.CONTAINS, "Welcome")
            senderCondition("sender", AssertAction.MATCHES, ".*@company\\.com")
        }

        respond {
            statusCode = 200
            body = mapOf(
                "messageId" to "$.fake.UUID",
                "status" to "delivered"
            )
        }
    }

    client.mocks().create(smtpMock)
    println("Created SMTP Kotlin DSL mock")
}

// ---- Advanced Patterns ----

/**
 * Advanced Kotlin DSL patterns: callbacks, extract, proxy, chain workflows.
 */
fun advancedPatterns(client: MockartyClient) {
    // Mock with HTTP callback
    val callbackMock = mock {
        id = "kt-advanced-callback"

        http {
            route = "/api/orders"
            method = "POST"
        }

        respond {
            statusCode = 201
            body = mapOf("orderId" to "$.fake.UUID", "status" to "created")
        }

        callback("https://webhook.example.com/order-created", "POST",
            mapOf("event" to "order.created", "timestamp" to "$.fake.DateISO"))
    }

    client.mocks().create(callbackMock)

    // Mock with Kafka callback DSL
    val kafkaCallbackMock = mock {
        id = "kt-advanced-kafka-callback"

        http {
            route = "/api/payments"
            method = "POST"
        }

        respond {
            statusCode = 200
            body = mapOf("paymentId" to "$.fake.UUID")
        }

        callback {
            type = "kafka"
            kafkaBrokers = "localhost:9092"
            kafkaTopic = "payment-events"
            body = mapOf("event" to "payment.processed")
            async = true
        }
    }

    client.mocks().create(kafkaCallbackMock)

    // Mock with store extraction
    val extractMock = mock {
        id = "kt-advanced-extract"
        chainId = "kt-order-flow"

        http {
            route = "/api/orders"
            method = "POST"
            condition("items", AssertAction.NOT_EMPTY)
        }

        extract {
            cStore = mapOf(
                "orderId" to "$.fake.UUID",
                "userId" to "$.req.userId",
                "status" to "created"
            )
            gStore = mapOf(
                "lastOrderTimestamp" to "$.fake.DateISO"
            )
        }

        respond {
            statusCode = 201
            body = mapOf(
                "orderId" to "$.cS.orderId",
                "status" to "$.cS.status"
            )
        }
    }

    client.mocks().create(extractMock)

    // Mock with proxy
    val proxyMock = mock {
        id = "kt-advanced-proxy"

        http {
            route = "/api/proxy-target/:path"
            method = "GET"
        }

        proxyTo("https://api.real-service.com")
    }

    client.mocks().create(proxyMock)

    // Mock with mock store (ephemeral per-request data)
    val mockStoreMock = mock {
        id = "kt-advanced-mock-store"
        mockStore = mapOf("taxRate" to 0.21, "currency" to "EUR")

        http {
            route = "/api/pricing"
            method = "GET"
        }

        respond {
            statusCode = 200
            body = mapOf(
                "taxRate" to "$.mS.taxRate",
                "currency" to "$.mS.currency"
            )
        }
    }

    client.mocks().create(mockStoreMock)

    // Mock with use limiter and TTL
    val limitedMock = mock {
        id = "kt-advanced-limited"
        ttl = 1800
        useLimiter = 10
        tags = listOf("limited", "kotlin")
        priority = 50

        http {
            route = "/api/limited-offer"
            method = "GET"
        }

        respond(200, mapOf("offer" to "50% off", "remaining" to "limited"))
    }

    client.mocks().create(limitedMock)

    println("Created advanced Kotlin DSL mocks")
}

// ---- Extension Functions ----

/**
 * Kotlin extension functions for concise client operations.
 */
fun extensionFunctions(client: MockartyClient) {
    // Create a mock directly with the extension function
    client.createMock {
        id = "kt-ext-quick-mock"
        http {
            route = "/api/quick"
            method = "GET"
        }
        respond(200, mapOf("quick" to true))
    }

    // Shortcut for HTTP mock
    client.mockHttp("/api/shortcut", "GET", "kt-ext-shortcut") {
        statusCode = 200
        body = mapOf("shortcut" to true)
    }

    // Shortcut for gRPC mock
    client.mockGrpc("test.Service", "Method", "kt-ext-grpc") {
        statusCode = 200
        body = mapOf("grpcResult" to "ok")
    }

    // Shortcut for MCP mock
    client.mockMcp("test_tool", "kt-ext-mcp") {
        statusCode = 200
        body = mapOf("content" to listOf(mapOf("type" to "text", "text" to "Tool result")))
    }

    // Create multiple mocks at once
    client.createMocks(
        {
            id = "kt-ext-batch-1"
            http { route = "/api/batch/1"; method = "GET" }
            respond(200, mapOf("batch" to 1))
        },
        {
            id = "kt-ext-batch-2"
            http { route = "/api/batch/2"; method = "GET" }
            respond(200, mapOf("batch" to 2))
        },
        {
            id = "kt-ext-batch-3"
            http { route = "/api/batch/3"; method = "GET" }
            respond(200, mapOf("batch" to 3))
        }
    )

    // Build mock without sending (for inspection/testing)
    val inspectMock = client.buildMock {
        id = "kt-ext-inspect"
        http {
            route = "/api/inspect"
            method = "POST"
        }
        respond(200)
    }
    println("Built (not sent) mock: ${inspectMock.id}, protocol: ${inspectMock.protocol()}")

    // Health check extensions
    println("Server healthy: ${client.isHealthy}")
    println("Server version: ${client.serverVersion}")

    println("Extension function examples completed")
}

// ---- New APIs: Agent Tasks ----

/**
 * AI agent task management using the Kotlin API.
 */
fun agentTasksExample(client: MockartyClient) {
    println("\n=== Kotlin: Agent Tasks ===")

    // Submit an AI agent task
    val task = client.agentTasks().submit(mapOf(
        "type" to "generate_mocks",
        "prompt" to "Create REST API mocks for a bookstore with books, authors, and reviews",
        "namespace" to "sandbox",
        "options" to mapOf(
            "includeConditions" to true,
            "responseFormat" to "json"
        )
    ))
    println("Submitted agent task: ${task.id} (status: ${task.status})")

    // List all tasks
    val tasks = client.agentTasks().list()
    println("Total agent tasks: ${tasks.size}")
    tasks.forEach { t ->
        println("  ${t.id}: ${t.type} - ${t.status}")
    }

    // Get task details and check completion
    val detail = client.agentTasks().get(task.id)
    println("Task detail: ${detail.status}")

    // Rerun a completed task
    if (detail.status == "completed") {
        val rerunned = client.agentTasks().rerun(task.id)
        println("Re-ran task -> new task: ${rerunned.id}")
    }

    // Export task result
    try {
        val exported = client.agentTasks().export(task.id)
        println("Exported task: ${exported.size} bytes")
    } catch (e: Exception) {
        println("Export not available: ${e.message}")
    }
}

// ---- New APIs: Pact Contract Testing ----

/**
 * Pact-based consumer-driven contract testing using the Kotlin API.
 */
fun pactContractExample(client: MockartyClient) {
    println("\n=== Kotlin: Pact Contracts ===")

    // Publish a consumer pact
    val pact = Pact()
        .consumer("kotlin-order-service")
        .provider("kotlin-user-service")
        .version("1.0.0")
        .interactions(listOf(
            mapOf(
                "description" to "get user for order",
                "request" to mapOf("method" to "GET", "path" to "/api/users/u-1"),
                "response" to mapOf(
                    "status" to 200,
                    "body" to mapOf("id" to "u-1", "name" to "Alice", "email" to "alice@kt.dev")
                )
            )
        ))

    val published = client.contracts().publishPact(pact)
    println("Published pact: ${published.id}")
    println("  ${published.consumer} -> ${published.provider}")

    // List pacts
    val pacts = client.contracts().listPacts()
    println("Total pacts: ${pacts.size}")
    pacts.forEach { p ->
        println("  ${p.consumer} -> ${p.provider} (v${p.version})")
    }

    // Verify pact
    val verification = client.contracts().verifyPact(mapOf(
        "pactId" to published.id,
        "providerUrl" to "http://localhost:5770",
        "providerVersion" to "1.0.0"
    ))
    println("Verification: ${if (verification.isSuccess) "PASSED" else "FAILED"}")

    // Can I deploy?
    val deployCheck = client.contracts().canIDeploy(mapOf(
        "application" to "kotlin-order-service",
        "version" to "1.0.0",
        "to" to "production"
    ))
    println("Can I deploy? ${deployCheck.isDeployable} (${deployCheck.reason})")

    // Generate mocks from pact
    val generatedMocks = client.contracts().generateMocksFromPact(published.id)
    println("Generated ${generatedMocks.size} mocks from pact")

    // List verifications
    val verifications = client.contracts().listVerifications()
    println("Total verifications: ${verifications.size}")
}

// ---- New APIs: Environments ----

/**
 * API Tester environment management using the Kotlin API.
 */
fun environmentsExample(client: MockartyClient) {
    println("\n=== Kotlin: Environments ===")

    // Create environments
    val dev = client.environments().create(
        Environment()
            .name("Kotlin Dev")
            .variables(mapOf(
                "baseUrl" to "http://localhost:8080",
                "apiKey" to "dev-key-kt",
                "timeout" to "30000"
            ))
    )
    println("Created environment: ${dev.name} (${dev.id})")

    val staging = client.environments().create(
        Environment()
            .name("Kotlin Staging")
            .variables(mapOf(
                "baseUrl" to "https://staging.example.com",
                "apiKey" to "staging-key-kt"
            ))
    )
    println("Created environment: ${staging.name}")

    // List environments
    val envs = client.environments().list()
    println("Environments: ${envs.size}")
    envs.forEach { e -> println("  ${e.name}: ${e.variables?.get("baseUrl")}") }

    // Activate an environment
    client.environments().activate(dev.id)
    println("Activated: ${dev.name}")

    // Get active environment
    val active = client.environments().getActive()
    println("Active environment: ${active.name}")

    // Update environment
    val updated = client.environments().update(dev.id,
        Environment()
            .name("Kotlin Dev (updated)")
            .variables(mapOf(
                "baseUrl" to "http://localhost:9090",
                "apiKey" to "updated-key",
                "debugMode" to "true"
            ))
    )
    println("Updated: ${updated.name}")
}

// ---- New APIs: Tags and Folders ----

/**
 * Tag and folder management using the Kotlin API.
 */
fun tagsAndFoldersExample(client: MockartyClient) {
    println("\n=== Kotlin: Tags & Folders ===")

    // Create tags
    val tag1 = client.tags().create("kotlin-tag")
    val tag2 = client.tags().create("integration")
    println("Created tags: ${tag1.name}, ${tag2.name}")

    // List all tags
    val tags = client.tags().list()
    println("Total tags: ${tags.size}")
    tags.forEach { t -> println("  - ${t.name}") }

    // Create folders
    val rootFolder = client.folders().create(
        MockFolder().name("Kotlin Services").description("Kotlin-based service mocks")
    )
    println("Created folder: ${rootFolder.name}")

    val subFolder = client.folders().create(
        MockFolder()
            .name("Auth Service")
            .description("Authentication mocks")
            .parentId(rootFolder.id)
    )
    println("Created sub-folder: ${subFolder.name}")

    // List folders
    val folders = client.folders().list()
    println("Folders: ${folders.size}")
    folders.forEach { f ->
        val indent = if (f.parentId != null) "    " else "  "
        println("$indent- ${f.name}")
    }

    // Create mocks and apply batch tags
    val mockIds = (1..3).map { i ->
        val m = mock {
            id = "kt-batch-tag-$i"
            http { route = "/api/kt-batch/$i"; method = "GET" }
            respond(200, mapOf("item" to i))
        }
        client.mocks().create(m)
        "kt-batch-tag-$i"
    }
    println("Created ${mockIds.size} mocks")

    // Batch update tags
    client.mocks().batchUpdateTags(mockIds, listOf("kotlin-tag", "integration", "v2"))
    println("Applied tags to ${mockIds.size} mocks")

    // Move mocks to folder
    client.mocks().moveToFolder(mockIds, rootFolder.id)
    println("Moved mocks to folder: ${rootFolder.name}")

    // Cleanup
    mockIds.forEach { client.mocks().delete(it) }
    println("Cleaned up batch mocks")
}

// ---- New APIs: Undefined Requests ----

/**
 * Undefined request management using the Kotlin API.
 */
fun undefinedRequestsExample(client: MockartyClient) {
    println("\n=== Kotlin: Undefined Requests ===")

    // List undefined requests
    val requests = client.undefined().list()
    println("Undefined requests: ${requests.size}")

    requests.take(5).forEach { req ->
        println("  ${req.method} ${req.path} (${req.count}x)")
    }

    // Create mocks from undefined requests
    requests.filter { !it.path.contains("/health") }
        .take(3)
        .forEach { req ->
            try {
                val mock = client.undefined().createMock(req.id)
                println("Created mock from undefined: ${mock.id}")
            } catch (e: Exception) {
                println("Skipped: ${req.path} - ${e.message}")
            }
        }

    // Ignore health-check undefined requests
    requests.filter { it.path.contains("/health") }
        .forEach { req ->
            client.undefined().ignore(req.id)
            println("Ignored: ${req.path}")
        }
}

// ---- New APIs: Stats and Monitoring ----

/**
 * System statistics and monitoring using the Kotlin API.
 */
fun statsAndMonitoring(client: MockartyClient) {
    println("\n=== Kotlin: Stats & Monitoring ===")

    // System stats
    val stats = client.stats().getStats()
    println("Stats: totalRequests=${stats["totalRequests"]}, " +
        "matched=${stats["matchedRequests"]}")

    // Resource counts
    val counts = client.stats().getCounts()
    println("Counts: mocks=${counts["mocks"]}, " +
        "namespaces=${counts["namespaces"]}")

    // System status
    val status = client.stats().getStatus()
    println("Status: ${status["status"]}, version=${status["version"]}")

    // Feature detection
    val features = client.stats().getFeatures()
    println("Features:")
    features.forEach { (key, value) -> println("  $key: $value") }

    // Conditional logic based on features
    if (features["fuzzing"] == true) {
        println("Fuzzing available - can run security scans")
    }
    if (features["aiAgent"] == true) {
        println("AI Agent available - can submit agent tasks")
    }
}

// ---- New APIs: Namespace Settings ----

/**
 * Namespace settings (users, cleanup, webhooks) using the Kotlin API.
 */
fun namespaceSettingsExample(client: MockartyClient) {
    println("\n=== Kotlin: Namespace Settings ===")

    val ns = "sandbox"

    // List users
    val users = client.namespaceSettings().listUsers(ns)
    println("Users in '$ns': ${users.size}")
    users.forEach { u -> println("  ${u.userId}: ${u.role}") }

    // Add a user
    client.namespaceSettings().addUser(ns, mapOf(
        "userId" to "kt-user-1",
        "role" to "editor"
    ))
    println("Added user 'kt-user-1' as editor")

    // Update user role
    client.namespaceSettings().updateUserRole(ns, "kt-user-1", "admin")
    println("Updated 'kt-user-1' to admin")

    // Get cleanup policy
    val policy = client.namespaceSettings().getCleanupPolicy(ns)
    println("Cleanup policy: enabled=${policy.isEnabled}, " +
        "mockTTL=${policy.mockTtlDays}d, logRetention=${policy.logRetentionDays}d")

    // List webhooks
    val webhooks = client.namespaceSettings().listWebhooks(ns)
    println("Webhooks: ${webhooks.size}")
    webhooks.forEach { wh -> println("  ${wh["name"]} -> ${wh["url"]}") }

    // Create a webhook
    val webhook = client.namespaceSettings().createWebhook(ns, mapOf(
        "name" to "Kotlin CI Notifications",
        "url" to "https://hooks.slack.com/kotlin-channel",
        "events" to listOf("mock.created", "testrun.completed"),
        "enabled" to true
    ))
    println("Created webhook: ${webhook["id"]}")

    // Remove user
    client.namespaceSettings().removeUser(ns, "kt-user-1")
    println("Removed user 'kt-user-1'")
}

// ---- New APIs: Proxy API ----

/**
 * Proxy API for HTTP, SOAP, and gRPC proxying using the Kotlin API.
 */
fun proxyApiExample(client: MockartyClient) {
    println("\n=== Kotlin: Proxy API ===")

    // HTTP proxy
    val httpResponse = client.proxy().http(mapOf(
        "url" to "https://httpbin.org/get",
        "method" to "GET",
        "headers" to mapOf("Accept" to "application/json"),
        "timeout" to 5000
    ))
    println("HTTP proxy: status=${httpResponse["statusCode"]}")

    // SOAP proxy
    val soapResponse = client.proxy().soap(mapOf(
        "url" to "http://soap-service.internal/ws",
        "soapAction" to "http://example.com/GetData",
        "body" to "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><GetData/></soap:Body></soap:Envelope>",
        "timeout" to 10000
    ))
    println("SOAP proxy: status=${soapResponse["statusCode"]}")

    // gRPC proxy
    val grpcResponse = client.proxy().grpc(mapOf(
        "target" to "grpc://service.internal:50051",
        "service" to "example.Service",
        "method" to "GetItem",
        "payload" to mapOf("itemId" to "item-1"),
        "timeout" to 5000
    ))
    println("gRPC proxy: status=${grpcResponse["statusCode"]}")
}
