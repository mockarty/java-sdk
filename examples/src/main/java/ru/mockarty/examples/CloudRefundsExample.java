package ru.mockarty.examples;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;

public final class CloudRefundsExample {
    private CloudRefundsExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create(
                System.getenv("MOCKARTY_BASE_URL"), System.getenv("MOCKARTY_API_KEY"))) {
            // The API token needs the exact operator:commerce:write scope.
            String operationId = System.getenv("REFUND_OPERATION_ID");
            if (operationId == null || operationId.isBlank()) {
                throw new IllegalArgumentException("REFUND_OPERATION_ID is required");
            }
            JsonNode selected = client.cloudRefunds().listRefunds().stream()
                    .filter(refund -> operationId.equals(refund.path("operation_id").asText()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("REFUND_OPERATION_ID is not actionable"));
            JsonNode resolution = client.cloudRefunds().resolveRefund(
                    selected.path("operation_id").asText(), "retry", "provider_recovery_retry",
                    selected.path("generation").asLong(), System.getenv("REFUND_IDEMPOTENCY_KEY"));
            System.out.println(resolution.path("refund").path("status").asText());
        }
    }
}
