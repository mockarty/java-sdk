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
| `mockarty-protocols` | Test clients for gRPC / Kafka / RabbitMQ / SOAP / GraphQL / SSE / WebSocket with auto-step capture |

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
import ru.mockarty.model.PluginProtocolCatalogue;

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

    // Discover active plugin wire codecs and their Socket routing name.
    PluginProtocolCatalogue protocols = client.mocks().listPluginProtocols();

    // Check health
    boolean healthy = client.health().ready();
}
```

### Fluent Tester DSL

For end-to-end tests that exercise multiple protocols, the
`ru.mockarty.tester` package provides a fluent chain mirroring the Go
and Python SDKs:

```java
import ru.mockarty.tester.Tester;

@Test
void userSignupFlow() {
    Tester t = new Tester.Builder()
        .baseUrl("http://localhost:8080")
        .build();
    t.http().post("/signup")
        .json(Map.of("email", "a@b.c"))
        .expectStatus(201)
        .extract("$.token", "token");
    t.http().get("/me")
        .header("Authorization", "Bearer {{token}}")
        .expectStatus(200)
        .expectJsonPath("$.email", "a@b.c");
    t.graphql("/gql")
        .query("{ me { id } }", null)
        .expectStatus(200)
        .expectNoErrors();
    t.finish();
    assertTrue(t.ok(), () -> t.errors().toString());
}
```

Vocabulary: `expectStatus`, `expectHeader`, `expectBodyContains`,
`expectJsonPath`, `expectJsonArrayLen`, `extract`. Built on the JDK 11
stdlib `HttpClient` — zero new dependencies.

Upload a Tester chain as a TCM external run in one call:

```java
import ru.mockarty.tester.ExternalRunBridge;

Tester t = new Tester.Builder().baseUrl("http://...").build();
t.http().get("/me").expectStatus(200);
t.finish();

client.externalRuns().report("qa",
    ExternalRunBridge.toExternalRunRequest(t,
        new ExternalRunBridge.Options()
            .caseName("me-endpoint")
            .autoCreate(true)));
```

`ExternalRunBridge.toExternalRunRequest(t, opts)` maps Tester report
to `ExternalRunRequest`: per-step `protocol/method/url/statusOrCode`
go into `metadata`, multi-failure errors join with `"; "`,
`ISO_INSTANT` timestamps. Same vocabulary as the Go
(`tester.ToExternalRun`) and Python (`tester.to_report_kwargs`) SDKs.

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

### Allure compatibility (mirror mode)

`@MockartyTest` enables Allure mirror mode by default. Existing tests
that use `io.qameta.allure.*` annotations (`@Severity`, `@Feature`,
`@Story`, `@Owner`, `@Description`, `@Issue`, `@TmsLink`, `@Link`,
`@Epic`, `@Tag`, `@Label`, `@Title`, `@Parameter`) flow into the
Mockarty case frame without refactoring. Detection is reflection-based,
results are cached per test method, and Mockarty does not require
`io.qameta.allure:allure-java-commons` on the user's classpath. Disable
the scan with `@MockartyTest(mirrorAllure = false)` if you do not use
Allure annotations.

```java
@MockartyTest
@Severity(SeverityLevel.CRITICAL)
@Feature("Auth")
class LoginTest {
    @Test
    @Story("Reject bad credentials")
    @Owner("auth-team")
    void shouldReject() { ... }
}
```

### Test discovery (catalogue sync)

Where the result reporter ships per-test *outcomes*, test discovery syncs
the full test *inventory* — every test the launcher collected, including
ones that will not run in this invocation — so the Mockarty TCM catalogue
mirrors the source tree. New tests are created, existing tests keep their
human-authored metadata, and tests removed from code are marked orphaned
(never deleted).

The `mockarty-junit5` module auto-registers a JUnit Platform
`TestExecutionListener` via SPI. It is **off by default** and runs
side-by-side with the result reporter. Enable it for a CI collect-and-sync
step:

```bash
./gradlew test \
  -Dmockarty.discover=true \
  -Dmockarty.discover.source=junit5:auth-suite \
  -DMOCKARTY_BASE_URL=https://mockarty.example.com \
  -DMOCKARTY_API_KEY=mk_... \
  -DMOCKARTY_NAMESPACE=qa
