package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live wire-parity smoke test for {@link CollectionApi} against a running
 * admin.
 *
 * <p>Drives create -> get -> list -> delete -> gone entirely through the SDK,
 * verifying the api-tester collection envelope (id / name / protocol)
 * round-trips against a real admin.
 *
 * <p>Gated by {@code TOK} (same convention as {@link FlowRunsLiveSmokeTest});
 * skipped otherwise.
 */
@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class CollectionLiveSmokeTest {

    @Test
    void liveCollectionCrudRoundTrip() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:5770")
                .apiKey(token)
                .namespace("sandbox")
                .build()) {

            String name = "java-sdk-coll-" + System.currentTimeMillis();
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("namespace", "sandbox");
            body.put("protocol", "http");

            Map<String, Object> created = client.collections().create(body);
            Object idObj = created.get("id");
            assertNotNull(idObj, "create returned no id");
            String collId = idObj.toString();
            assertEquals(name, created.get("name"), "create name mismatch");
            assertEquals("http", created.get("protocol"), "create protocol mismatch");

            try {
                // get(id) decodes the real wire shape
                Map<String, Object> got = client.collections().get(collId);
                assertEquals(collId, String.valueOf(got.get("id")), "get id mismatch");
                assertEquals(name, got.get("name"), "get name mismatch");

                // list() includes the created collection
                List<Map<String, Object>> listed = client.collections().list();
                assertTrue(listed.stream().anyMatch(c -> collId.equals(String.valueOf(c.get("id")))),
                        "list() did not include " + collId);
            } finally {
                client.collections().delete(collId);
            }

            // after delete it's gone from the list
            List<Map<String, Object>> remaining = client.collections().list();
            assertFalse(remaining.stream().anyMatch(c -> collId.equals(String.valueOf(c.get("id")))),
                    "collection " + collId + " still listed after delete");
        }
    }
}
