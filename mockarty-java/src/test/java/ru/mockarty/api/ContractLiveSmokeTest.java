package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.Contract;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live wire-parity smoke test for {@link ContractApi} config CRUD against a
 * running admin.
 *
 * <p>Drives createConfig -> listConfigs -> deleteConfig -> gone entirely
 * through the SDK, verifying the {@link Contract} wire shape (id / name /
 * specUrl) round-trips against a real admin. Catches a Java-SDK/admin drift
 * on the contract-config envelope.
 *
 * <p>Gated by {@code TOK} (same convention as {@link FlowRunsLiveSmokeTest});
 * skipped otherwise.
 */
@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class ContractLiveSmokeTest {

    @Test
    void liveContractConfigCrudRoundTrip() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:5770")
                .apiKey(token)
                .namespace("sandbox")
                .build()) {

            String name = "java-sdk-contract-" + System.currentTimeMillis();
            Contract config = new Contract()
                    .name(name)
                    .namespace("sandbox")
                    .protocol("http")
                    .specUrl("http://127.0.0.1:5770/health");

            Contract saved = client.contracts().saveConfig(config);
            assertNotNull(saved.getId(), "createConfig returned no id");
            assertEquals(name, saved.getName(), "createConfig name mismatch");
            String configId = saved.getId();

            try {
                // listConfigs() includes the saved config (decodes the wire shape)
                List<Contract> configs = client.contracts().listConfigs();
                assertTrue(configs.stream().anyMatch(c -> configId.equals(c.getId())),
                        "listConfigs() did not include " + configId);
            } finally {
                client.contracts().deleteConfig(configId);
            }

            // after delete it's gone from the list
            List<Contract> remaining = client.contracts().listConfigs();
            assertFalse(remaining.stream().anyMatch(c -> configId.equals(c.getId())),
                    "config " + configId + " still listed after deleteConfig");
        }
    }
}
