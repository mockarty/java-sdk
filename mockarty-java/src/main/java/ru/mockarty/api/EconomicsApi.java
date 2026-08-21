// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.LLMPrice;
import ru.mockarty.model.LLMPriceList;
import ru.mockarty.model.LLMBudget;
import ru.mockarty.model.LLMBudgetList;
import ru.mockarty.model.LLMUsageReport;
import ru.mockarty.model.LLMUsageRefund;
import ru.mockarty.model.ResourcePrice;
import ru.mockarty.model.ResourcePriceList;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Administrator AI/workflow usage and immutable price-book operations. */
public class EconomicsApi {
    private final MockartyClient client;

    public EconomicsApi(MockartyClient client) { this.client = client; }

    public LLMPriceList listPrices() throws MockartyException {
        return listPrices(null, null, 0);
    }

    public LLMPriceList listPrices(String provider, String model, int limit) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/admin/llm-prices?");
        if (provider != null && !provider.isBlank()) path.append("provider=").append(enc(provider.trim())).append('&');
        if (model != null && !model.isBlank()) path.append("model=").append(enc(model.trim())).append('&');
        if (limit > 0) path.append("limit=").append(limit).append('&');
        if (path.charAt(path.length() - 1) == '?' || path.charAt(path.length() - 1) == '&') {
            path.setLength(path.length() - 1);
        }
        return client.get(path.toString(), LLMPriceList.class);
    }

    public LLMPrice appendPrice(LLMPrice price) throws MockartyException {
        if (price == null || blank(price.getProvider()) || blank(price.getModel()) ||
                blank(price.getCurrency()) || blank(price.getEffectiveFrom())) {
            throw new IllegalArgumentException("provider, model, currency and effective time are required");
        }
        return client.post("/api/v1/admin/llm-prices", price, LLMPrice.class);
    }

    public ResourcePriceList listResourcePrices(String eventKind, String provider, String resource,
                                                String unit, int limit) throws MockartyException {
        validateResourceKindUnit(eventKind, unit, false);
        StringBuilder path = new StringBuilder("/api/v1/admin/llm-prices?eventKind=")
                .append(enc(eventKind.trim()));
        if (!blank(provider)) path.append("&provider=").append(enc(provider.trim()));
        if (!blank(resource)) path.append("&resource=").append(enc(resource.trim()));
        if (!blank(unit)) path.append("&unit=").append(enc(unit.trim()));
        if (limit > 0) path.append("&limit=").append(limit);
        return client.get(path.toString(), ResourcePriceList.class);
    }

    public ResourcePrice appendResourcePrice(ResourcePrice price) throws MockartyException {
        if (price == null || blank(price.getProvider()) || blank(price.getResource()) ||
                blank(price.getCurrency()) || blank(price.getEffectiveFrom()) ||
                price.getProviderMicrosPerUnit() < 0 || price.getCustomerMicrosPerUnit() < 0) {
            throw new IllegalArgumentException(
                    "provider, resource, currency, effective time and non-negative prices are required");
        }
        validateResourceKindUnit(price.getEventKind(), price.getUnit(), true);
        return client.post("/api/v1/admin/llm-prices", price, ResourcePrice.class);
    }

    public LLMUsageReport getUsage() throws MockartyException {
        return getUsage("profile", 30);
    }

    public LLMUsageReport getUsage(String groupBy, int days) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/admin/llm-usage?");
        if (groupBy != null && !groupBy.isBlank()) path.append("groupBy=").append(enc(groupBy)).append('&');
        if (days > 0) path.append("days=").append(days).append('&');
        if (path.charAt(path.length() - 1) == '?' || path.charAt(path.length() - 1) == '&') {
            path.setLength(path.length() - 1);
        }
        return client.get(path.toString(), LLMUsageReport.class);
    }

    public byte[] downloadUsageStatement(String from, String to, String namespace,
                                         String profileId, int limit) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/admin/llm-usage/statement.csv?");
        if (!blank(from)) path.append("from=").append(enc(from.trim())).append('&');
        if (!blank(to)) path.append("to=").append(enc(to.trim())).append('&');
        if (!blank(namespace)) path.append("namespace=").append(enc(namespace.trim())).append('&');
        if (!blank(profileId)) path.append("profileId=").append(enc(profileId.trim())).append('&');
        if (limit > 0) path.append("limit=").append(limit).append('&');
        if (path.charAt(path.length() - 1) == '?' || path.charAt(path.length() - 1) == '&') {
            path.setLength(path.length() - 1);
        }
        return client.getBytes(path.toString());
    }

    public LLMUsageRefund refundUsage(String eventId, String reason) throws MockartyException {
        if (blank(eventId) || reason == null || reason.trim().length() < 3) {
            throw new IllegalArgumentException("event id and refund reason are required");
        }
        return client.post("/api/v1/admin/llm-usage/" + enc(eventId.trim()) + "/refund",
                Map.of("reason", reason.trim()), LLMUsageRefund.class);
    }

    public LLMBudgetList listBudgets(String namespace, boolean active, int limit) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/admin/llm-budgets?");
        if (namespace != null && !namespace.isBlank()) path.append("namespace=").append(enc(namespace.trim())).append('&');
        if (active) path.append("active=true&");
        if (limit > 0) path.append("limit=").append(limit).append('&');
        if (path.charAt(path.length() - 1) == '?' || path.charAt(path.length() - 1) == '&') path.setLength(path.length() - 1);
        return client.get(path.toString(), LLMBudgetList.class);
    }

    public LLMBudget createBudget(LLMBudget budget) throws MockartyException {
        validateBudget(budget, false);
        return client.post("/api/v1/admin/llm-budgets", budget, LLMBudget.class);
    }

    public LLMBudget updateBudget(LLMBudget budget) throws MockartyException {
        validateBudget(budget, true);
        return client.put("/api/v1/admin/llm-budgets/" + enc(budget.getId()), budget, LLMBudget.class);
    }

    private static void validateBudget(LLMBudget budget, boolean requireId) {
        if (budget == null || blank(budget.getNamespace()) || blank(budget.getScopeType()) ||
                blank(budget.getCurrency()) || blank(budget.getPeriodStart()) || blank(budget.getPeriodEnd()) ||
                (requireId && blank(budget.getId()))) {
            throw new IllegalArgumentException("budget namespace, scope, currency, period and id for updates are required");
        }
    }

    private static void validateResourceKindUnit(String eventKind, String unit, boolean requireUnit) {
        String kindValue = eventKind == null ? "" : eventKind.trim();
        String unitValue = unit == null ? "" : unit.trim();
        boolean valid = (!requireUnit || !unitValue.isEmpty()) &&
                (("tool_call".equals(kindValue) && (unitValue.isEmpty() || "calls".equals(unitValue))) ||
                        ("runner_seconds".equals(kindValue) &&
                                (unitValue.isEmpty() || "seconds".equals(unitValue))));
        if (!valid) {
            throw new IllegalArgumentException(
                    "event kind must be tool_call or runner_seconds and unit must match");
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
