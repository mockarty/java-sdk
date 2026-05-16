# mockarty-pact

Pact V3 + V4 consumer DSL for the [Mockarty](https://mockarty.ru) Java SDK.

Pure JVM — **no Rust FFI**, no `pact-jvm` dependency, no native binary on
your classpath. The DSL emits Pact-compatible `pact.json` so any
broker / provider verifier in the Pact ecosystem can consume the contract.

Part of [`mockarty/java-sdk`](https://github.com/mockarty/java-sdk) — see the
root [README](../README.md) for SDK-wide overview.

## Why a Mockarty Pact module?

The reference Pact-JVM consumer library calls into a Rust FFI
(`libpact_ffi`) which:

- ships per-platform `.dylib`/`.so`/`.dll` binaries you have to manage;
- has no air-gapped install story;
- forces a CGO-style native dependency on every consumer test JVM.

Mockarty deploys into regulated, air-gapped environments where a
side-channel native library is a non-starter. So we wrote a pure-Java
Pact serialiser. We deliberately *do not* reimplement the verifier side
(Mockarty admin or external Pact tools handle that) — this module is the
thin SDK-side surface only.

## Quick start

```java
import ru.mockarty.pact.Consumer;
import ru.mockarty.pact.Matchers;
import ru.mockarty.pact.MockServer;
import ru.mockarty.pact.Pact;
import ru.mockarty.pact.SpecVersion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;

Pact pact = Consumer.named("OrderService")
        .withProvider("PaymentService")
        .specVersion(SpecVersion.V4)
        .outputDir(Path.of("./pacts"))
        .addInteraction(it -> it
            .given("payment service is up")
            .uponReceiving("a charge request")
            .withRequest("POST", "/charge")
            .withHeader("Content-Type", "application/json")
            .withJsonBody(Map.of("amount", Matchers.like(100)))
            .willRespondWith(200)
            .withJsonBody(Map.of("id", Matchers.like("abc"))))
        .build();

try (MockServer mock = MockServer.start(pact)) {
    HttpResponse<String> resp = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(URI.create(mock.uri() + "/charge"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"amount\":100}"))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    // assert against resp …
    mock.verify();
}
// pact.json was written to ./pacts/orderservice-paymentservice.json
```

## JUnit 5 integration

Add the `@PactConsumer` annotation on the test class and a static method
annotated `@PactBuilder` returning a `Consumer` (or `Pact`). The
extension is auto-detected via JUnit's `Extension` SPI; you don't need
`@ExtendWith`.

```java
@PactConsumer(name = "OrderService", provider = "PaymentService",
              specVersion = SpecVersion.V4)
class PaymentClientTest {

    @PactBuilder
    static Consumer pact() {
        return Consumer.named("OrderService")
            .addInteraction(it -> it
                .uponReceiving("a charge request")
                .withRequest("POST", "/charge")
                .withJsonBody(Map.of("amount", Matchers.like(100)))
                .willRespondWith(200)
                .withJsonBody(Map.of("id", Matchers.like("abc"))));
    }

    @Test
    void chargesPayment(MockServer server) {
        // server.uri() points at an ephemeral 127.0.0.1:<port>
        // your real client code talks to it
    }
}
```

On Gradle, enable extension auto-detection (this module's own build
already does it):

```kotlin
tasks.withType<Test> {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}
```

## V3 vs V4

The `SpecVersion` enum on the `Consumer` builder picks the wire format.
Default is **V4**.

| Feature                                 | V3      | V4      |
|----------------------------------------|---------|---------|
| Flat `matchingRules` keyed by JSONPath  | yes     | -       |
| Category-split rules (body/header/...)  | -       | yes     |
| `providerState` (string)                | yes     | -       |
| `providerStates` (array, with `params`) | -       | yes     |
| Interaction `type` discriminator        | -       | yes     |
| Binary bodies                           | -       | yes     |
| Plugin declarations (metadata)          | -       | yes     |
| `matchType`, `minType`, `maxType`,
  `minMaxType`, `arrayContains`,
  `equality`, `eachKey`, `eachValue`     | -       | yes     |

Mismatches surface as `IllegalStateException` at build / serialise time
— never silent downgrades. Using a V4-only matcher under V3 fails loud,
as does passing provider-state params under V3.

## Matcher reference

V3 + V4 shared (all under `ru.mockarty.pact.Matchers`):

- `like(example)` — same JSON type as `example`.
- `term(regex, example)` — regex match.
- `eachLike(example)` / `eachLike(example, min)` — array of like-shaped elements.
- `eachKeyLike(example)` — legacy V3 map-key matcher.
- `regex(pattern, example)` — explicit regex with its own example.
- `integer(example)` / `decimal(example)` / `bool(example)` — primitive type matchers.

V4-only:

- `matchType(example)` — strict type-only match.
- `minType(example, min)` / `maxType(example, max)` / `minMaxType(example, min, max)` — bounded arrays.
- `arrayContains(variant…)` — every variant must appear somewhere.
- `equality(example)` — strict deep-equality escape hatch.
- `eachKey(example, rule…)` / `eachValue(example, rule…)` — apply rules per map key/value.

## Phase-1 limitations & Phase-2 follow-ups

| Limitation                                | Status    | Tracked in                  |
|-------------------------------------------|-----------|-----------------------------|
| Plugins recorded in metadata only         | stub      | Phase 2 (plugin runtime)    |
| V4 generators (`$random`, `$date`)        | not yet   | Phase 2 (generators block)  |
| Async / Message Pact                      | not yet   | Phase 2 (Synchronous/Message) |
| Kotlin DSL wrapper                        | not yet   | Phase 2 (mockarty-kotlin)   |
| Provider-side verification helpers        | out of scope | Server-side / external Pact tools |

The Phase-1 surface intentionally tracks the consumer-contract author
workflow: design + emit + replay locally + write file. Verification
runs against Mockarty admin (which knows how to read pact.json) or
your existing pact-broker integration.

## License

MIT — see [LICENSE](../LICENSE).
