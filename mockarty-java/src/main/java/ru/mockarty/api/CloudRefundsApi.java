package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
    private final MockartyClient client;

    public CloudRefundsApi(MockartyClient client) {
        this.client = client;
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
}