```

| Setting | System property | Env variable | Default |
|---------|-----------------|--------------|---------|
| Enable | `mockarty.discover` | `MOCKARTY_DISCOVER` | off |
| Source (scope key) | `mockarty.discover.source` | `MOCKARTY_DISCOVER_SOURCE` | `junit5` |
| Prune missing | `mockarty.discover.pruneMissing` | `MOCKARTY_DISCOVER_PRUNE` | `true` |

The listener walks the discovered test plan and maps each leaf test:
`fullName` = `Class#method`, `name` = display name, `suite` = the test
class, `sourceRef` = `File.java`, `labels` = JUnit `@Tag`s. A sync failure
is logged and swallowed — discovery never fails the build.

You can also build and sync a manifest manually (handy for non-JUnit test
sources):

```java
DiscoveryResult res = client.discovery().sync("qa",
    new DiscoveryManifest("junit5:auth-suite")
        .framework("junit5")
        .pruneMissing(true)
        .addCase(new DiscoveryManifestCase("com.example.AuthTest#testLogin", "testLogin")
            .suite("AuthTest")
            .sourceRef("AuthTest.java")
            .labels(java.util.List.of("smoke"))));
System.out.println("created=" + res.getCreated() + " orphaned=" + res.getOrphaned());
```

See [`DiscoveryExample.java`](./examples/src/main/java/ru/mockarty/examples/DiscoveryExample.java).

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

### MCP (Model Context Protocol) — mocking an MCP tool

