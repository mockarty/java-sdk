// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.dsl

import ru.mockarty.MockartyClient
import ru.mockarty.model.Contract
import ru.mockarty.model.ContractValidationResult
import ru.mockarty.model.FuzzingConfig
import ru.mockarty.model.FuzzingResult
import ru.mockarty.model.FuzzingRun
import ru.mockarty.model.GeneratorRequest
import ru.mockarty.model.GeneratorResponse
import ru.mockarty.model.ImportResult
import ru.mockarty.model.Mock
import ru.mockarty.model.RecorderSession
import ru.mockarty.model.SaveMockResponse
import ru.mockarty.model.TestRun

/**
 * Extension functions for MockartyClient providing Kotlin DSL support.
 */

/**
 * Creates a mock using the Kotlin DSL and sends it to the server.
 *
 * Usage:
 * ```kotlin
 * client.createMock {
 *     id = "user-service-get"
 *     http {
 *         route = "/api/users/:id"
 *         method = "GET"
 *     }
 *     respond {
 *         statusCode = 200
 *         body = mapOf("name" to "John")
 *     }
 * }
 * ```
 */
fun MockartyClient.createMock(init: MockScope.() -> Unit): SaveMockResponse {
    return mocks().create(mock(init))
}

/**
 * Builds a mock using the DSL without sending it to the server.
 * Useful for testing or inspection.
 */
fun MockartyClient.buildMock(init: MockScope.() -> Unit): Mock {
    return mock(init)
}

/**
 * Creates multiple mocks using the DSL.
 */
fun MockartyClient.createMocks(vararg builders: MockScope.() -> Unit): List<SaveMockResponse> {
    return builders.map { init ->
        mocks().create(mock(init))
    }
}

/**
 * Checks if the Mockarty server is reachable and healthy.
 */
val MockartyClient.isHealthy: Boolean
    get() = health().ready()

/**
 * Gets the Mockarty server version.
 */
val MockartyClient.serverVersion: String
    get() = health().version()

/**
 * Kotlin-friendly shortcut to create an HTTP mock with a response.
 *
 * Usage:
 * ```kotlin
 * client.mockHttp("/api/users", "GET") {
 *     statusCode = 200
 *     body = listOf(mapOf("id" to 1, "name" to "John"))
 * }
 * ```
 */
fun MockartyClient.mockHttp(
    route: String,
    method: String,
    id: String? = null,
    responseInit: ResponseScope.() -> Unit
): SaveMockResponse {
    return createMock {
        id?.let { this.id = it }
        http {
            this.route = route
            this.method = method
        }
        respond(responseInit)
    }
}

/**
 * Kotlin-friendly shortcut to create a gRPC mock with a response.
 */
fun MockartyClient.mockGrpc(
    service: String,
    method: String,
    id: String? = null,
    responseInit: ResponseScope.() -> Unit
): SaveMockResponse {
    return createMock {
        id?.let { this.id = it }
        grpc {
            this.service = service
            this.method = method
        }
        respond(responseInit)
    }
}

/**
 * Kotlin-friendly shortcut to create an MCP mock with a response.
 */
fun MockartyClient.mockMcp(
    tool: String,
    id: String? = null,
    responseInit: ResponseScope.() -> Unit
): SaveMockResponse {
    return createMock {
        id?.let { this.id = it }
        mcp {
            this.tool = tool
        }
        respond(responseInit)
    }
}

// ---- Generator Extensions ----

/**
 * Generates mocks from an OpenAPI specification string.
 */
fun MockartyClient.generateFromOpenAPI(spec: String, namespace: String? = null): GeneratorResponse {
    val req = GeneratorRequest().spec(spec)
    namespace?.let { req.namespace(it) }
    return generator().fromOpenAPI(req)
}

/**
 * Generates mocks from a WSDL specification string.
 */
fun MockartyClient.generateFromWSDL(wsdl: String, namespace: String? = null): GeneratorResponse {
    val req = GeneratorRequest().wsdlContent(wsdl)
    namespace?.let { req.namespace(it) }
    return generator().fromWSDL(req)
}

/**
 * Generates mocks from a Protocol Buffers definition string.
 */
fun MockartyClient.generateFromProto(proto: String, namespace: String? = null): GeneratorResponse {
    val req = GeneratorRequest().protoContent(proto)
    namespace?.let { req.namespace(it) }
    return generator().fromProto(req)
}

/**
 * Generates mocks from a GraphQL schema or URL.
 */
fun MockartyClient.generateFromGraphQL(spec: String, graphqlUrl: String? = null, namespace: String? = null): GeneratorResponse {
    val req = GeneratorRequest().spec(spec)
    graphqlUrl?.let { req.graphqlUrl(it) }
    namespace?.let { req.namespace(it) }
    return generator().fromGraphQL(req)
}

/**
 * Generates mocks from a HAR file content.
 */
fun MockartyClient.generateFromHAR(harContent: String, namespace: String? = null): GeneratorResponse {
    val req = GeneratorRequest().harContent(harContent)
    namespace?.let { req.namespace(it) }
    return generator().fromHAR(req)
}

// ---- Import Extensions ----

/**
 * Imports mocks from a Postman collection JSON.
 */
fun MockartyClient.importPostman(content: String, namespace: String? = null): ImportResult {
    return imports().postman(content, namespace)
}

/**
 * Imports mocks from an OpenAPI specification.
 */
fun MockartyClient.importOpenAPI(content: String, namespace: String? = null): ImportResult {
    return imports().openAPI(content, namespace)
}

// ---- Contract Extensions ----

/**
 * Validates a contract and returns the result.
 */
fun MockartyClient.validateMocks(request: Map<String, Any>): Map<String, Any> {
    return contracts().validateMocks(request)
}

// ---- Fuzzing Extensions ----

/**
 * Starts a fuzzing run and returns the run object.
 */
fun MockartyClient.startFuzzing(configId: String): FuzzingRun {
    return fuzzing().start(configId)
}

/**
 * Gets a fuzzing result by run ID.
 */
fun MockartyClient.getFuzzingResult(runId: String): FuzzingResult {
    return fuzzing().getResult(runId)
}

// ---- Recorder Extensions ----

/**
 * Starts a new recording session.
 */
fun MockartyClient.startRecording(config: Map<String, Any>): RecorderSession {
    return recorder().start(config)
}

// ---- Test Run Extensions ----

/**
 * Gets all test runs.
 */
fun MockartyClient.allTestRuns(): List<TestRun> {
    return testRuns().list()
}

// ---- Stats Extensions ----

/**
 * Gets the current system statistics.
 */
@Suppress("UNCHECKED_CAST")
fun MockartyClient.systemStats(): Map<String, Any> {
    return stats().getStats() as Map<String, Any>
}

/**
 * Gets the current system status.
 */
@Suppress("UNCHECKED_CAST")
fun MockartyClient.systemStatus(): Map<String, Any> {
    return stats().getStatus() as Map<String, Any>
}
