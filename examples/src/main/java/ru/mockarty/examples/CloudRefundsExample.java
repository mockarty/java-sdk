package ru.mockarty.examples;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;

public final class CloudRefundsExample {
    private CloudRefundsExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create(
                System.getenv("MOCKARTY_BASE_URL"), System.getenv("MOCKARTY_API_KEY"))) {
            JsonNode resolution = client.cloudRefunds().resolveRefund(
                    System.getenv("REFUND_OPERATION_ID"), "retry", "provider_recovery_retry",
                    4, "refund-resolution:example-1");
            System.out.println(resolution.path("refund").path("status").asText());
        }
    }
}
