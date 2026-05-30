package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.ChaosProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live wire-parity smoke test for {@link ChaosApi} profile CRUD against a
 * running admin.
 *
 * <p>Drives createProfile -> listProfiles -> deleteProfile -> gone entirely
 * through the SDK, verifying the {@link ChaosProfile} object decodes on
 * create and the {@code {profiles: [...]}} list envelope round-trips.
 *
 * <p>Gated by {@code TOK} (same convention as {@link FlowRunsLiveSmokeTest});
 * skipped otherwise.
 */
@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class ChaosLiveSmokeTest {

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> profilesOf(Map<String, Object> env) {
        Object raw = env == null ? null : env.get("profiles");
        return raw instanceof List ? (List<Map<String, Object>>) raw : Collections.emptyList();
    }

    @Test
    void liveChaosProfileCrudRoundTrip() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:5770")
                .apiKey(token)
                .namespace("sandbox")
                .build()) {

            String name = "java-sdk-chaos-" + System.currentTimeMillis();
            ChaosProfile created = client.chaos().createProfile(new ChaosProfile()
                    .name(name)
                    .namespaceId("sandbox"));
            assertNotNull(created.getId(), "createProfile returned no id");
            assertEquals(name, created.getName(), "createProfile name mismatch");
            String profileId = created.getId();

            try {
                // listProfiles() returns the {profiles: [...], count} envelope —
                // the created profile is present.
                List<Map<String, Object>> profiles = profilesOf(client.chaos().listProfiles());
                assertTrue(profiles.stream().anyMatch(p -> profileId.equals(String.valueOf(p.get("id")))),
                        "listProfiles() did not include " + profileId);
            } finally {
                client.chaos().deleteProfile(profileId);
            }

            // after delete it's gone from the list
            List<Map<String, Object>> remaining = profilesOf(client.chaos().listProfiles());
            assertFalse(remaining.stream().anyMatch(p -> profileId.equals(String.valueOf(p.get("id")))),
                    "profile " + profileId + " still listed after deleteProfile");
        }
    }
}
