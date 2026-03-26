// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.dsl

import ru.mockarty.model.*

/**
 * DSL scope for HTTP request context configuration.
 */
@MockartyDslMarker
class HttpScope {
    var route: String = ""
    var method: String = "GET"
    var routePattern: String? = null
    var sortArray: Boolean? = null

    private val conditions = mutableListOf<Condition>()
    private val queryConditions = mutableListOf<Condition>()
    private val headerConditions = mutableListOf<Condition>()

    fun condition(path: String, action: AssertAction, value: Any? = null) {
        conditions.add(Condition.of(path, action, value))
    }

    fun queryCondition(name: String, action: AssertAction, value: Any? = null) {
        queryConditions.add(Condition.of(name, action, value))
    }

    fun headerCondition(name: String, action: AssertAction, value: Any? = null) {
        headerConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): HttpRequestContext {
        val ctx = HttpRequestContext().route(route).httpMethod(method)
        routePattern?.let { ctx.routePattern(it) }
        sortArray?.let { ctx.sortArray(it) }
        if (conditions.isNotEmpty()) ctx.conditions(conditions)
        if (queryConditions.isNotEmpty()) ctx.queryParams(queryConditions)
        if (headerConditions.isNotEmpty()) ctx.header(headerConditions)
        return ctx
    }
}

/**
 * DSL scope for gRPC request context configuration.
 */
@MockartyDslMarker
class GrpcScope {
    var service: String = ""
    var method: String = ""
    var methodType: String? = null
    var sortArray: Boolean? = null

    private val conditions = mutableListOf<Condition>()
    private val metaConditions = mutableListOf<Condition>()

    fun condition(path: String, action: AssertAction, value: Any? = null) {
        conditions.add(Condition.of(path, action, value))
    }

    fun metaCondition(name: String, action: AssertAction, value: Any? = null) {
        metaConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): GrpcRequestContext {
        val ctx = GrpcRequestContext().service(service).method(method)
        methodType?.let { ctx.methodType(it) }
        sortArray?.let { ctx.sortArray(it) }
        if (conditions.isNotEmpty()) ctx.conditions(conditions)
        if (metaConditions.isNotEmpty()) ctx.meta(metaConditions)
        return ctx
    }
}

/**
 * DSL scope for MCP request context configuration.
 */
@MockartyDslMarker
class McpScope {
    var tool: String = ""
    var method: String = "tools/call"
    var resource: String? = null
    var description: String? = null
    var sortArray: Boolean? = null

    private val conditions = mutableListOf<Condition>()
    private val headerConditions = mutableListOf<Condition>()

    fun condition(path: String, action: AssertAction, value: Any? = null) {
        conditions.add(Condition.of(path, action, value))
    }

    fun headerCondition(name: String, action: AssertAction, value: Any? = null) {
        headerConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): MCPRequestContext {
        val ctx = MCPRequestContext().tool(tool).method(method)
        resource?.let { ctx.resource(it) }
        description?.let { ctx.description(it) }
        sortArray?.let { ctx.sortArray(it) }
        if (conditions.isNotEmpty()) ctx.conditions(conditions)
        if (headerConditions.isNotEmpty()) ctx.header(headerConditions)
        return ctx
    }
}

/**
 * DSL scope for SOAP request context configuration.
 */
@MockartyDslMarker
class SoapScope {
    var service: String = ""
    var method: String = ""
    var action: String? = null
    var path: String? = null
    var sortArray: Boolean? = null

    private val conditions = mutableListOf<Condition>()
    private val headerConditions = mutableListOf<Condition>()

    fun condition(path: String, action: AssertAction, value: Any? = null) {
        conditions.add(Condition.of(path, action, value))
    }

    fun headerCondition(name: String, action: AssertAction, value: Any? = null) {
        headerConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): SoapRequestContext {
        val ctx = SoapRequestContext().service(service).method(method)
        action?.let { ctx.action(it) }
        path?.let { ctx.path(it) }
        sortArray?.let { ctx.sortArray(it) }
        if (conditions.isNotEmpty()) ctx.conditions(conditions)
        if (headerConditions.isNotEmpty()) ctx.header(headerConditions)
        return ctx
    }
}

/**
 * DSL scope for GraphQL request context configuration.
 */
@MockartyDslMarker
class GraphqlScope {
    var operation: String = ""
    var field: String = ""
    var type: String? = null
    var path: String? = null
    var sortArray: Boolean? = null

    private val conditions = mutableListOf<Condition>()
    private val headerConditions = mutableListOf<Condition>()

    fun condition(path: String, action: AssertAction, value: Any? = null) {
        conditions.add(Condition.of(path, action, value))
    }

