package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

public final class CloudRiskExample {
    private CloudRiskExample() {}

    public static void main(String[] args) throws Exception {
		try (MockartyClient client = MockartyClient.create(System.getenv("MOCKARTY_BASE_URL"), System.getenv("MOCKARTY_API_KEY"))) {
			client.cloudRisk().listCases("open", 50).forEach(item ->
					System.out.println(item.path("id").asText() + " " + item.path("reason_code").asText()));
			// releaseEnforcement derives a stable idempotency key for safe exact retries.
		}
    }
}
