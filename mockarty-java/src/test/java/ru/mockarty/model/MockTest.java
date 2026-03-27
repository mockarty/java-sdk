// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.builder.ConditionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Nested
    @DisplayName("JSON Serialization")
    class SerializationTests {

        @Test
        @DisplayName("should serialize HTTP mock to JSON with correct property names")
        void serializeHttpMock() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("test-mock")
                    .namespace("sandbox")
                    .http(new HttpRequestContext()
                            .route("/api/users/:id")
                            .httpMethod("GET")
                            .addCondition(Condition.equals("$.userId", "123"))
                            .addHeader(Condition.notEmpty("Authorization")))
                    .response(new ContentResponse()
                            .statusCode(200)
                            .payload(Map.of("name", "John")))
                    .ttl(3600L)
                    .priority(10L)
                    .tags("users", "v2");

            String json = mapper.writeValueAsString(mock);

            // Verify JSON property names match Go server expectations
            assertTrue(json.contains("\"id\""));
            assertTrue(json.contains("\"namespace\""));
            assertTrue(json.contains("\"http\""));
            assertTrue(json.contains("\"route\""));
            assertTrue(json.contains("\"httpMethod\""));
            assertTrue(json.contains("\"conditions\""));
            assertTrue(json.contains("\"header\""));
            assertTrue(json.contains("\"statusCode\""));
            assertTrue(json.contains("\"payload\""));
            assertTrue(json.contains("\"ttl\""));
            assertTrue(json.contains("\"priority\""));
            assertTrue(json.contains("\"tags\""));

            // Null fields should not be present
            assertFalse(json.contains("\"grpc\""));
            assertFalse(json.contains("\"mcp\""));
            assertFalse(json.contains("\"proxy\""));
        }

        @Test
        @DisplayName("should serialize gRPC mock correctly")
        void serializeGrpcMock() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("grpc-mock")
                    .grpc(new GrpcRequestContext()
                            .service("UserService")
                            .method("GetUser")
                            .methodType("unary"))
                    .response(new ContentResponse()
                            .payload(Map.of("name", "John")));

            String json = mapper.writeValueAsString(mock);

            assertTrue(json.contains("\"service\""));
            assertTrue(json.contains("\"method\""));
            assertTrue(json.contains("\"methodType\""));
        }

        @Test
        @DisplayName("should serialize MCP mock correctly")
        void serializeMcpMock() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("mcp-mock")
                    .mcp(new MCPRequestContext()
                            .tool("search_documents")
                            .method("tools/call")
                            .description("Search the document index"))
                    .response(new ContentResponse()
                            .payload(Map.of("results", List.of())));

            String json = mapper.writeValueAsString(mock);

            assertTrue(json.contains("\"tool\""));
            assertTrue(json.contains("\"description\""));
        }

        @Test
        @DisplayName("should serialize Condition with assertAction correctly")
        void serializeCondition() throws JsonProcessingException {
            Condition cond = Condition.of("$.name", AssertAction.EQUALS, "John");

            String json = mapper.writeValueAsString(cond);

            assertTrue(json.contains("\"assertAction\":\"equals\""));
            assertTrue(json.contains("\"path\":\"$.name\""));
            assertTrue(json.contains("\"value\":\"John\""));
        }

        @Test
        @DisplayName("should serialize AssertAction enum values correctly")
        void serializeAssertActions() throws JsonProcessingException {
            assertEquals("\"equals\"", mapper.writeValueAsString(AssertAction.EQUALS));
            assertEquals("\"contains\"", mapper.writeValueAsString(AssertAction.CONTAINS));
            assertEquals("\"not_equals\"", mapper.writeValueAsString(AssertAction.NOT_EQUALS));
            assertEquals("\"not_contains\"", mapper.writeValueAsString(AssertAction.NOT_CONTAINS));
            assertEquals("\"any\"", mapper.writeValueAsString(AssertAction.ANY));
            assertEquals("\"notEmpty\"", mapper.writeValueAsString(AssertAction.NOT_EMPTY));
            assertEquals("\"empty\"", mapper.writeValueAsString(AssertAction.EMPTY));
            assertEquals("\"matches\"", mapper.writeValueAsString(AssertAction.MATCHES));
        }

        @Test
        @DisplayName("should serialize Protocol enum values correctly")
        void serializeProtocol() throws JsonProcessingException {
            assertEquals("\"http\"", mapper.writeValueAsString(Protocol.HTTP));
            assertEquals("\"grpc\"", mapper.writeValueAsString(Protocol.GRPC));
            assertEquals("\"mcp\"", mapper.writeValueAsString(Protocol.MCP));
            assertEquals("\"socket\"", mapper.writeValueAsString(Protocol.SOCKET));
            assertEquals("\"soap\"", mapper.writeValueAsString(Protocol.SOAP));
            assertEquals("\"graphql\"", mapper.writeValueAsString(Protocol.GRAPHQL));
            assertEquals("\"sse\"", mapper.writeValueAsString(Protocol.SSE));
            assertEquals("\"kafka\"", mapper.writeValueAsString(Protocol.KAFKA));
            assertEquals("\"rabbitmq\"", mapper.writeValueAsString(Protocol.RABBITMQ));
            assertEquals("\"smtp\"", mapper.writeValueAsString(Protocol.SMTP));
        }

        @Test
        @DisplayName("should serialize OneOf correctly")
        void serializeOneOf() throws JsonProcessingException {
            OneOf oneOf = OneOf.ordered(
                    new ContentResponse().statusCode(200).payload("ok"),
                    new ContentResponse().statusCode(500).error("fail")
            );

            String json = mapper.writeValueAsString(oneOf);

            assertTrue(json.contains("\"order\":\"order\""));
            assertTrue(json.contains("\"responses\""));
        }

        @Test
        @DisplayName("should serialize Proxy correctly")
        void serializeProxy() throws JsonProcessingException {
            Proxy proxy = Proxy.to("https://backend.example.com");
            String json = mapper.writeValueAsString(proxy);

            assertTrue(json.contains("\"target\":\"https://backend.example.com\""));
        }

        @Test
        @DisplayName("should serialize Callback correctly")
        void serializeCallback() throws JsonProcessingException {
            Callback cb = Callback.http("https://webhook.example.com", "POST",
                    Map.of("event", "created"));
            cb.timeout(30).retryCount(3).async(true);

            String json = mapper.writeValueAsString(cb);

            assertTrue(json.contains("\"url\""));
            assertTrue(json.contains("\"method\""));
            assertTrue(json.contains("\"body\""));
            assertTrue(json.contains("\"timeout\""));
            assertTrue(json.contains("\"retryCount\""));
        }

        @Test
        @DisplayName("should serialize webhooks field name correctly")
        void serializeWebhooksFieldName() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("webhook-test")
                    .http(new HttpRequestContext().route("/api/test").httpMethod("GET"))
                    .addCallback(Callback.http("http://example.com", "POST", null));

            String json = mapper.writeValueAsString(mock);

            // The Go server uses "webhooks" as the JSON field name for callbacks
            assertTrue(json.contains("\"webhooks\""));
        }

        @Test
        @DisplayName("should serialize Kafka request context correctly")
        void serializeKafkaContext() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("kafka-mock")
                    .kafka(new KafkaRequestContext()
                            .topic("orders")
                            .serverName("kafka-cluster")
                            .consumerGroup("order-consumers")
                            .outputTopic("orders-responses")
                            .outputBrokers("broker1:9092,broker2:9092"));

            String json = mapper.writeValueAsString(mock);

            assertTrue(json.contains("\"topic\""));
            assertTrue(json.contains("\"consumerGroup\""));
            assertTrue(json.contains("\"outputTopic\""));
            assertTrue(json.contains("\"outputBrokers\""));
        }

        @Test
        @DisplayName("should serialize RabbitMQ request context correctly")
        void serializeRabbitMQContext() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("rabbit-mock")
                    .rabbitmq(new RabbitMQRequestContext()
                            .queue("orders-queue")
                            .exchange("orders-exchange")
                            .routingKey("order.created")
                            .outputURL("amqp://localhost:5672")
                            .outputExchange("responses"));

            String json = mapper.writeValueAsString(mock);

            assertTrue(json.contains("\"queue\""));
            assertTrue(json.contains("\"exchange\""));
            assertTrue(json.contains("\"routingKey\""));
            assertTrue(json.contains("\"outputURL\""));
            assertTrue(json.contains("\"outputExchange\""));
        }

        @Test
        @DisplayName("should serialize SMTP request context correctly")
        void serializeSmtpContext() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("smtp-mock")
                    .smtp(new SmtpRequestContext()
                            .serverName("mail-server")
                            .addSenderCondition(Condition.contains("", "@company.com"))
                            .addSubjectCondition(Condition.contains("", "Invoice")));

            String json = mapper.writeValueAsString(mock);

            assertTrue(json.contains("\"serverName\""));
            assertTrue(json.contains("\"senderConditions\""));
            assertTrue(json.contains("\"subjectConditions\""));
        }

        @Test
        @DisplayName("should serialize SSE request context correctly")
        void serializeSseContext() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("sse-mock")
                    .sse(new SSERequestContext()
                            .eventPath("/events/updates")
                            .eventName("update")
                            .description("Real-time updates stream"));

            String json = mapper.writeValueAsString(mock);

            assertTrue(json.contains("\"eventPath\""));
            assertTrue(json.contains("\"eventName\""));
            assertTrue(json.contains("\"description\""));
        }

        @Test
        @DisplayName("should serialize Extract correctly")
        void serializeExtract() throws JsonProcessingException {
            Mock mock = new Mock()
                    .id("extract-mock")
                    .http(new HttpRequestContext().route("/api/test").httpMethod("POST"))
                    .extract(new Extract()
                            .gStore(Map.of("lastUserId", "$.userId"))
                            .cStore(Map.of("step", "1")))
                    .response(new ContentResponse().statusCode(200));

            String json = mapper.writeValueAsString(mock);

            assertTrue(json.contains("\"extract\""));
            assertTrue(json.contains("\"gStore\""));
            assertTrue(json.contains("\"cStore\""));
        }
    }

    @Nested
    @DisplayName("JSON Deserialization")
    class DeserializationTests {

        @Test
        @DisplayName("should deserialize HTTP mock from JSON")
        void deserializeHttpMock() throws JsonProcessingException {
            String json = "{\"id\":\"test-mock\",\"namespace\":\"sandbox\"," +
                    "\"http\":{\"route\":\"/api/users/:id\",\"httpMethod\":\"GET\"}," +
                    "\"response\":{\"statusCode\":200,\"payload\":{\"name\":\"John\"}}," +
                    "\"ttl\":3600,\"priority\":10,\"tags\":[\"users\",\"v2\"]}";

            Mock mock = mapper.readValue(json, Mock.class);

            assertEquals("test-mock", mock.getId());
            assertEquals("sandbox", mock.getNamespace());
            assertNotNull(mock.getHttp());
            assertEquals("/api/users/:id", mock.getHttp().getRoute());
            assertEquals("GET", mock.getHttp().getHttpMethod());
            assertEquals(200, mock.getResponse().getStatusCode());
            assertEquals(3600L, mock.getTtl());
            assertEquals(10L, mock.getPriority());
            assertEquals(2, mock.getTags().size());
        }

        @Test
        @DisplayName("should deserialize AssertAction from JSON strings")
        void deserializeAssertAction() throws JsonProcessingException {
            assertEquals(AssertAction.EQUALS, mapper.readValue("\"equals\"", AssertAction.class));
            assertEquals(AssertAction.CONTAINS, mapper.readValue("\"contains\"", AssertAction.class));
            assertEquals(AssertAction.NOT_EQUALS, mapper.readValue("\"not_equals\"", AssertAction.class));
            assertEquals(AssertAction.NOT_EMPTY, mapper.readValue("\"notEmpty\"", AssertAction.class));
            assertEquals(AssertAction.MATCHES, mapper.readValue("\"matches\"", AssertAction.class));
        }

        @Test
        @DisplayName("should deserialize SaveMockResponse")
        void deserializeSaveMockResponse() throws JsonProcessingException {
            String json = "{\"overwritten\":true,\"mock\":{\"id\":\"updated-mock\"}}";

            SaveMockResponse response = mapper.readValue(json, SaveMockResponse.class);

            assertTrue(response.isOverwritten());
            assertEquals("updated-mock", response.getMock().getId());
        }

        @Test
        @DisplayName("should deserialize HealthResponse")
        void deserializeHealthResponse() throws JsonProcessingException {
            String json = "{\"status\":\"pass\",\"releaseId\":\"1.2.3\"," +
                    "\"checks\":{\"database\":[{\"status\":\"pass\"}]}}";

            HealthResponse response = mapper.readValue(json, HealthResponse.class);

            assertEquals("pass", response.getStatus());
            assertEquals("1.2.3", response.getReleaseId());
            assertTrue(response.isHealthy());
        }

        @Test
        @DisplayName("should handle unknown fields gracefully")
        void unknownFields() throws JsonProcessingException {
            String json = "{\"id\":\"test\",\"unknownField\":\"value\",\"anotherUnknown\":123}";

            Mock mock = mapper.readValue(json, Mock.class);
            assertEquals("test", mock.getId());
        }

        @Test
        @DisplayName("should deserialize mock with conditions")
        void deserializeWithConditions() throws JsonProcessingException {
            String json = "{\"id\":\"cond-mock\"," +
                    "\"http\":{\"route\":\"/api/test\",\"httpMethod\":\"POST\"," +
                    "\"conditions\":[{\"path\":\"$.name\",\"assertAction\":\"equals\",\"value\":\"John\"}]," +
                    "\"header\":[{\"path\":\"Authorization\",\"assertAction\":\"notEmpty\"}]}}";

            Mock mock = mapper.readValue(json, Mock.class);

            assertNotNull(mock.getHttp().getConditions());
            assertEquals(1, mock.getHttp().getConditions().size());
            assertEquals("$.name", mock.getHttp().getConditions().get(0).getPath());
            assertEquals(AssertAction.EQUALS, mock.getHttp().getConditions().get(0).getAssertAction());
            assertEquals("John", mock.getHttp().getConditions().get(0).getValue());
        }
    }

    @Nested
    @DisplayName("Protocol detection")
    class ProtocolDetectionTests {

        @Test
        @DisplayName("should detect HTTP protocol")
        void detectHttp() {
            Mock mock = new Mock().http(new HttpRequestContext().route("/test"));
            assertEquals(Protocol.HTTP, mock.protocol());
        }

        @Test
        @DisplayName("should detect gRPC protocol")
        void detectGrpc() {
            Mock mock = new Mock().grpc(new GrpcRequestContext().service("Svc"));
            assertEquals(Protocol.GRPC, mock.protocol());
        }

        @Test
        @DisplayName("should detect MCP protocol")
        void detectMcp() {
            Mock mock = new Mock().mcp(new MCPRequestContext().tool("test"));
            assertEquals(Protocol.MCP, mock.protocol());
        }

        @Test
        @DisplayName("should detect SOAP protocol")
        void detectSoap() {
            Mock mock = new Mock().soap(new SoapRequestContext().service("Svc"));
            assertEquals(Protocol.SOAP, mock.protocol());
        }

        @Test
        @DisplayName("should detect GraphQL protocol")
        void detectGraphql() {
            Mock mock = new Mock().graphql(new GraphQLRequestContext().operation("query"));
            assertEquals(Protocol.GRAPHQL, mock.protocol());
        }

        @Test
        @DisplayName("should detect SSE protocol")
        void detectSse() {
            Mock mock = new Mock().sse(new SSERequestContext().eventPath("/events"));
            assertEquals(Protocol.SSE, mock.protocol());
        }

        @Test
        @DisplayName("should detect Kafka protocol")
        void detectKafka() {
            Mock mock = new Mock().kafka(new KafkaRequestContext().topic("test"));
            assertEquals(Protocol.KAFKA, mock.protocol());
        }

        @Test
        @DisplayName("should detect RabbitMQ protocol")
        void detectRabbitmq() {
            Mock mock = new Mock().rabbitmq(new RabbitMQRequestContext().queue("test"));
            assertEquals(Protocol.RABBITMQ, mock.protocol());
        }

        @Test
        @DisplayName("should detect SMTP protocol")
        void detectSmtp() {
            Mock mock = new Mock().smtp(new SmtpRequestContext().serverName("mail"));
            assertEquals(Protocol.SMTP, mock.protocol());
        }

        @Test
        @DisplayName("should default to HTTP when no context set")
        void defaultHttp() {
            Mock mock = new Mock();
            assertEquals(Protocol.HTTP, mock.protocol());
        }
    }

    @Nested
    @DisplayName("ConditionBuilder")
    class ConditionBuilderTests {

        @Test
        @DisplayName("should build conditions list")
        void buildConditions() {
            List<Condition> conditions = ConditionBuilder.create()
                    .equals("$.name", "John")
                    .contains("$.email", "@example.com")
                    .notEmpty("$.phone")
                    .matches("$.zipCode", "\\d{5}")
                    .build();

            assertEquals(4, conditions.size());
            assertEquals(AssertAction.EQUALS, conditions.get(0).getAssertAction());
            assertEquals(AssertAction.CONTAINS, conditions.get(1).getAssertAction());
            assertEquals(AssertAction.NOT_EMPTY, conditions.get(2).getAssertAction());
            assertEquals(AssertAction.MATCHES, conditions.get(3).getAssertAction());
        }
    }

    @Nested
    @DisplayName("Page model")
    class PageTests {

        @Test
        @DisplayName("should create empty page")
        void emptyPage() {
            Page<Mock> page = Page.empty();
            assertEquals(0, page.getTotal());
            assertTrue(page.getItems().isEmpty());
            assertFalse(page.hasMore());
        }

        @Test
        @DisplayName("should detect when more pages exist")
        void hasMore() {
            Page<Mock> page = new Page<>(List.of(new Mock()), 100, 0, 10);
            assertTrue(page.hasMore());
        }

        @Test
        @DisplayName("should detect last page")
        void lastPage() {
            Page<Mock> page = new Page<>(List.of(new Mock()), 10, 9, 10);
            assertFalse(page.hasMore());
        }

        @Test
        @DisplayName("should deserialize Page from JSON")
        void deserializePage() throws JsonProcessingException {
            String json = "{\"items\":[{\"id\":\"mock1\"},{\"id\":\"mock2\"}],\"total\":50,\"offset\":0,\"limit\":10}";

            // Use JavaType for parameterized Page<Mock>
            var pageType = mapper.getTypeFactory()
                    .constructParametricType(Page.class, Mock.class);
            Page<Mock> page = mapper.readValue(json, pageType);

            assertEquals(50, page.getTotal());
            assertEquals(0, page.getOffset());
            assertEquals(10, page.getLimit());
            assertEquals(2, page.getItems().size());
            assertEquals("mock1", page.getItems().get(0).getId());
            assertTrue(page.hasMore());
        }
    }
}
