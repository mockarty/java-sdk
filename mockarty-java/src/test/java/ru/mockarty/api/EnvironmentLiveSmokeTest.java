package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.Environment;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live wire-parity smoke test for {@link EnvironmentApi} against a running
 * admin.
 *
 * <p>The other environment tests use hand-written expected responses — they
 * verify the SDK's own decoding, not that it matches the real admin's wire
 * shape. This drives create -> get -> list -> update -> delete -> gone fully
 * through the SDK, so a drift between the Java SDK and the admin's actual
 * JSON fails here. Note the wire shape: {@code variables} is a flat
 * {@code {name: value}} MAP (not a list of objects).
 *
 * <p>Gated by {@code TOK} (same convention as {@link FlowRunsLiveSmokeTest});
 * skipped otherwise so the offline suite stays self-contained.
 */
@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class EnvironmentLiveSmokeTest {

    @Test
    void liveEnvironmentCrudRoundTrip() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:5770")
                .apiKey(token)
                .namespace("sandbox")
                .build()) {

            String name = "java-sdk-env-" + System.currentTimeMillis();
            Environment env = new Environment()
                    .name(name)
                    .namespace("sandbox")
                    .variables(Map.of("BASE_URL", "http://example.test"));

            Environment created = client.environments().create(env);
            assertNotNull(created.getId(), "create returned no id");
            assertEquals(name, created.getName(), "create name mismatch");
            assertNotNull(created.getVariables(), "variables map missing on create");
            assertEquals("http://example.test", created.getVariables().get("BASE_URL"),
                    "variables map did not round-trip on create");
            String envId = created.getId();

            try {
                // get returns the flat environment object (wire shape decodes)
                Environment fetched = client.environments().get(envId);
                assertEquals(envId, fetched.getId(), "get id mismatch");

                // list includes the created environment
                List<Environment> envs = client.environments().list();
                assertTrue(envs.stream().anyMatch(e -> envId.equals(e.getId())),
                        "list() did not include " + envId);

                // update mutates the variables map and round-trips
                Environment updated = client.environments().update(envId, new Environment()
                        .name(name)
                        .namespace("sandbox")
                        .variables(Map.of("BASE_URL", "http://changed.test", "TOKEN", "xyz")));
                Map<String, String> uvars = updated.getVariables();
                assertNotNull(uvars, "update returned no variables");
                assertEquals("http://changed.test", uvars.get("BASE_URL"),
                        "update did not change BASE_URL");
                assertEquals("xyz", uvars.get("TOKEN"), "update did not add TOKEN");
            } finally {
                client.environments().delete(envId);
            }

            // gone from the list after delete
            List<Environment> remaining = client.environments().list();
            assertFalse(remaining.stream().anyMatch(e -> envId.equals(e.getId())),
                    "environment " + envId + " still listed after delete");
        }
    }
}
