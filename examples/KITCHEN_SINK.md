# KitchenSinkExample — end-to-end Tester DSL showcase (Java)

Java mirror of the Go SDK's `examples/kitchen_sink` and Python SDK's
`examples/kitchen_sink/main.py`. One executable that exercises every
Tester facet plus the reporting / upstream-tracker side-channels you'd
want in a CI pipeline.

## What it demonstrates

| Step | Facet | What it proves |
|------|-------|----------------|
| 1+2  | `t.http()` | Token chain: GET `/token-chain/issue` → extract → POST `/token-chain/validate` with `{{token}}` interpolation |
| 3    | `t.graphql()` | Query with variables + Bearer header, `expectField` JSONPath |
| 4    | `t.http()` | Every `expect*` kind on a single response |
| 5    | Jira mock | Auto-file a Bug on failure |
| 6    | GitLab mock | Trigger pipeline + poll until success |
| 7    | `client.externalRuns().report(...)` via `ExternalRunBridge` | Upload to Mockarty TCM |
| 8    | Exit code | Non-zero on failure → `set -e` friendly |

## Run it

```bash
# 1. testbackend on 18770 (provides token-chain + Jira/GitLab mocks)
mockarty-testbackend &

# 2. (optional) Mockarty admin on 5770
mockarty &

# 3. Run via Gradle
cd sdk/java-sdk
TESTBACKEND_URL=http://127.0.0.1:18770 \
MOCKARTY_URL=http://127.0.0.1:5770 \
MOCKARTY_API_KEY=mk_... \
MOCKARTY_NAMESPACE=sandbox \
./gradlew :examples:run -PmainClass=ru.mockarty.examples.KitchenSinkExample
```

## Notable differences from Go / Python

- **No `wrap()`** — the Java SDK's Tester doesn't yet expose a step-
  grouping helper. Steps render as a flat list in the Allure report
  rather than nested under a parent. The chains themselves are
  identical.
- **`ExternalRunBridge.toExternalRunRequest(t, opts)`** is the
  conversion seam — same kwargs as Python's `to_report_kwargs(t, ...)`.

## Environment variables

Same matrix as Go / Python — `TESTBACKEND_URL`, `MOCKARTY_URL`,
`MOCKARTY_API_KEY`, `MOCKARTY_NAMESPACE`, `JIRA_PROJECT_KEY`,
`GITLAB_PROJECT_ID`.
