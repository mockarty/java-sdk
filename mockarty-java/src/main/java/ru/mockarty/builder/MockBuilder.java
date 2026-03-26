// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.builder;

import ru.mockarty.model.AssertAction;
import ru.mockarty.model.Callback;
import ru.mockarty.model.Condition;
import ru.mockarty.model.ContentResponse;
import ru.mockarty.model.Extract;
import ru.mockarty.model.GrpcRequestContext;
import ru.mockarty.model.GraphQLRequestContext;
import ru.mockarty.model.HttpRequestContext;
import ru.mockarty.model.KafkaRequestContext;
import ru.mockarty.model.MCPRequestContext;
import ru.mockarty.model.Mock;
import ru.mockarty.model.OneOf;
import ru.mockarty.model.Proxy;
import ru.mockarty.model.RabbitMQRequestContext;
import ru.mockarty.model.SmtpRequestContext;
import ru.mockarty.model.SoapRequestContext;
import ru.mockarty.model.SSERequestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating Mock objects.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * Mock mock = MockBuilder.http("/api/users/:id", "GET")
 *     .id("user-service-get")
 *     .namespace("production")
 *     .tags("users", "v2")
 *     .headerCondition("Authorization", AssertAction.NOT_EMPTY, null)
 *     .respond(200, Map.of(
 *         "id", "$.pathParam.id",
 *         "name", "$.fake.FirstName"
 *     ))
 *     .ttl(3600)
 *     .build();
 * }</pre>
 */
public class MockBuilder {

    private final Mock mock;
    private HttpRequestContext httpCtx;
    private GrpcRequestContext grpcCtx;
    private MCPRequestContext mcpCtx;
    private SoapRequestContext soapCtx;
    private GraphQLRequestContext graphqlCtx;
    private SSERequestContext sseCtx;
    private KafkaRequestContext kafkaCtx;
    private RabbitMQRequestContext rabbitmqCtx;
    private SmtpRequestContext smtpCtx;

    private MockBuilder() {
        this.mock = new Mock();
    }

    // Factory methods for each protocol

    /**
     * Creates a builder for an HTTP mock.
     *
     * @param route  the URL route pattern (e.g., "/api/users/:id")
     * @param method the HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    public static MockBuilder http(String route, String method) {
        MockBuilder builder = new MockBuilder();
        builder.httpCtx = new HttpRequestContext()
                .route(route)
                .httpMethod(method);
        return builder;
    }

    /**
     * Creates a builder for a gRPC mock.
     *
     * @param service the gRPC service name
     * @param method  the gRPC method name
     */
    public static MockBuilder grpc(String service, String method) {
        MockBuilder builder = new MockBuilder();
        builder.grpcCtx = new GrpcRequestContext()
                .service(service)
                .method(method);
        return builder;
    }

    /**
     * Creates a builder for an MCP mock.
     *
     * @param tool the MCP tool name
     */
    public static MockBuilder mcp(String tool) {
        MockBuilder builder = new MockBuilder();
        builder.mcpCtx = new MCPRequestContext()
                .tool(tool)
                .method("tools/call");
        return builder;
    }

    /**
     * Creates a builder for a SOAP mock.
     *
     * @param service the SOAP service name
     * @param method  the SOAP method name
     */
    public static MockBuilder soap(String service, String method) {
        MockBuilder builder = new MockBuilder();
        builder.soapCtx = new SoapRequestContext()
                .service(service)
                .method(method);
        return builder;
    }

    /**
     * Creates a builder for a GraphQL mock.
     *
     * @param operation the GraphQL operation type (query, mutation, subscription)
     * @param field     the GraphQL field name
     */
    public static MockBuilder graphql(String operation, String field) {
        MockBuilder builder = new MockBuilder();
        builder.graphqlCtx = new GraphQLRequestContext()
                .operation(operation)
                .field(field);
        return builder;
    }

    /**
     * Creates a builder for an SSE mock.
     *
     * @param eventPath the SSE event path
     * @param eventName the SSE event name
     */
    public static MockBuilder sse(String eventPath, String eventName) {
        MockBuilder builder = new MockBuilder();
        builder.sseCtx = new SSERequestContext()
                .eventPath(eventPath)
                .eventName(eventName);
        return builder;
    }

