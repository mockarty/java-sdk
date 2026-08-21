package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.LLMUsageReport;

public final class EconomicsExample {
    private EconomicsExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create()) {
            LLMUsageReport report = client.economics().getUsage("profile", 30);
            System.out.printf("calls=%d tokens=%d unpriced=%d%n",
                    report.getTotals().getCalls(), report.getTotals().getTotalTokens(), report.getUnpricedCalls());
            byte[] statement = client.economics().downloadUsageStatement(null, null, null, null, 100);
            System.out.printf("statement_bytes=%d%n", statement.length);
            int toolPrices = client.economics().listResourcePrices(
                    "tool_call", null, null, "calls", 100).getResourcePrices().size();
            System.out.printf("tool_price_entries=%d%n", toolPrices);
        }
    }
}
