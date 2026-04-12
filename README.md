<p align="center">
  <img src="https://raw.githubusercontent.com/mockarty/java-sdk/main/logo.svg" alt="Mockarty" width="400">
</p>

<h1 align="center">Java SDK</h1>

<p align="center">
  Official Java client library for <a href="https://mockarty.ru">Mockarty</a> — a multi-protocol mock server for HTTP, gRPC, MCP, GraphQL, SOAP, SSE, WebSocket, Kafka, RabbitMQ, and SMTP.
</p>

<p align="center">
  <a href="https://central.sonatype.com/namespace/ru.mockarty"><img src="https://img.shields.io/maven-central/v/ru.mockarty/mockarty-java" alt="Maven Central"></a>
  <a href="https://github.com/mockarty/java-sdk/blob/main/LICENSE"><img src="https://img.shields.io/github/license/mockarty/java-sdk" alt="License"></a>
</p>

## Modules

| Module | Description |
|--------|-------------|
| `mockarty-java` | Core client library with builders and model classes |
| `mockarty-junit5` | JUnit 5 extension for test integration |
| `mockarty-kotlin` | Kotlin DSL and extension functions |

## Requirements

- Java 11+
- Mockarty server running (default: `http://localhost:5770`)

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    // Core SDK
    implementation("ru.mockarty:mockarty-java:0.3.0")

    // JUnit 5 extension (test scope)
    testImplementation("ru.mockarty:mockarty-junit5:0.3.0")

    // Kotlin DSL (optional)
    implementation("ru.mockarty:mockarty-kotlin:0.3.0")
}
```

### Maven

```xml
<dependency>
    <groupId>ru.mockarty</groupId>
    <artifactId>mockarty-java</artifactId>
    <version>0.3.0</version>
</dependency>
```

## Quick Start

### Java

```java
import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.Mock;

try (MockartyClient client = MockartyClient.builder()
        .baseUrl("http://localhost:5770")
        .apiKey("your-api-key")
        .namespace("sandbox")
        .build()) {

    // Create an HTTP mock
    Mock mock = MockBuilder.http("/api/users/:id", "GET")
        .id("user-service-get")
        .headerCondition("Authorization", AssertAction.NOT_EMPTY, null)
        .respond(200, Map.of(
            "id", "$.pathParam.id",
            "name", "$.fake.FirstName",
            "email", "$.fake.Email"
        ))
        .ttl(3600)
        .build();

    client.mocks().create(mock);

    // Check health
    boolean healthy = client.health().ready();
}
```

### Kotlin DSL

```kotlin
import ru.mockarty.MockartyClient
import ru.mockarty.dsl.*
import ru.mockarty.model.AssertAction

val client = MockartyClient.create("http://localhost:5770", "your-api-key")

client.createMock {
    id = "user-service-get"
    namespace = "production"
    tags = listOf("users", "v2")

    http {
        route = "/api/users/:id"
        method = "GET"
        headerCondition("Authorization", AssertAction.NOT_EMPTY)
    }

    respond {
        statusCode = 200
        body = mapOf(
            "id" to "$.pathParam.id",
            "name" to "$.fake.FirstName"
        )
    }

    ttl = 3600
}
```

### JUnit 5

```java
import ru.mockarty.junit5.MockartyTest;
import ru.mockarty.junit5.MockartyServer;
import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;

@MockartyTest(namespace = "test", cleanupAfterEach = true)
class UserApiTest {

    @Test
    void shouldReturnUser(MockartyClient client, MockartyServer server) {
        server.createMock(MockBuilder.http("/api/users/1", "GET")
            .respond(200, Map.of("id", 1, "name", "John"))
            .build());

        // Your test code here...
        // Mocks are automatically cleaned up after each test
    }
}
```

## Supported Protocols

### HTTP

```java
MockBuilder.http("/api/users/:id", "GET")
    .condition("$.role", AssertAction.EQUALS, "admin")
    .headerCondition("Authorization", AssertAction.NOT_EMPTY, null)
    .queryCondition("format", AssertAction.EQUALS, "json")
    .respond(200, Map.of("name", "$.fake.FirstName"))
    .build();
```

### gRPC

```java
MockBuilder.grpc("UserService", "GetUser")
    .serverName("grpc-server")
    .condition("$.user_id", AssertAction.EQUALS, "123")
    .respond(200, Map.of("name", "John", "email", "john@test.com"))
    .build();
```

### MCP (Model Context Protocol)

```java
MockBuilder.mcp("search_documents")
    .condition("$.query", AssertAction.NOT_EMPTY, null)
    .respond(200, Map.of("results", List.of("doc1", "doc2")))
    .build();
```

### GraphQL

```java
MockBuilder.graphql("query", "user")
    .respond(200, Map.of(
        "data", Map.of("user", Map.of("name", "John"))
    ))
    .build();
```

### SOAP

```java
MockBuilder.soap("PaymentService", "ProcessPayment")
    .respond(200, "<PaymentResult><status>OK</status></PaymentResult>")
    .build();
```

## Features

### OneOf Responses

Return different responses in sequence or randomly:

```java
MockBuilder.http("/api/flaky", "GET")
    .oneOfOrdered(
        new ContentResponse().statusCode(200).payload(Map.of("status", "ok")),
        new ContentResponse().statusCode(500).error("server error"),
        new ContentResponse().statusCode(200).payload(Map.of("status", "recovered"))
    )
    .build();
```

### Proxy

Forward requests to a real backend:

```java
MockBuilder.http("/api/real-service", "GET")
    .proxyTo("https://api.example.com")
    .build();
```

### Callbacks (Webhooks)

Fire webhooks when a mock is matched:

```java
MockBuilder.http("/api/orders", "POST")
    .respond(201, Map.of("orderId", "123"))
    .callback("https://webhook.example.com/notify", "POST",
        Map.of("event", "order.created"))
    .build();
```

### Store Operations

```java
// Global store
client.stores().globalSet("counter", 0);
Map<String, Object> store = client.stores().globalGet();

// Chain store
client.stores().chainSet("registration-flow", "step", "1");
```

## Configuration

The client can be configured via builder, environment variables, or system properties:

| Setting | Builder | Env Variable | Default |
|---------|---------|-------------|---------|
| Base URL | `.baseUrl()` | `MOCKARTY_BASE_URL` | `http://localhost:5770` |
| API Key | `.apiKey()` | `MOCKARTY_API_KEY` | (none) |
| Namespace | `.namespace()` | `MOCKARTY_NAMESPACE` | `sandbox` |
| Timeout | `.timeout()` | - | 30 seconds |

## License

MIT License. See [LICENSE](LICENSE) for details.
