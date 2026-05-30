package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.FuzzingConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live wire-parity smoke test for {@link FuzzingApi} config CRUD against a
 * running admin.
 *
 * <p>Drives createConfig -> getConfig -> listConfigs -> deleteConfig -> gone
 * entirely through the SDK, verifying the {@link FuzzingConfig} envelope
 * (id / name / targetBaseUrl) round-trips and that {@code listConfigs}
 * unwraps the {@code {configs: [...]}} envelope.
 *
 * <p>Gated by {@code TOK} (same convention as {@link FlowRunsLiveSmokeTest});
 * skipped otherwise.
 */
@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class FuzzingLiveSmokeTest {

    @Test
    void liveFuzzingConfigCrudRoundTrip() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:5770")
                .apiKey(token)
                .namespace("sandbox")
                .build()) {

            String name = "java-sdk-fuzz-" + System.currentTimeMillis();
            String target = "http://127.0.0.1:5770";
            FuzzingConfig config = new FuzzingConfig()
                    .name(name)
                    .namespace("sandbox")
                    .targetBaseUrl(target)
                    .method("GET");

            FuzzingConfig created = client.fuzzing().createConfig(config);
            assertNotNull(created.getId(), "createConfig returned no id");
            assertEquals(name, created.getName(), "createConfig name mismatch");
            assertEquals(target, created.getTargetBaseUrl(),
                    "targetBaseUrl did not round-trip on create");
            String configId = created.getId();

            try {
                // getConfig decodes the single-config wire shape
                FuzzingConfig fetched = client.fuzzing().getConfig(configId);
                assertEquals(configId, fetched.getId(), "getConfig id mismatch");
                assertEquals(target, fetched.getTargetBaseUrl(),
                        "targetBaseUrl did not round-trip on get");

                // listConfigs unwraps the {configs: [...]} envelope
                List<FuzzingConfig> configs = client.fuzzing().listConfigs();
                assertTrue(configs.stream().anyMatch(c -> configId.equals(c.getId())),
                        "listConfigs() did not include " + configId);
            } finally {
                client.fuzzing().deleteConfig(configId);
            }

            // after delete it's gone from the list
            List<FuzzingConfig> remaining = client.fuzzing().listConfigs();
            assertFalse(remaining.stream().anyMatch(c -> configId.equals(c.getId())),
                    "config " + configId + " still listed after deleteConfig");
        }
    }
}