    /**
     * Creates a builder for a Kafka mock.
     *
     * @param topic      the Kafka topic
     * @param serverName the Kafka server name
     */
    public static MockBuilder kafka(String topic, String serverName) {
        MockBuilder builder = new MockBuilder();
        builder.kafkaCtx = new KafkaRequestContext()
                .topic(topic)
                .serverName(serverName);
        return builder;
    }

    /**
     * Creates a builder for a RabbitMQ mock.
     *
     * @param queue      the RabbitMQ queue name
     * @param serverName the RabbitMQ server name
     */
    public static MockBuilder rabbitmq(String queue, String serverName) {
        MockBuilder builder = new MockBuilder();
        builder.rabbitmqCtx = new RabbitMQRequestContext()
                .queue(queue)
                .serverName(serverName);
        return builder;
    }

    /**
     * Creates a builder for an SMTP mock.
     *
     * @param serverName the SMTP server name
     */
    public static MockBuilder smtp(String serverName) {
        MockBuilder builder = new MockBuilder();
        builder.smtpCtx = new SmtpRequestContext()
                .serverName(serverName);
        return builder;
    }

    // Common configuration methods

    /**
     * Sets the mock ID.
     */
    public MockBuilder id(String id) {
        mock.id(id);
        return this;
    }

    /**
     * Sets the namespace for the mock.
     */
    public MockBuilder namespace(String namespace) {
        mock.namespace(namespace);
        return this;
    }

    /**
     * Sets the chain ID for linking related mocks.
     */
    public MockBuilder chainId(String chainId) {
        mock.chainId(chainId);
        return this;
    }

    /**
     * Sets tags for categorizing the mock.
     */
    public MockBuilder tags(String... tags) {
        mock.tags(new ArrayList<>(Arrays.asList(tags)));
        return this;
    }

    /**
     * Sets the priority for mock matching (higher = matched first).
     */
    public MockBuilder priority(long priority) {
        mock.priority(priority);
        return this;
    }

    /**
     * Sets the TTL (time to live) in seconds.
     */
    public MockBuilder ttl(long seconds) {
        mock.ttl(seconds);
        return this;
    }

    /**
     * Sets the path prefix for the mock.
     */
    public MockBuilder pathPrefix(String prefix) {
        mock.pathPrefix(prefix);
        return this;
    }

    /**
     * Sets the server name for grouping mocks by environment.
     */
    public MockBuilder serverName(String serverName) {
        mock.serverName(serverName);
        return this;
    }

    /**
     * Sets the folder ID for hierarchical organization.
     */
    public MockBuilder folderId(String folderId) {
        mock.folderId(folderId);
        return this;
    }

    /**
     * Sets the use limiter (max number of times mock can be matched).
     */
    public MockBuilder useLimiter(int limit) {
        mock.useLimiter(limit);
        return this;
    }

    // Condition methods

    /**
     * Adds a body/data condition to the mock's request context.
     */
    public MockBuilder condition(String path, AssertAction action, Object value) {
        Condition cond = Condition.of(path, action, value);
        addConditionToContext(cond);
        return this;
    }

    /**
     * Adds a header condition to the mock's request context.
     */
    public MockBuilder headerCondition(String name, AssertAction action, Object value) {
        Condition cond = Condition.of(name, action, value);
        addHeaderConditionToContext(cond);
        return this;
    }

    /**
     * Adds a query parameter condition (HTTP only).
     */
    public MockBuilder queryCondition(String name, AssertAction action, Object value) {
        if (httpCtx == null) {
            throw new IllegalStateException("Query conditions are only supported for HTTP mocks");
        }
        httpCtx.addQueryParam(Condition.of(name, action, value));
        return this;
    }

    // Response methods

    /**
     * Sets a response with only a status code.
     */
    public MockBuilder respond(int statusCode) {
        mock.response(new ContentResponse().statusCode(statusCode));
        return this;
    }

    /**
     * Sets a response with status code and body.
     */
    public MockBuilder respond(int statusCode, Object body) {
        mock.response(new ContentResponse().statusCode(statusCode).payload(body));
        return this;
    }

    /**
     * Sets a response with status code, body, and headers.
     */
    public MockBuilder respond(int statusCode, Object body, Map<String, List<String>> headers) {
        mock.response(new ContentResponse().statusCode(statusCode).payload(body).headers(headers));
        return this;
    }

    /**
     * Sets a response with status code, body, and delay.
     */
    public MockBuilder respondWithDelay(int statusCode, Object body, int delayMs) {
        mock.response(new ContentResponse().statusCode(statusCode).payload(body).delay(delayMs));
        return this;
    }

