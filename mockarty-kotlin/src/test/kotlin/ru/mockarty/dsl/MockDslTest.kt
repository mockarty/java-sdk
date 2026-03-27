// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.dsl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import ru.mockarty.model.AssertAction
import ru.mockarty.model.Protocol
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MockDslTest {

    private val mapper = ObjectMapper().also {
        it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Nested
    @DisplayName("HTTP mock DSL")
    inner class HttpDslTests {

        @Test
        @DisplayName("should build basic HTTP mock")
        fun basicHttpMock() {
            val m = mock {
                id = "dsl-http-test"
                namespace = "test"

                http {
                    route = "/api/users/:id"
                    method = "GET"
                }

                respond {
                    statusCode = 200
                    body = mapOf("name" to "John")
                }
            }

            assertEquals("dsl-http-test", m.id)
            assertEquals("test", m.namespace)
            assertNotNull(m.http)
            assertEquals("/api/users/:id", m.http.route)
            assertEquals("GET", m.http.httpMethod)
            assertEquals(Protocol.HTTP, m.protocol())
            assertEquals(200, m.response.statusCode)
        }

        @Test
        @DisplayName("should build HTTP mock with conditions")
        fun httpWithConditions() {
            val m = mock {
                id = "http-conditions"

                http {
                    route = "/api/users"
                    method = "POST"
                    condition("$.name", AssertAction.NOT_EMPTY)
                    headerCondition("Authorization", AssertAction.NOT_EMPTY)
                    queryCondition("format", AssertAction.EQUALS, "json")
                }

                respond {
                    statusCode = 201
                }
            }

            assertEquals(1, m.http.conditions.size)
            assertEquals(1, m.http.header.size)
            assertEquals(1, m.http.queryParams.size)
        }

        @Test
        @DisplayName("should build HTTP mock with all options")
        fun httpFullOptions() {
            val m = mock {
                id = "full-options"
                namespace = "production"
                chainId = "user-flow"
                tags = listOf("users", "v2")
                ttl = 3600
                priority = 10
                pathPrefix = "/v2"
                useLimiter = 100
                folderId = "folder-1"

                http {
                    route = "/api/users/:id"
                    method = "GET"
                }

                respond {
                    statusCode = 200
                    body = mapOf(
                        "id" to "$.pathParam.id",
                        "name" to "$.fake.FirstName",
                        "email" to "$.fake.Email"
                    )
                    delay = 100
                    headers = mapOf("X-Custom" to listOf("value"))
                }
            }

            assertEquals("full-options", m.id)
            assertEquals("production", m.namespace)
            assertEquals("user-flow", m.chainId)
            assertEquals(listOf("users", "v2"), m.tags)
            assertEquals(3600L, m.ttl)
            assertEquals(10L, m.priority)
            assertEquals("/v2", m.pathPrefix)
            assertEquals(100, m.useLimiter)
            assertEquals("folder-1", m.folderId)
            assertEquals(100, m.response.delay)
        }

        @Test
        @DisplayName("should build HTTP mock with simple respond shortcut")
        fun httpSimpleRespond() {
            val m = mock {
                http {
                    route = "/api/ping"
                    method = "GET"
                }
                respond(200, "pong")
            }

            assertEquals(200, m.response.statusCode)
            assertEquals("pong", m.response.payload)
        }
    }

    @Nested
    @DisplayName("gRPC mock DSL")
    inner class GrpcDslTests {

        @Test
        @DisplayName("should build gRPC mock")
        fun basicGrpcMock() {
            val m = mock {
                id = "grpc-test"

                grpc {
                    service = "UserService"
                    method = "GetUser"
                    methodType = "unary"
                    condition("$.user_id", AssertAction.EQUALS, "123")
                    metaCondition("x-request-id", AssertAction.NOT_EMPTY)
                }

                respond {
                    statusCode = 200
                    body = mapOf("name" to "John")
                }
            }

            assertEquals(Protocol.GRPC, m.protocol())
            assertEquals("UserService", m.grpc.service)
            assertEquals("GetUser", m.grpc.method)
            assertEquals("unary", m.grpc.methodType)
            assertEquals(1, m.grpc.conditions.size)
            assertEquals(1, m.grpc.meta.size)
        }
    }

    @Nested
    @DisplayName("MCP mock DSL")
    inner class McpDslTests {

        @Test
        @DisplayName("should build MCP mock")
        fun basicMcpMock() {
            val m = mock {
                id = "mcp-test"

                mcp {
                    tool = "search_documents"
                    description = "Search indexed documents"
                    condition("$.query", AssertAction.NOT_EMPTY)
                }

                respond {
                    statusCode = 200
                    body = mapOf("results" to listOf("doc1", "doc2"))
                }
            }

            assertEquals(Protocol.MCP, m.protocol())
            assertEquals("search_documents", m.mcp.tool)
            assertEquals("tools/call", m.mcp.method)
            assertEquals("Search indexed documents", m.mcp.description)
        }
    }

    @Nested
    @DisplayName("Other protocol DSL tests")
    inner class OtherProtocolDslTests {

        @Test
        @DisplayName("should build SOAP mock")
        fun soapMock() {
            val m = mock {
                soap {
                    service = "PaymentService"
                    method = "ProcessPayment"
                    action = "urn:ProcessPayment"
                }
                respond(200, "<Result>OK</Result>")
            }

            assertEquals(Protocol.SOAP, m.protocol())
            assertEquals("PaymentService", m.soap.service)
        }

        @Test
        @DisplayName("should build GraphQL mock")
        fun graphqlMock() {
            val m = mock {
                graphql {
                    operation = "query"
                    field = "user"
                    path = "/graphql"
                }
                respond(200, mapOf("data" to mapOf("user" to mapOf("name" to "John"))))
            }

            assertEquals(Protocol.GRAPHQL, m.protocol())
            assertEquals("query", m.graphql.operation)
            assertEquals("user", m.graphql.field)
        }

        @Test
        @DisplayName("should build SSE mock")
        fun sseMock() {
            val m = mock {
                sse {
                    eventPath = "/events/updates"
                    eventName = "update"
                    description = "Real-time updates"
                }
                respond(200)
            }

            assertEquals(Protocol.SSE, m.protocol())
            assertEquals("/events/updates", m.sse.eventPath)
        }

        @Test
        @DisplayName("should build Kafka mock")
        fun kafkaMock() {
            val m = mock {
                kafka {
                    topic = "orders"
                    serverName = "kafka-cluster"
                    consumerGroup = "order-consumers"
                    outputTopic = "orders-responses"
                }
                respond(200, mapOf("processed" to true))
            }

            assertEquals(Protocol.KAFKA, m.protocol())
            assertEquals("orders", m.kafka.topic)
            assertEquals("orders-responses", m.kafka.outputTopic)
        }

        @Test
        @DisplayName("should build RabbitMQ mock")
        fun rabbitmqMock() {
            val m = mock {
                rabbitmq {
                    queue = "orders-queue"
                    serverName = "rabbit-server"
                    exchange = "orders"
                    routingKey = "order.created"
                }
                respond(200)
            }

            assertEquals(Protocol.RABBITMQ, m.protocol())
            assertEquals("orders-queue", m.rabbitmq.queue)
        }

        @Test
        @DisplayName("should build SMTP mock")
        fun smtpMock() {
            val m = mock {
                smtp {
                    serverName = "mail-server"
                    senderCondition("", AssertAction.CONTAINS, "@company.com")
                    subjectCondition("", AssertAction.CONTAINS, "Invoice")
                }
                respond(200)
            }

            assertEquals(Protocol.SMTP, m.protocol())
            assertEquals("mail-server", m.smtp.serverName)
        }
    }

    @Nested
    @DisplayName("Proxy DSL")
    inner class ProxyDslTests {

        @Test
        @DisplayName("should build proxy mock")
        fun proxyMock() {
            val m = mock {
                http {
                    route = "/api/proxy"
                    method = "GET"
                }
                proxyTo("https://backend.example.com")
            }

            assertNotNull(m.proxy)
            assertEquals("https://backend.example.com", m.proxy.target)
        }
    }

    @Nested
    @DisplayName("Callback DSL")
    inner class CallbackDslTests {

        @Test
        @DisplayName("should build mock with simple callback")
        fun simpleCallback() {
            val m = mock {
                http {
                    route = "/api/order"
                    method = "POST"
                }
                respond(201)
                callback("https://webhook.example.com/notify", "POST", mapOf("event" to "created"))
            }

            assertEquals(1, m.callbacks.size)
            assertEquals("https://webhook.example.com/notify", m.callbacks[0].url)
        }

        @Test
        @DisplayName("should build mock with detailed callback DSL")
        fun detailedCallback() {
            val m = mock {
                http {
                    route = "/api/order"
                    method = "POST"
                }
                respond(201)
                callback {
                    type = "kafka"
                    kafkaBrokers = "broker:9092"
                    kafkaTopic = "events"
                    body = mapOf("type" to "order.created")
                    timeout = 30
                    retryCount = 3
                }
            }

            assertEquals(1, m.callbacks.size)
            assertEquals("kafka", m.callbacks[0].type)
            assertEquals("broker:9092", m.callbacks[0].kafkaBrokers)
        }
    }

    @Nested
    @DisplayName("OneOf DSL")
    inner class OneOfDslTests {

        @Test
        @DisplayName("should build ordered OneOf responses")
        fun orderedOneOf() {
            val m = mock {
                http {
                    route = "/api/flaky"
                    method = "GET"
                }
                oneOfOrdered {
                    respond(200, mapOf("status" to "ok"))
                    respond(500, "error")
                    respond {
                        statusCode = 200
                        body = mapOf("status" to "recovered")
                    }
                }
            }

            assertNotNull(m.oneOf)
            assertEquals("order", m.oneOf.order)
            assertEquals(3, m.oneOf.responses.size)
        }

        @Test
        @DisplayName("should build random OneOf responses")
        fun randomOneOf() {
            val m = mock {
                http {
                    route = "/api/random"
                    method = "GET"
                }
                oneOfRandom {
                    respond(200, "response-a")
                    respond(200, "response-b")
                }
            }

            assertEquals("random", m.oneOf.order)
            assertEquals(2, m.oneOf.responses.size)
        }
    }

    @Nested
    @DisplayName("Extract DSL")
    inner class ExtractDslTests {

        @Test
        @DisplayName("should build mock with extract")
        fun extractMock() {
            val m = mock {
                http {
                    route = "/api/register"
                    method = "POST"
                }
                respond(201)
                extract {
                    gStore = mapOf("lastUserId" to "$.userId")
                    cStore = mapOf("step" to "1")
                    mStore = mapOf("requestTime" to "$.timestamp")
                }
            }

            assertNotNull(m.extract)
            assertEquals("$.userId", m.extract.gStore["lastUserId"])
            assertEquals("1", m.extract.cStore["step"])
        }
    }

    @Nested
    @DisplayName("JSON serialization")
    inner class SerializationTests {

        @Test
        @DisplayName("should serialize DSL-built mock to valid JSON")
        fun serializeToJson() {
            val m = mock {
                id = "serialization-test"
                namespace = "test"
                tags = listOf("api", "v1")

                http {
                    route = "/api/test"
                    method = "POST"
                    condition("$.name", AssertAction.EQUALS, "test")
                }

                respond {
                    statusCode = 200
                    body = mapOf("result" to "ok")
                }

                ttl = 600
            }

            val json = mapper.writeValueAsString(m)

            // Verify JSON structure matches server expectations
            assertTrue(json.contains("\"id\":\"serialization-test\""))
            assertTrue(json.contains("\"namespace\":\"test\""))
            assertTrue(json.contains("\"http\""))
            assertTrue(json.contains("\"route\":\"/api/test\""))
            assertTrue(json.contains("\"httpMethod\":\"POST\""))
            assertTrue(json.contains("\"conditions\""))
            assertTrue(json.contains("\"assertAction\":\"equals\""))
            assertTrue(json.contains("\"response\""))
            assertTrue(json.contains("\"statusCode\":200"))
            assertTrue(json.contains("\"ttl\":600"))

            // Null fields should be omitted
            assertFalse(json.contains("\"grpc\""))
            assertFalse(json.contains("\"mcp\""))
        }

        @Test
        @DisplayName("should produce round-trip serializable JSON")
        fun roundTrip() {
            val original = mock {
                id = "round-trip"
                http {
                    route = "/api/test"
                    method = "GET"
                }
                respond {
                    statusCode = 200
                    body = mapOf("key" to "value")
                }
            }

            val json = mapper.writeValueAsString(original)
            val deserialized = mapper.readValue(json, ru.mockarty.model.Mock::class.java)

            assertEquals(original.id, deserialized.id)
            assertEquals(original.http.route, deserialized.http.route)
            assertEquals(original.http.httpMethod, deserialized.http.httpMethod)
            assertEquals(original.response.statusCode, deserialized.response.statusCode)
        }
    }
}
