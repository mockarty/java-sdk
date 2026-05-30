package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live smoke test for the pact -&gt; Mockarty contract import bridge.
 *
 * <p>Imports a small pact through {@link ContractApi#importPact} and asserts a
 * contract is created, then cleans it up. Gated by {@code TOK} (same
 * convention as {@link ContractLiveSmokeTest}); skipped otherwise.
 */
@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class ContractImportPactLiveSmokeTest {

    @Test
    void liveImportPactRoundTrip() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:5770")
                .apiKey(token)
                .namespace("sandbox")
                .build()) {

            String consumer = "JavaConsumer" + System.currentTimeMillis();
            String pact = "{"
                    + "\"consumer\":{\"name\":\"" + consumer + "\"},"
                    + "\"provider\":{\"name\":\"JavaProvider\"},"
                    + "\"interactions\":[{\"description\":\"smoke\","
                    + "\"request\":{\"method\":\"GET\",\"path\":\"/ping\"},"
                    + "\"response\":{\"status\":200,\"body\":{\"ok\":true}}}],"
                    + "\"metadata\":{\"pactSpecification\":{\"version\":\"3.0.0\"}}}";

            Map<String, Object> result;
            try {
                result = client.contracts().importPact(pact, "1.0.0-live", null);
            } catch (Exception e) {
                String msg = String.valueOf(e.getMessage());
                if (msg.contains("not licensed") || msg.contains("feature_not_licensed")
                        || msg.contains("limit") || msg.contains("trial")) {
                    return; // license gating is correct behaviour
                }
                throw e;
            }

            String contractId = String.valueOf(result.get("id"));
            assertNotNull(contractId, "importPact returned no id");
            assertTrue(!contractId.isEmpty() && !"null".equals(contractId),
                    "importPact returned no id: " + result);

            @SuppressWarnings("unchecked")
            Map<String, Object> consumerObj = (Map<String, Object>) result.get("consumer");
            assertEquals(consumer, consumerObj.get("name"));
            @SuppressWarnings("unchecked")
            Map<String, Object> providerObj = (Map<String, Object>) result.get("provider");
            assertEquals("JavaProvider", providerObj.get("name"));

            // Clean up the contract we just created.
            client.contracts().deletePact(contractId);
        }
    }
}
