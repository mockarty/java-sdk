package ru.mockarty.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.MockartyClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Dispatch one Shared run with an msa_ credential. */
public final class CloudServiceAccountRunsExample {
    private CloudServiceAccountRunsExample() { }

    public static void main(String[] args) throws Exception {
        try (var client = MockartyClient.builder().baseUrl(System.getenv("MOCKARTY_BASE_URL"))
                .apiKey(System.getenv("MOCKARTY_SERVICE_ACCOUNT_CREDENTIAL")).build()) {
            var payload = new ObjectMapper().createObjectNode().put("suite", "smoke");
            var run = client.cloudServiceAccount().dispatchRun(
                    System.getenv("MOCKARTY_SPACE_ID"), Instant.now().plus(10, ChronoUnit.MINUTES).toString(),
                    "execute", "batch", System.getenv("MOCKARTY_PROJECT_ID"), payload,
                    System.getenv("MOCKARTY_IDEMPOTENCY_KEY"));
            System.out.println(run.getId() + " " + run.getState() + " " + run.getRevision());
        }
    }
}
