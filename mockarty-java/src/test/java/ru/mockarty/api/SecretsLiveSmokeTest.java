package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live wire-parity smoke test for {@link SecretsApi} (store + entry model)
 * against a running admin.
 *
 * <p>The Secrets Storage API is a namespace-scoped store-of-entries, NOT a
 * flat key/value map: createStore -> createEntry -> getEntry (decrypted) ->
 * listEntries (metadata only) -> rotateEntry -> deleteEntry -> deleteStore.
 * This drives that whole lifecycle through the SDK, asserting:
 * <ul>
 *   <li>the store / entry envelopes unwrap correctly,</li>
 *   <li>getEntry returns the decrypted value (round-trip),</li>
 *   <li>listEntries NEVER includes the plaintext value (security invariant),</li>
 *   <li>rotateEntry actually changes the stored value.</li>
 * </ul>
 *
 * <p>Gated by {@code TOK} (same convention as {@link FlowRunsLiveSmokeTest});
 * skipped otherwise.
 */
@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class SecretsLiveSmokeTest {

    @Test
    void liveSecretsStoreAndEntryRoundTrip() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:5770")
                .apiKey(token)
                .namespace("sandbox")
                .build()) {

            String storeName = "java-sdk-store-" + System.currentTimeMillis();
            String key = "API_KEY";
            String secretValue = "p4r1ty-v4lue";
            String rotatedValue = "r0t4ted-v4lue";

            Map<String, Object> store = client.secrets().createStore(storeName, null, "inline");
            Object storeIdObj = store.get("id");
            assertNotNull(storeIdObj, "createStore returned no id");
            String storeId = storeIdObj.toString();
            assertEquals(storeName, store.get("name"), "createStore name mismatch");

            try {
                // create an entry — the response carries metadata, never the value
                Map<String, Object> entry = client.secrets().createEntry(storeId, key, secretValue, null);
                assertEquals(key, entry.get("key"), "createEntry key mismatch");
                assertFalse(entry.containsKey("value"),
                        "SECURITY: createEntry response leaked the value: " + entry);

                // getEntry returns the decrypted value (round-trip)
                Map<String, Object> fetched = client.secrets().getEntry(storeId, key);
                assertEquals(secretValue, fetched.get("value"),
                        "getEntry did not round-trip the value");

                // listEntries is metadata-only — the plaintext value must NOT appear
                List<Map<String, Object>> entries = client.secrets().listEntries(storeId);
                assertTrue(entries.stream().anyMatch(e -> key.equals(e.get("key"))),
                        "listEntries did not include " + key);
                for (Map<String, Object> e : entries) {
                    Object v = e.get("value");
                    assertTrue(v == null || v.toString().isEmpty(),
                            "SECURITY: listEntries leaked a value: " + e);
                }

                // rotateEntry replaces the stored value
                client.secrets().rotateEntry(storeId, key, rotatedValue);
                Map<String, Object> after = client.secrets().getEntry(storeId, key);
                assertEquals(rotatedValue, after.get("value"),
                        "rotateEntry did not change the value");

                // deleteEntry removes it
                client.secrets().deleteEntry(storeId, key);
                List<Map<String, Object>> remaining = client.secrets().listEntries(storeId);
                assertFalse(remaining.stream().anyMatch(e -> key.equals(e.get("key"))),
                        "entry " + key + " still listed after deleteEntry");
            } finally {
                client.secrets().deleteStore(storeId);
            }

            // store is gone from the listing after delete
            List<Map<String, Object>> stores = client.secrets().listStores();
            assertFalse(stores.stream().anyMatch(s -> storeId.equals(String.valueOf(s.get("id")))),
                    "store " + storeId + " still listed after deleteStore");
        }
    }
}
