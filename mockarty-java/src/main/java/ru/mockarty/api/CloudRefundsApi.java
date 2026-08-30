package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Operator-only durable Cloud refund recovery API.
 *
 * <p>The browser-session {@code /api/v1/cloud/billing/refunds} endpoint is
 * deliberately absent: creating a refund is an interactive, action-bound
 * step-up operation.</p>
 */
public class CloudRefundsApi {
    private static final Pattern REASON_CODE = Pattern.compile("[a-z0-9._:-]{2,64}");
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._:/@-]{1,128}");
    private static final String BASE = "/api/v1/cloud/operator/refunds/";
    private static final String PAYMENTS = "/api/v1/cloud/operator/payments";
    private final MockartyClient client;

    public CloudRefundsApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists the redacted actionable refund projection. The caller needs the
     * exact {@code operator:commerce:write} token scope or an interactive
     * operator session. Payment records in the shared envelope are ignored.
     */
    public List<JsonNode> listRefunds() throws MockartyException {
        JsonNode response = client.get(PAYMENTS, JsonNode.class);
        JsonNode refunds = response == null ? null : response.get("refunds");
        if (refunds == null || !refunds.isArray()) {
            throw new MockartyException("operator payments response is missing refunds");
        }
        List<JsonNode> result = new ArrayList<>();
        for (JsonNode refund : refunds) {
            if (!validRefund(refund)) {
                throw new MockartyException("operator payments response contains an invalid refund projection");
            }
            result.add(refund);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Rejects an operator-required refund or reopens provider-backed recovery.
     * This operation cannot manufacture a successful monetary outcome.
     */
    public JsonNode resolveRefund(String operationId, String action, String reasonCode,
                                  long generation, String idempotencyKey) throws MockartyException {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operation id is required");
        }
        if (!"reject".equals(action) && !"retry".equals(action)) {
            throw new IllegalArgumentException("action must be reject or retry");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        if (reasonCode == null || !REASON_CODE.matcher(reasonCode).matches()) {
            throw new IllegalArgumentException("reason code must contain 2-64 safe lowercase characters");
        }
        // Never trim or normalise this replay authority: the exact caller value
        // is forwarded in Idempotency-Key.
        if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new IllegalArgumentException("idempotency key must contain 1-128 safe characters");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", action);
        body.put("reason_code", reasonCode);
        body.put("generation", generation);
        return client.postWithHeaders(BASE + encode(operationId) + "/resolve", body, JsonNode.class,
                Map.of("Idempotency-Key", idempotencyKey));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static boolean validRefund(JsonNode refund) {
        return refund != null && refund.isObject()
                && refund.path("operation_id").isTextual() && !refund.path("operation_id").asText().isEmpty()
                && refund.path("generation").isIntegralNumber() && refund.path("generation").asLong() >= 0
                && refund.path("status").isTextual() && !refund.path("status").asText().isEmpty()
                && refund.path("amount_minor").isIntegralNumber() && refund.path("amount_minor").asLong() >= 0
                && refund.path("currency").isTextual() && !refund.path("currency").asText().isEmpty()
                && refund.path("provider").isTextual() && !refund.path("provider").asText().isEmpty();
    }
}