    /**
     * Sets a response with an error.
     */
    public MockBuilder respondWithError(int statusCode, String error) {
        mock.response(new ContentResponse().statusCode(statusCode).error(error));
        return this;
    }

    /**
     * Sets a response using a payload template path.
     */
    public MockBuilder respondFromTemplate(int statusCode, String templatePath) {
        mock.response(new ContentResponse().statusCode(statusCode).payloadTemplatePath(templatePath));
        return this;
    }

    /**
     * Sets a custom ContentResponse.
     */
    public MockBuilder respond(ContentResponse response) {
        mock.response(response);
        return this;
    }

    /**
     * Sets multiple OneOf responses with ordered delivery.
     */
    public MockBuilder oneOfOrdered(ContentResponse... responses) {
        mock.oneOf(OneOf.ordered(responses));
        return this;
    }

    /**
     * Sets multiple OneOf responses with random delivery.
     */
    public MockBuilder oneOfRandom(ContentResponse... responses) {
        mock.oneOf(OneOf.random(responses));
        return this;
    }

    // Proxy method

    /**
     * Configures the mock to proxy requests to a target URL.
     */
    public MockBuilder proxyTo(String target) {
        mock.proxy(new Proxy(target));
        return this;
    }

    // Callback methods

    /**
     * Adds an HTTP callback (webhook) that fires when the mock is matched.
     */
    public MockBuilder callback(String url, String method, Object body) {
        mock.addCallback(Callback.http(url, method, body));
        return this;
    }

    /**
     * Adds a custom callback.
     */
    public MockBuilder callback(Callback callback) {
        mock.addCallback(callback);
        return this;
    }

    // Extract methods

    /**
     * Sets extraction rules for populating stores from request data.
     */
    public MockBuilder extract(Extract extract) {
        mock.extract(extract);
        return this;
    }

    /**
     * Sets initial mock store values.
     */
    public MockBuilder mockStore(Map<String, Object> store) {
        mock.mockStore(store);
        return this;
    }

    /**
     * Builds the Mock object.
     */
    public Mock build() {
        if (httpCtx != null) mock.http(httpCtx);
        if (grpcCtx != null) mock.grpc(grpcCtx);
        if (mcpCtx != null) mock.mcp(mcpCtx);
        if (soapCtx != null) mock.soap(soapCtx);
        if (graphqlCtx != null) mock.graphql(graphqlCtx);
        if (sseCtx != null) mock.sse(sseCtx);
        if (kafkaCtx != null) mock.kafka(kafkaCtx);
        if (rabbitmqCtx != null) mock.rabbitmq(rabbitmqCtx);
        if (smtpCtx != null) mock.smtp(smtpCtx);
        return mock;
    }

    // Private helper methods

    private void addConditionToContext(Condition condition) {
        if (httpCtx != null) {
            httpCtx.addCondition(condition);
        } else if (grpcCtx != null) {
            grpcCtx.addCondition(condition);
        } else if (mcpCtx != null) {
            mcpCtx.addCondition(condition);
        } else if (soapCtx != null) {
            soapCtx.addCondition(condition);
        } else if (graphqlCtx != null) {
            graphqlCtx.addCondition(condition);
        } else if (sseCtx != null) {
            sseCtx.addCondition(condition);
        } else if (kafkaCtx != null) {
            kafkaCtx.addCondition(condition);
        } else if (rabbitmqCtx != null) {
            rabbitmqCtx.addCondition(condition);
        }
    }

    private void addHeaderConditionToContext(Condition condition) {
        if (httpCtx != null) {
            httpCtx.addHeader(condition);
        } else if (grpcCtx != null) {
            grpcCtx.addMeta(condition);
        } else if (mcpCtx != null) {
            mcpCtx.addHeader(condition);
        } else if (soapCtx != null) {
            soapCtx.addHeader(condition);
        } else if (graphqlCtx != null) {
            graphqlCtx.addHeader(condition);
        } else if (sseCtx != null) {
            sseCtx.addHeaderCondition(condition);
        } else if (kafkaCtx != null) {
            kafkaCtx.addHeader(condition);
        } else if (rabbitmqCtx != null) {
            rabbitmqCtx.addHeader(condition);
        } else if (smtpCtx != null) {
            smtpCtx.addHeaderCondition(condition);
        }
    }
}
