// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.dsl

import ru.mockarty.model.*

/**
 * DSL marker annotation for Mockarty DSL scopes.
 * Prevents implicit access to outer scope receivers.
 */
@DslMarker
annotation class MockartyDslMarker

/**
 * Top-level DSL function to create a Mock.
 *
 * Usage:
 * ```kotlin
 * val myMock = mock {
 *     id = "user-service-get"
 *     namespace = "production"
 *     tags = listOf("users", "v2")
 *
 *     http {
 *         route = "/api/users/:id"
 *         method = "GET"
 *         headerCondition("Authorization", AssertAction.NOT_EMPTY)
 *     }
 *
 *     respond {
 *         statusCode = 200
 *         body = mapOf("id" to "$.pathParam.id", "name" to "$.fake.FirstName")
 *     }
 *
 *     ttl = 3600
 * }
 * ```
 */
fun mock(init: MockScope.() -> Unit): Mock {
    return MockScope().apply(init).build()
}

/**
 * Top-level scope for building a Mock.
 */
@MockartyDslMarker
class MockScope {
    var id: String? = null
    var namespace: String? = null
    var chainId: String? = null
    var tags: List<String> = emptyList()
    var ttl: Long? = null
    var priority: Long? = null
    var pathPrefix: String? = null
    var serverName: String? = null
    var folderId: String? = null
    var useLimiter: Int? = null
    var mockStore: Map<String, Any>? = null

    private var _http: HttpRequestContext? = null
    private var _grpc: GrpcRequestContext? = null
    private var _mcp: MCPRequestContext? = null
    private var _soap: SoapRequestContext? = null
    private var _graphql: GraphQLRequestContext? = null
    private var _sse: SSERequestContext? = null
    private var _kafka: KafkaRequestContext? = null
    private var _rabbitmq: RabbitMQRequestContext? = null
    private var _smtp: SmtpRequestContext? = null
    private var _response: ContentResponse? = null
    private var _oneOf: OneOf? = null
    private var _proxy: Proxy? = null
    private var _callbacks: MutableList<Callback> = mutableListOf()
    private var _extract: Extract? = null

    /**
     * Configures an HTTP request context.
     */
    fun http(init: HttpScope.() -> Unit) {
        _http = HttpScope().apply(init).build()
    }

    /**
     * Configures a gRPC request context.
     */
    fun grpc(init: GrpcScope.() -> Unit) {
        _grpc = GrpcScope().apply(init).build()
    }

    /**
     * Configures an MCP request context.
     */
    fun mcp(init: McpScope.() -> Unit) {
        _mcp = McpScope().apply(init).build()
    }

    /**
     * Configures a SOAP request context.
     */
    fun soap(init: SoapScope.() -> Unit) {
        _soap = SoapScope().apply(init).build()
    }

    /**
     * Configures a GraphQL request context.
     */
    fun graphql(init: GraphqlScope.() -> Unit) {
        _graphql = GraphqlScope().apply(init).build()
    }

    /**
     * Configures an SSE request context.
     */
    fun sse(init: SseScope.() -> Unit) {
        _sse = SseScope().apply(init).build()
    }

    /**
     * Configures a Kafka request context.
     */
    fun kafka(init: KafkaScope.() -> Unit) {
        _kafka = KafkaScope().apply(init).build()
    }

    /**
     * Configures a RabbitMQ request context.
     */
    fun rabbitmq(init: RabbitmqScope.() -> Unit) {
        _rabbitmq = RabbitmqScope().apply(init).build()
    }

    /**
     * Configures an SMTP request context.
     */
    fun smtp(init: SmtpScope.() -> Unit) {
        _smtp = SmtpScope().apply(init).build()
    }

    /**
     * Configures the response.
     */
    fun respond(init: ResponseScope.() -> Unit) {
        _response = ResponseScope().apply(init).build()
    }

    /**
     * Configures a simple response with status code and body.
     */
    fun respond(statusCode: Int, body: Any? = null) {
        _response = ContentResponse().statusCode(statusCode).also {
            if (body != null) it.payload(body)
        }
    }

    /**
     * Configures a proxy to a target URL.
     */
    fun proxyTo(target: String) {
        _proxy = Proxy(target)
    }

    /**
     * Adds an HTTP callback.
     */
    fun callback(url: String, method: String = "POST", body: Any? = null) {
        _callbacks.add(Callback.http(url, method, body))
    }

    /**
     * Adds a custom callback.
     */
    fun callback(init: CallbackScope.() -> Unit) {
        _callbacks.add(CallbackScope().apply(init).build())
    }

    /**
     * Configures ordered OneOf responses.
     */
    fun oneOfOrdered(init: OneOfScope.() -> Unit) {
        val scope = OneOfScope().apply(init)
        _oneOf = OneOf.ordered(*scope.responses.toTypedArray())
    }

    /**
     * Configures random OneOf responses.
     */
    fun oneOfRandom(init: OneOfScope.() -> Unit) {
        val scope = OneOfScope().apply(init)
        _oneOf = OneOf.random(*scope.responses.toTypedArray())
    }

    /**
     * Configures data extraction rules.
     */
    fun extract(init: ExtractScope.() -> Unit) {
        _extract = ExtractScope().apply(init).build()
    }

    internal fun build(): Mock {
        val mock = Mock()
        id?.let { mock.id(it) }
        namespace?.let { mock.namespace(it) }
        chainId?.let { mock.chainId(it) }
        if (tags.isNotEmpty()) mock.tags(tags)
        ttl?.let { mock.ttl(it) }
        priority?.let { mock.priority(it) }
        pathPrefix?.let { mock.pathPrefix(it) }
        serverName?.let { mock.serverName(it) }
        folderId?.let { mock.folderId(it) }
        useLimiter?.let { mock.useLimiter(it) }
        mockStore?.let { mock.mockStore(it) }

        _http?.let { mock.http(it) }
        _grpc?.let { mock.grpc(it) }
        _mcp?.let { mock.mcp(it) }
        _soap?.let { mock.soap(it) }
        _graphql?.let { mock.graphql(it) }
        _sse?.let { mock.sse(it) }
        _kafka?.let { mock.kafka(it) }
        _rabbitmq?.let { mock.rabbitmq(it) }
        _smtp?.let { mock.smtp(it) }
        _response?.let { mock.response(it) }
        _oneOf?.let { mock.oneOf(it) }
        _proxy?.let { mock.proxy(it) }
        if (_callbacks.isNotEmpty()) mock.callbacks(_callbacks)
        _extract?.let { mock.extract(it) }

        return mock
    }
}
