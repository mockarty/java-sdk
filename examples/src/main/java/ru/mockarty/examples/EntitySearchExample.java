// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.EntitySearchRequest;
import ru.mockarty.model.EntitySearchResponse;
import ru.mockarty.model.EntitySearchResult;

/**
 * Demonstrates the unified entity-search API — resolve human-readable names
 * into canonical IDs across mocks, test plans, perf configs, fuzz configs,
 * chaos experiments and contract pacts in one call.
 *
 * <p>Powers the same picker the UI uses, so CI/CD pipelines no longer have
 * to hard-code UUIDs.</p>
 */
public class EntitySearchExample {

    public static void main(String[] args) throws MockartyException {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey(System.getenv().getOrDefault("MOCKARTY_API_KEY", "your-api-key"))
                .namespace(System.getenv().getOrDefault("MOCKARTY_NAMESPACE", "production"))
                .build()) {

            findTestPlans(client);
            paginateMocks(client);
        }
    }

    /** Find every Test Plan whose name contains "smoke" (case-insensitive). */
    static void findTestPlans(MockartyClient client) throws MockartyException {
        EntitySearchResponse plans = client.entitySearch().search(new EntitySearchRequest()
                .type(EntitySearchRequest.TYPE_TEST_PLAN)
                .query("smoke")
                .limit(25));

        System.out.println("Test Plans matching 'smoke' (" + plans.getTotal() + " total):");
        for (EntitySearchResult p : plans.getItems()) {
            String numeric = p.getNumericId() != null ? " (#" + p.getNumericId() + ")" : "";
            System.out.println("  " + p.getName() + numeric
                    + "  ns=" + p.getNamespace()
                    + "  created=" + p.getCreatedAt());
        }
    }

    /**
     * Paginate through every mock in the namespace using the documented
     * default page size. Server caps {@code limit} at {@link
     * EntitySearchRequest#MAX_LIMIT}, so always honour the documented ceiling.
     */
    static void paginateMocks(MockartyClient client) throws MockartyException {
        int offset = 0;
        while (true) {
            EntitySearchResponse page = client.entitySearch().search(new EntitySearchRequest()
                    .type(EntitySearchRequest.TYPE_MOCK)
                    .limit(EntitySearchRequest.DEFAULT_LIMIT)
                    .offset(offset));

            for (EntitySearchResult m : page.getItems()) {
                System.out.println("mock " + m.getName() + "  id=" + m.getId());
            }
            offset += page.getItems().size();
            if (page.getItems().isEmpty() || offset >= page.getTotal()) {
                break;
            }
        }
    }
}