    fun headerCondition(name: String, action: AssertAction, value: Any? = null) {
        headerConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): GraphQLRequestContext {
        val ctx = GraphQLRequestContext().operation(operation).field(field)
        type?.let { ctx.type(it) }
        path?.let { ctx.path(it) }
        sortArray?.let { ctx.sortArray(it) }
        if (conditions.isNotEmpty()) ctx.conditions(conditions)
        if (headerConditions.isNotEmpty()) ctx.header(headerConditions)
        return ctx
    }
}

/**
 * DSL scope for SSE request context configuration.
 */
@MockartyDslMarker
class SseScope {
    var eventPath: String = ""
    var eventName: String = ""
    var description: String? = null
    var sortArray: Boolean? = null

    private val conditions = mutableListOf<Condition>()
    private val headerConditions = mutableListOf<Condition>()

    fun condition(path: String, action: AssertAction, value: Any? = null) {
        conditions.add(Condition.of(path, action, value))
    }

    fun headerCondition(name: String, action: AssertAction, value: Any? = null) {
        headerConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): SSERequestContext {
        val ctx = SSERequestContext().eventPath(eventPath).eventName(eventName)
        description?.let { ctx.description(it) }
        sortArray?.let { ctx.sortArray(it) }
        if (conditions.isNotEmpty()) ctx.conditions(conditions)
        if (headerConditions.isNotEmpty()) ctx.headerConditions(headerConditions)
        return ctx
    }
}

/**
 * DSL scope for Kafka request context configuration.
 */
@MockartyDslMarker
class KafkaScope {
    var topic: String = ""
    var serverName: String = ""
    var consumerGroup: String? = null
    var sortArray: Boolean? = null
    var outputTopic: String? = null
    var outputBrokers: String? = null
    var outputKey: String? = null
    var outputHeaders: Map<String, String>? = null

    private val conditions = mutableListOf<Condition>()
    private val headerConditions = mutableListOf<Condition>()

    fun condition(path: String, action: AssertAction, value: Any? = null) {
        conditions.add(Condition.of(path, action, value))
    }

    fun headerCondition(name: String, action: AssertAction, value: Any? = null) {
        headerConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): KafkaRequestContext {
        val ctx = KafkaRequestContext().topic(topic).serverName(serverName)
        consumerGroup?.let { ctx.consumerGroup(it) }
        sortArray?.let { ctx.sortArray(it) }
        outputTopic?.let { ctx.outputTopic(it) }
        outputBrokers?.let { ctx.outputBrokers(it) }
        outputKey?.let { ctx.outputKey(it) }
        outputHeaders?.let { ctx.outputHeaders(it) }
        if (conditions.isNotEmpty()) ctx.conditions(conditions)
        if (headerConditions.isNotEmpty()) ctx.headers(headerConditions)
        return ctx
    }
}

/**
 * DSL scope for RabbitMQ request context configuration.
 */
@MockartyDslMarker
class RabbitmqScope {
    var queue: String = ""
    var exchange: String? = null
    var routingKey: String? = null
    var serverName: String = ""
    var sortArray: Boolean? = null
    var outputURL: String? = null
    var outputExchange: String? = null
    var outputRoutingKey: String? = null
    var outputQueue: String? = null

    private val conditions = mutableListOf<Condition>()
    private val headerConditions = mutableListOf<Condition>()

    fun condition(path: String, action: AssertAction, value: Any? = null) {
        conditions.add(Condition.of(path, action, value))
    }

    fun headerCondition(name: String, action: AssertAction, value: Any? = null) {
        headerConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): RabbitMQRequestContext {
        val ctx = RabbitMQRequestContext().queue(queue).serverName(serverName)
        exchange?.let { ctx.exchange(it) }
        routingKey?.let { ctx.routingKey(it) }
        sortArray?.let { ctx.sortArray(it) }
        outputURL?.let { ctx.outputURL(it) }
        outputExchange?.let { ctx.outputExchange(it) }
        outputRoutingKey?.let { ctx.outputRoutingKey(it) }
        outputQueue?.let { ctx.outputQueue(it) }
        if (conditions.isNotEmpty()) ctx.conditions(conditions)
        if (headerConditions.isNotEmpty()) ctx.headers(headerConditions)
        return ctx
    }
}

/**
 * DSL scope for SMTP request context configuration.
 */
@MockartyDslMarker
class SmtpScope {
    var serverName: String = ""
    var sortArray: Boolean? = null

