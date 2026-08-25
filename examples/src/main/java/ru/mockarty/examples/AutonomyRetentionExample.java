package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.AutonomyNamespaceSettings;

/** Configure autonomous-run safety and mission evidence retention. */
public final class AutonomyRetentionExample {
    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(System.getenv("MOCKARTY_BASE_URL"))
                .apiKey(System.getenv("MOCKARTY_API_KEY"))
                .namespace("engineering")
                .build()) {
            AutonomyNamespaceSettings settings = client.namespaceSettings().getAutonomySettings();
            client.namespaceSettings().saveAutonomySettings(settings
                    .journalEventRetentionDays(365)
					.journalPayloadRetentionDays(30)
					.runWindowMinutes(90), "safety-change-2026-08-25");
        }
    }
}
