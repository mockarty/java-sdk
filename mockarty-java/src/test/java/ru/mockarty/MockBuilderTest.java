// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty;

import ru.mockarty.builder.MockBuilder;
import ru.mockarty.builder.ResponseBuilder;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.Callback;
import ru.mockarty.model.ContentResponse;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Protocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockBuilderTest {

    @Nested
    @DisplayName("HTTP mock builder")
    class HttpBuilderTests {

        @Test
        @DisplayName("should build a basic HTTP GET mock")
        void basicHttpGet() {
            Mock mock = MockBuilder.http("/api/users/:id", "GET")
                    .id("get-user")
                    .build();

            assertEquals("get-user", mock.getId());
            assertNotNull(mock.getHttp());
            assertEquals("/api/users/:id", mock.getHttp().getRoute());
            assertEquals("GET", mock.getHttp().getHttpMethod());
            assertEquals(Protocol.HTTP, mock.protocol());
        }

        @Test
        @DisplayName("should build HTTP mock with full configuration")
        void fullHttpMock() {
            Mock mock = MockBuilder.http("/api/users/:id", "GET")
                    .id("user-service-get")
                    .namespace("production")
                    .tags("users", "v2")
                    .priority(10)
                    .ttl(3600)
                    .pathPrefix("/v2")
                    .headerCondition("Authorization", AssertAction.NOT_EMPTY, null)
                    .condition("$.userId", AssertAction.EQUALS, "123")
                    .queryCondition("format", AssertAction.EQUALS, "json")
                    .respond(200, Map.of(
                            "id", "$.pathParam.id",
                            "name", "$.fake.FirstName",
                            "email", "$.fake.Email"
                    ))
                    .build();

            assertEquals("user-service-get", mock.getId());
            assertEquals("production", mock.getNamespace());
            assertEquals(2, mock.getTags().size());
            assertTrue(mock.getTags().contains("users"));
            assertEquals(10L, mock.getPriority());
            assertEquals(3600L, mock.getTtl());
            assertEquals("/v2", mock.getPathPrefix());
            assertNotNull(mock.getHttp().getConditions());
            assertEquals(1, mock.getHttp().getConditions().size());
            assertNotNull(mock.getHttp().getHeader());
            assertEquals(1, mock.getHttp().getHeader().size());
            assertNotNull(mock.getHttp().getQueryParams());
            assertEquals(1, mock.getHttp().getQueryParams().size());
            assertNotNull(mock.getResponse());
            assertEquals(200, mock.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should build HTTP mock with delay")
        void httpWithDelay() {
            Mock mock = MockBuilder.http("/api/slow", "GET")
                    .respondWithDelay(200, Map.of("status", "ok"), 500)
                    .build();

            assertEquals(500, mock.getResponse().getDelay());
        }

        @Test
        @DisplayName("should build HTTP mock with error response")
        void httpWithError() {
            Mock mock = MockBuilder.http("/api/error", "GET")
                    .respondWithError(500, "Internal Server Error")
                    .build();

            assertEquals(500, mock.getResponse().getStatusCode());
            assertEquals("Internal Server Error", mock.getResponse().getError());
        }

        @Test
        @DisplayName("should build HTTP mock with template response")
        void httpWithTemplate() {
            Mock mock = MockBuilder.http("/api/template", "GET")
                    .respondFromTemplate(200, "/templates/response.json")
                    .build();

            assertEquals("/templates/response.json", mock.getResponse().getPayloadTemplatePath());
        }

        @Test
        @DisplayName("should build HTTP mock with custom ContentResponse")
        void httpWithCustomResponse() {
            ContentResponse response = ResponseBuilder.create()
                    .statusCode(201)
                    .payload(Map.of("created", true))
                    .header("X-Request-Id", "abc-123")
                    .delay(100)
                    .build();

            Mock mock = MockBuilder.http("/api/users", "POST")
                    .respond(response)
                    .build();

            assertEquals(201, mock.getResponse().getStatusCode());
            assertEquals(100, mock.getResponse().getDelay());
            assertNotNull(mock.getResponse().getHeaders());
        }
    }

    @Nested
    @DisplayName("gRPC mock builder")
    class GrpcBuilderTests {

        @Test
        @DisplayName("should build a gRPC mock")
        void basicGrpc() {
            Mock mock = MockBuilder.grpc("UserService", "GetUser")
                    .id("grpc-get-user")
                    .serverName("test-server")
                    .condition("$.user_id", AssertAction.EQUALS, "123")
                    .respond(200, Map.of("name", "John", "email", "john@example.com"))
                    .build();

            assertEquals("grpc-get-user", mock.getId());
            assertNotNull(mock.getGrpc());
            assertEquals("UserService", mock.getGrpc().getService());
            assertEquals("GetUser", mock.getGrpc().getMethod());
            assertEquals(Protocol.GRPC, mock.protocol());
            assertEquals(1, mock.getGrpc().getConditions().size());
        }

        @Test
        @DisplayName("should add metadata conditions for gRPC")
        void grpcWithMeta() {
            Mock mock = MockBuilder.grpc("UserService", "GetUser")
                    .headerCondition("x-request-id", AssertAction.NOT_EMPTY, null)
                    .respond(200)
                    .build();

            assertNotNull(mock.getGrpc().getMeta());
            assertEquals(1, mock.getGrpc().getMeta().size());
        }
    }

    @Nested
    @DisplayName("MCP mock builder")
    class McpBuilderTests {

        @Test
        @DisplayName("should build an MCP mock")
        void basicMcp() {
            Mock mock = MockBuilder.mcp("search_documents")
                    .id("mcp-search")
                    .respond(200, Map.of("results", java.util.List.of("doc1", "doc2")))
                    .build();

            assertEquals("mcp-search", mock.getId());
            assertNotNull(mock.getMcp());
            assertEquals("search_documents", mock.getMcp().getTool());
            assertEquals("tools/call", mock.getMcp().getMethod());
            assertEquals(Protocol.MCP, mock.protocol());
        }
    }

    @Nested
    @DisplayName("SOAP mock builder")
    class SoapBuilderTests {

        @Test
        @DisplayName("should build a SOAP mock")
        void basicSoap() {
            Mock mock = MockBuilder.soap("PaymentService", "ProcessPayment")
                    .id("soap-payment")
                    .respond(200, "<PaymentResult><status>OK</status></PaymentResult>")
                    .build();

            assertNotNull(mock.getSoap());
            assertEquals("PaymentService", mock.getSoap().getService());
            assertEquals("ProcessPayment", mock.getSoap().getMethod());
            assertEquals(Protocol.SOAP, mock.protocol());
        }
    }

    @Nested
    @DisplayName("GraphQL mock builder")
    class GraphQLBuilderTests {

        @Test
        @DisplayName("should build a GraphQL mock")
        void basicGraphql() {
            Mock mock = MockBuilder.graphql("query", "user")
                    .id("graphql-user")
                    .respond(200, Map.of(
                            "data", Map.of("user", Map.of("name", "John"))
                    ))
                    .build();

            assertNotNull(mock.getGraphql());
            assertEquals("query", mock.getGraphql().getOperation());
            assertEquals("user", mock.getGraphql().getField());
            assertEquals(Protocol.GRAPHQL, mock.protocol());
        }
    }

    @Nested
    @DisplayName("OneOf responses")
    class OneOfTests {

        @Test
        @DisplayName("should build mock with ordered OneOf responses")
        void orderedOneOf() {
            Mock mock = MockBuilder.http("/api/flaky", "GET")
                    .oneOfOrdered(
                            new ContentResponse().statusCode(200).payload(Map.of("status", "ok")),
                            new ContentResponse().statusCode(500).error("server error"),
                            new ContentResponse().statusCode(200).payload(Map.of("status", "recovered"))
                    )
                    .build();

            assertNotNull(mock.getOneOf());
            assertEquals("order", mock.getOneOf().getOrder());
            assertEquals(3, mock.getOneOf().getResponses().size());
        }

        @Test
        @DisplayName("should build mock with random OneOf responses")
        void randomOneOf() {
            Mock mock = MockBuilder.http("/api/random", "GET")
                    .oneOfRandom(
                            new ContentResponse().statusCode(200).payload("response-a"),
                            new ContentResponse().statusCode(200).payload("response-b")
                    )
                    .build();

            assertEquals("random", mock.getOneOf().getOrder());
            assertEquals(2, mock.getOneOf().getResponses().size());
        }
    }

    @Nested
    @DisplayName("Proxy")
    class ProxyTests {

        @Test
        @DisplayName("should build mock with proxy")
        void proxyMock() {
            Mock mock = MockBuilder.http("/api/proxy", "GET")
                    .proxyTo("https://real-service.example.com")
                    .build();

            assertNotNull(mock.getProxy());
            assertEquals("https://real-service.example.com", mock.getProxy().getTarget());
        }
    }

    @Nested
    @DisplayName("Callbacks")
    class CallbackTests {

        @Test
        @DisplayName("should build mock with HTTP callback")
        void httpCallback() {
            Mock mock = MockBuilder.http("/api/order", "POST")
                    .respond(201, Map.of("orderId", "123"))
                    .callback("https://webhook.example.com/notify", "POST",
                            Map.of("event", "order.created"))
                    .build();

            assertNotNull(mock.getCallbacks());
            assertEquals(1, mock.getCallbacks().size());
            assertEquals("https://webhook.example.com/notify", mock.getCallbacks().get(0).getUrl());
        }

        @Test
        @DisplayName("should build mock with custom callback")
        void customCallback() {
            Callback kafkaCallback = Callback.kafka("broker:9092", "events", Map.of("type", "created"));

            Mock mock = MockBuilder.http("/api/order", "POST")
                    .respond(201)
                    .callback(kafkaCallback)
                    .build();

            assertEquals("kafka", mock.getCallbacks().get(0).getType());
        }
    }

    @Nested
    @DisplayName("Additional protocol builders")
    class AdditionalProtocolTests {

        @Test
        @DisplayName("should build SSE mock")
        void sseMock() {
            Mock mock = MockBuilder.sse("/events/notifications", "notification")
                    .id("sse-notifications")
                    .respond(200)
                    .build();

            assertNotNull(mock.getSse());
            assertEquals("/events/notifications", mock.getSse().getEventPath());
            assertEquals("notification", mock.getSse().getEventName());
            assertEquals(Protocol.SSE, mock.protocol());
        }

        @Test
        @DisplayName("should build Kafka mock")
        void kafkaMock() {
            Mock mock = MockBuilder.kafka("orders-topic", "kafka-server")
                    .id("kafka-orders")
                    .respond(200, Map.of("status", "processed"))
                    .build();

            assertNotNull(mock.getKafka());
            assertEquals("orders-topic", mock.getKafka().getTopic());
            assertEquals(Protocol.KAFKA, mock.protocol());
        }

        @Test
        @DisplayName("should build RabbitMQ mock")
        void rabbitmqMock() {
            Mock mock = MockBuilder.rabbitmq("orders-queue", "rabbit-server")
                    .id("rabbit-orders")
                    .respond(200)
                    .build();

            assertNotNull(mock.getRabbitmq());
            assertEquals("orders-queue", mock.getRabbitmq().getQueue());
            assertEquals(Protocol.RABBITMQ, mock.protocol());
        }

        @Test
        @DisplayName("should build SMTP mock")
        void smtpMock() {
            Mock mock = MockBuilder.smtp("mail-server")
                    .id("smtp-mock")
                    .respond(200)
                    .build();

            assertNotNull(mock.getSmtp());
            assertEquals("mail-server", mock.getSmtp().getServerName());
            assertEquals(Protocol.SMTP, mock.protocol());
        }
    }

    @Nested
    @DisplayName("Chain and misc options")
    class MiscTests {

        @Test
        @DisplayName("should set chain ID")
        void chainId() {
            Mock mock = MockBuilder.http("/api/step1", "POST")
                    .id("step-1")
                    .chainId("registration-flow")
                    .respond(200)
                    .build();

            assertEquals("registration-flow", mock.getChainId());
        }

        @Test
        @DisplayName("should set use limiter")
        void useLimiter() {
            Mock mock = MockBuilder.http("/api/limited", "GET")
                    .useLimiter(5)
                    .respond(200)
                    .build();

            assertEquals(5, mock.getUseLimiter());
        }

        @Test
        @DisplayName("should set folder ID")
        void folderId() {
            Mock mock = MockBuilder.http("/api/test", "GET")
                    .folderId("folder-123")
                    .respond(200)
                    .build();

            assertEquals("folder-123", mock.getFolderId());
        }
    }
}