This section is about **mocking** a third-party MCP tool endpoint (making
Mockarty impersonate an MCP server). To **call** Mockarty's own MCP tool
surface as a client, see [MCP Client](#mcp-client) below.

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

## MCP Client

Drive Mockarty's agent-facing tool surface programmatically over the admin
node's Model Context Protocol endpoint — list the tools the server exposes and
call them with typed arguments. Reuses the client's server URL + API key; tool
licensing is enforced server-side. Handshake, session, and JSON/SSE framing are
handled for you.

```java
McpApi mcp = client.mcp();
for (McpApi.McpTool tool : mcp.listTools()) {      // discover available tools
    System.out.println(tool.name + " — " + tool.description);
}
McpApi.McpToolResult result = mcp.callTool("list_mocks", Map.of());
System.out.println(result.text());                  // JSON result text
```

See [`McpClientExample.java`](./examples/src/main/java/ru/mockarty/examples/McpClientExample.java).

## Agent Tasks (submit-and-wait)

Dispatch a free-form task into Mockarty's autonomous agent network and block
for its result:

```java
AgentTask task = client.agentTasks().submitAndWait(
    Map.of("title", "audit", "prompt", "Fuzz the /users API and summarise"),
    Duration.ofSeconds(2));
System.out.println(task.getResult());   // throws MockartyException if it fails/cancels
```

## Issue Tracker (task automation)

Create/read/update/transition issues, comment, search, claim the next issue,
and manage projects/sprints (loosely-typed `JsonNode`/`Map` I/O):

```java
IssueTrackerApi it = client.issueTracker();
JsonNode issue = it.createIssue(null, Map.of("projectId", pid, "type", "bug", "title", "500 on /pay"));
it.addComment(null, issue.get("id").asText(), "repro attached");
it.moveIssue(null, issue.get("id").asText(), "in_progress", null);
JsonNode next = it.nextIssue(null, Map.of("assigneeId", me));
```

## Test Case Management (TCM)

Author cases, run them, poll case-runs, file defects, manage folders +
attachments:

```java
TcmApi tcm = client.tcm();
JsonNode c = tcm.createCase(null, Map.of("folderId", fid, "title", "Checkout smoke"));
JsonNode run = tcm.runCase(null, c.get("id").asText(), null);
JsonNode cr = tcm.getCaseRun(null, run.get("runId").asText());
if ("failed".equals(cr.path("status").asText())) {
    tcm.createDefect(null, Map.of("title", "checkout broke", "caseRunId", run.get("runId").asText()));
}
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

## Protocol Clients

The `mockarty-protocols` module lets a CI test drive the system under
test for **gRPC, Kafka, RabbitMQ, SOAP, GraphQL, SSE, WebSocket**.
Every call records a `Step` (start / end / duration / status / payload
preview) so the TCM external run shows a per-call timeline at the end.

```java
import ru.mockarty.protocols.telemetry.AccumulatingRecorder;
import ru.mockarty.protocols.grpc.GrpcClient;

AccumulatingRecorder rec = new AccumulatingRecorder();
try (GrpcClient grpc = new GrpcClient("service:50051", opts -> opts
        .recorder(rec)
        .protoDescriptorSet(Files.readAllBytes(Path.of("user.desc"))))) {
    Map<String, Object> resp = grpc.invokeJson(
        "acme.UserService/GetUser",
        Map.of("id", "u-42"),
        Map.class);
}
// At test finish, push the captured timeline:
client.externalRuns().report(b -> b.caseName("my case").status("passed").steps(rec.payloads()));
```

Add the module to `build.gradle.kts`:

```kotlin
dependencies {
    api("ru.mockarty:mockarty-protocols:0.1.0")
}
```

Full cross-language reference (Java / Go / Python side-by-side, every
protocol, options, classification rules, troubleshooting):
**[SDK Protocol Clients](https://mockarty.ru/docs/sdk-protocol-clients)**.

## Examples

The [`examples/src/main/java/ru/mockarty/examples/`](./examples/src/main/java/ru/mockarty/examples/)
directory has 30+ runnable programs covering every facet of the SDK.
The most useful starting points:

| Example | What it shows |
|---------|---------------|
| [`KitchenSinkExample.java`](./examples/src/main/java/ru/mockarty/examples/KitchenSinkExample.java) | Full adopter showcase — Tester DSL chain (HTTP → GraphQL → assertions), `wrap()` grouping, ExternalRunBridge upload to TCM. See [`KITCHEN_SINK.md`](./examples/KITCHEN_SINK.md) for the runnable script. |
| [`CiCdPipelineExample.java`](./examples/src/main/java/ru/mockarty/examples/CiCdPipelineExample.java) | JUnit5-driven CI test emitting an ExternalRunRequest from a single step. |
| [`AgentTasksExample.java`](./examples/src/main/java/ru/mockarty/examples/AgentTasksExample.java) | Tester DSL emitting external-run reports from a JUnit5 test method. |
| [`DiscoveryExample.java`](./examples/src/main/java/ru/mockarty/examples/DiscoveryExample.java) | Syncs a test-discovery manifest into TCM via `client.discovery().sync(...)`, and documents the JUnit5 auto-discovery listener switch. |

For protocol-specific code: `HttpMocksExample`, `GraphQLMocksExample`,
`GrpcMocksExample`, `SoapMocksExample`, `MessagingMocksExample`,
`SseMocksExample`.

## Configuration

The client can be configured via builder, environment variables, or system properties:

| Setting | Builder | Env Variable | Default |
|---------|---------|-------------|---------|
| Base URL | `.baseUrl()` | `MOCKARTY_BASE_URL` | `http://localhost:5770` |
| API Key | `.apiKey()` | `MOCKARTY_API_KEY` | (none) |
| Namespace | `.namespace()` | `MOCKARTY_NAMESPACE` | `sandbox` |
| Timeout | `.timeout()` | - | 30 seconds |

## License

This SDK is proprietary software, **not** open source. It is licensed under the
**Mockarty SDK License Agreement** — see [LICENSE](LICENSE) for the full terms.

- **Free** for evaluation, learning, and non-commercial / community use.
- **Commercial use requires a valid, paid Mockarty subscription.** Using this
  SDK in production or for commercial advantage without a subscription is not
  permitted.

For commercial subscriptions and licensing inquiries, see
[mockarty.ru](https://mockarty.ru) or contact orlovich.artem@gmail.com.
