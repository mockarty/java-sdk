# mockarty-fuzz

Language-side fuzz target DSL for Mockarty. Describe what to fuzz in
idiomatic Java; the module transpiles your description to Mockarty's
canonical fuzz JSON config and hands it to either the running admin
server or a local `mockarty-cli` subprocess.

This is a **thin** SDK layer: there is NO embedded fuzz engine, NO Java
mutators, NO native detectors. Mockarty's existing fuzz runtime
(`internal/fuzzing/`) does the actual work — server-side or in the CLI.
Java just speaks the schema.

## Quick start

```java
import ru.mockarty.fuzz.*;
import java.time.Duration;

Target target = Target.named("login-flow")
    .description("Stress-test login endpoint")
    .httpEndpoint("POST", "/api/v1/login")
    .seeds(
        Seed.of("valid",      "{\"username\":\"admin\",\"password\":\"x\"}"),
        Seed.of("missing-pw", "{\"username\":\"admin\"}"))
    .mutator(Mutator.JSON)
    .duration(Duration.ofMinutes(5))
    .stopOnFinding(true)
    .reporter(Reporter.ALLURE)
    .assertion(Assertion.statusInRange(200, 299))
    .assertion(Assertion.noErrorInBody("stack trace", "SQLSTATE"))
    .build();

try (Runner runner = Runner.of(
        "https://mockarty.example.com", "default", "tok-abc")) {
    JobId job = runner.submit(target);
    Result result = runner.waitFor(job);
    System.out.println("findings: " + result.totalFindings());
    result.findings().forEach(System.out::println);
}
```

## Three paths, one schema

| Path                       | When to use                                      | How                                                    |
|----------------------------|--------------------------------------------------|--------------------------------------------------------|
| **In-code submit**         | CI/CD, integration tests w/ a live admin         | `runner.submit(target)` + `waitFor` / `stream`         |
| **JSON file → CLI**        | Air-gapped runners, version-pinned configs       | `target.writeTo("login.json")` → `mockarty-cli fuzz run login.json` |
| **Local subprocess**       | Offline iteration, IDE-only loops, no admin      | `runner.localSpawn(target)`                            |

All three converge on the same JSON config (schema parity with
`internal/fuzzing/config.go FuzzConfig` + `FuzzOptions`).

## JUnit5 fixture

```java
@MockartyFuzz(adminUrl = "https://mockarty.example.com",
              namespace = "default",
              apiToken = "tok-abc")
class LoginFuzzTest {

    @FuzzBuilder
    static Target target() {
        return Target.named("login")
            .httpEndpoint("POST", "/api/v1/login")
            .seeds(Seed.of("valid", "{}"))
            .mutator(Mutator.JSON)
            .build();
    }

    @Test
    void noNewVulnerabilities(Runner runner, Target target) throws Exception {
        Result r = runner.waitFor(runner.submit(target));
        assertEquals(0, r.criticalFindings());
        assertEquals(0, r.highFindings());
    }
}
```

No `@ExtendWith` needed — the extension auto-registers via
`META-INF/services`.

## Surface

| Type                  | Role                                                          |
|-----------------------|---------------------------------------------------------------|
| `Target`              | Immutable fuzz target. Build with `Target.named(...)`.        |
| `Target.Builder`      | Fluent builder. Mutable, not thread-safe.                     |
| `Seed`                | One corpus entry. Text, bytes, or file-on-disk.               |
| `Mutator`             | JSON / XML / BYTES / STRING / URL / HEADER / GRPC / GRAPHQL.  |
| `Mutator.custom(...)` | User-defined mutator (registered server-side, resolved by name). |
| `Assertion`           | Sealed: `Status`, `NoCrash`, `ResponseTimeUnder`, `NoErrorInBody`. |
| `Reporter`            | `NONE`, `ALLURE`, `JUNIT`, `SARIF`, `JSON`.                   |
| `Protocol`            | `HTTP` / `GRAPHQL` / `GRPC` — implied by the endpoint builder. |
| `Transpiler`          | `toJson` / `toPrettyJson` / `toMap`.                          |
| `Runner`              | `submit` / `waitFor` / `stream` / `stop` / `localSpawn`.      |
| `Result`              | Final status + findings counts + finding list.                |
| `Finding`             | Single discovered issue (id, severity, category, request).    |
| `Event`               | Sealed: `Progress`, `FindingFound`, `Completed`.              |
| `JobId`               | Opaque run identifier.                                        |

## Phase 2 backlog

- Kotlin DSL wrapper (`mockarty-kotlin` module gets a `fuzz {}` block).
- Distributed fuzz fan-out across runner pools (target → sharded
  seed corpora, one runner per shard).
- Inline custom JavaScript mutators (define mutator body in Java, ship
  script alongside JSON config).
- AFL-style coverage feedback streaming (engine-side instrumentation,
  SDK surfaces `Event.Coverage` deltas).
- OpenAPI / Swagger import → auto-built corpora.
- Allure step emission during streaming (`runner.stream(job)` → Allure
  attachment per finding).

## License

MIT — see `../LICENSE`.