    private val senderConditions = mutableListOf<Condition>()
    private val recipientConditions = mutableListOf<Condition>()
    private val subjectConditions = mutableListOf<Condition>()
    private val bodyConditions = mutableListOf<Condition>()
    private val headerConditions = mutableListOf<Condition>()

    fun senderCondition(path: String, action: AssertAction, value: Any? = null) {
        senderConditions.add(Condition.of(path, action, value))
    }

    fun recipientCondition(path: String, action: AssertAction, value: Any? = null) {
        recipientConditions.add(Condition.of(path, action, value))
    }

    fun subjectCondition(path: String, action: AssertAction, value: Any? = null) {
        subjectConditions.add(Condition.of(path, action, value))
    }

    fun bodyCondition(path: String, action: AssertAction, value: Any? = null) {
        bodyConditions.add(Condition.of(path, action, value))
    }

    fun headerCondition(name: String, action: AssertAction, value: Any? = null) {
        headerConditions.add(Condition.of(name, action, value))
    }

    internal fun build(): SmtpRequestContext {
        val ctx = SmtpRequestContext().serverName(serverName)
        sortArray?.let { ctx.sortArray(it) }
        if (senderConditions.isNotEmpty()) ctx.senderConditions(senderConditions)
        if (recipientConditions.isNotEmpty()) ctx.recipientConditions(recipientConditions)
        if (subjectConditions.isNotEmpty()) ctx.subjectConditions(subjectConditions)
        if (bodyConditions.isNotEmpty()) ctx.bodyConditions(bodyConditions)
        if (headerConditions.isNotEmpty()) ctx.headerConditions(headerConditions)
        return ctx
    }
}

/**
 * DSL scope for response configuration.
 */
@MockartyDslMarker
class ResponseScope {
    var statusCode: Int = 200
    var body: Any? = null
    var templatePath: String? = null
    var error: String? = null
    var delay: Int? = null
    var decode: String? = null
    var headers: Map<String, List<String>>? = null

    internal fun build(): ContentResponse {
        val response = ContentResponse().statusCode(statusCode)
        body?.let { response.payload(it) }
        templatePath?.let { response.payloadTemplatePath(it) }
        error?.let { response.error(it) }
        delay?.let { response.delay(it) }
        decode?.let { response.decode(it) }
        headers?.let { response.headers(it) }
        return response
    }
}

/**
 * DSL scope for callback configuration.
 */
@MockartyDslMarker
class CallbackScope {
    var type: String = "http"
    var url: String? = null
    var method: String = "POST"
    var body: Any? = null
    var headers: Map<String, String>? = null
    var timeout: Int? = null
    var retryCount: Int? = null
    var async: Boolean? = null
    var trigger: String? = null

    // Kafka-specific
    var kafkaBrokers: String? = null
    var kafkaTopic: String? = null
    var kafkaKey: String? = null

    // RabbitMQ-specific
    var rabbitURL: String? = null
    var rabbitExchange: String? = null
    var rabbitRoutingKey: String? = null

    internal fun build(): Callback {
        val cb = Callback().type(type).method(method)
        url?.let { cb.url(it) }
        body?.let { cb.body(it) }
        headers?.let { cb.headers(it) }
        timeout?.let { cb.timeout(it) }
        retryCount?.let { cb.retryCount(it) }
        async?.let { cb.async(it) }
        trigger?.let { cb.trigger(it) }
        kafkaBrokers?.let { cb.kafkaBrokers(it) }
        kafkaTopic?.let { cb.kafkaTopic(it) }
        kafkaKey?.let { cb.kafkaKey(it) }
        rabbitURL?.let { cb.rabbitURL(it) }
        rabbitExchange?.let { cb.rabbitExchange(it) }
        rabbitRoutingKey?.let { cb.rabbitRoutingKey(it) }
        return cb
    }
}

/**
 * DSL scope for OneOf response configuration.
 */
@MockartyDslMarker
class OneOfScope {
    internal val responses = mutableListOf<ContentResponse>()

    fun respond(init: ResponseScope.() -> Unit) {
        responses.add(ResponseScope().apply(init).build())
    }

    fun respond(statusCode: Int, body: Any? = null) {
        val response = ContentResponse().statusCode(statusCode)
        body?.let { response.payload(it) }
        responses.add(response)
    }
}

/**
 * DSL scope for extract configuration.
 */
@MockartyDslMarker
class ExtractScope {
    var mStore: Map<String, Any>? = null
    var cStore: Map<String, Any>? = null
    var gStore: Map<String, Any>? = null

    internal fun build(): Extract {
        val extract = Extract()
        mStore?.let { extract.mStore(it) }
        cStore?.let { extract.cStore(it) }
        gStore?.let { extract.gStore(it) }
        return extract
    }
}
