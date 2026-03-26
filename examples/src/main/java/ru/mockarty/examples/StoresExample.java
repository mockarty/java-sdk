// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.Extract;
import ru.mockarty.model.Mock;

import java.util.Map;

/**
 * Store operations examples covering Global Store, Chain Store, and Mock Store.
 *
 * <p>Stores in Mockarty:</p>
 * <ul>
 *   <li><b>Global Store (gS)</b> - Namespace-scoped global state, shared across all mocks</li>
 *   <li><b>Chain Store (cS)</b> - State shared across mocks linked by a chainId</li>
 *   <li><b>Mock Store (mS)</b> - Ephemeral per-request state, used during a single mock invocation</li>
 * </ul>
 */
public class StoresExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            // Direct store API operations
            globalStoreOperations(client);
            chainStoreOperations(client);

            // Mocks that use stores
            mockWithGlobalStoreExtract(client);
            mockWithChainStore(client);
            mockWithMockStore(client);
            orderWorkflowWithChainStore(client);
            counterWithGlobalStore(client);
        }
    }

    // ---- Direct Store API Operations ----

    /**
     * Global Store: set, get, and delete key-value pairs.
     */
    static void globalStoreOperations(MockartyClient client) {
        // Set values in the global store
        client.stores().globalSet("app.version", "2.1.0");
        client.stores().globalSet("feature.darkMode", true);
        client.stores().globalSet("config.maxRetries", 3);
        client.stores().globalSet("counters.totalRequests", 0);

        // Read the entire global store
        Map<String, Object> store = client.stores().globalGet();
        System.out.println("Global store contents: " + store);

        // Read global store for a specific namespace
        Map<String, Object> prodStore = client.stores().globalGet("production");
        System.out.println("Production global store: " + prodStore);

        // Set values in another namespace
        client.stores().globalSet("production", "deployment.status", "stable");

        // Delete a key
        client.stores().globalDelete("feature.darkMode");

        System.out.println("Global store operations completed");
    }

    /**
     * Chain Store: set, get, and delete values for a specific chain.
     */
    static void chainStoreOperations(MockartyClient client) {
        String chainId = "order-flow-001";

        // Set chain store values
        client.stores().chainSet(chainId, "orderId", "ORD-12345");
        client.stores().chainSet(chainId, "userId", "USR-67890");
        client.stores().chainSet(chainId, "step", "created");
        client.stores().chainSet(chainId, "totalAmount", 149.99);

        // Read the chain store
        Map<String, Object> chainStore = client.stores().chainGet(chainId);
        System.out.println("Chain store for " + chainId + ": " + chainStore);

        // Read chain store from a specific namespace
        Map<String, Object> prodChain = client.stores().chainGet(chainId, "production");
        System.out.println("Production chain store: " + prodChain);

        // Update a value
        client.stores().chainSet(chainId, "step", "processing");

        // Delete a key
        client.stores().chainDelete(chainId, "totalAmount");

        System.out.println("Chain store operations completed");
    }

    // ---- Mocks That Use Stores ----

    /**
     * Mock that extracts request data into the Global Store.
     * Every time this mock is called, it saves the user's IP to the global store.
     */
    static void mockWithGlobalStoreExtract(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/track/visit", "POST")
                .id("store-global-extract")
                .extract(new Extract()
                        .gStore(Map.of(
                                "lastVisitor.ip", "$.reqHeader.X-Forwarded-For[0]",
                                "lastVisitor.userAgent", "$.reqHeader.User-Agent[0]",
                                "lastVisitor.timestamp", "$.fake.DateISO"
                        ))
                )
                .respond(200, Map.of(
                        "tracked", true,
                        "serverTime", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with Global Store extraction");
    }

    /**
     * Mock that references Global Store values in its response.
     * Returns the app version and config stored in the global store.
     */
    static void counterWithGlobalStore(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/system/info", "GET")
                .id("store-global-read")
                .respond(200, Map.of(
                        "version", "$.gS.app.version",
                        "darkMode", "$.gS.feature.darkMode",
                        "maxRetries", "$.gS.config.maxRetries",
                        "totalRequests", "$.gS.counters.totalRequests"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock that reads from Global Store");
    }

    /**
     * Chain store workflow: order creation -> payment -> shipping.
     * Each step saves and reads from the chain store.
     */
    static void orderWorkflowWithChainStore(MockartyClient client) {
        String chainId = "order-workflow";

        // Step 1: Create order - extracts orderId into chain store
        Mock createOrder = MockBuilder.http("/api/orders", "POST")
                .id("chain-order-create")
                .chainId(chainId)
                .condition("items", AssertAction.NOT_EMPTY, null)
                .extract(new Extract()
                        .cStore(Map.of(
                                "orderId", "$.fake.UUID",
                                "userId", "$.req.userId",
                                "totalAmount", "$.req.totalAmount",
                                "status", "created"
                        ))
                )
                .respond(201, Map.of(
                        "orderId", "$.cS.orderId",
                        "status", "created",
                        "message", "Order created successfully"
                ))
                .build();

        // Step 2: Process payment - reads orderId from chain store, updates status
        Mock processPayment = MockBuilder.http("/api/orders/:id/pay", "POST")
                .id("chain-order-pay")
                .chainId(chainId)
                .extract(new Extract()
                        .cStore(Map.of(
                                "status", "paid",
                                "paymentId", "$.fake.UUID",
                                "paidAt", "$.fake.DateISO"
                        ))
                )
                .respond(200, Map.of(
                        "orderId", "$.cS.orderId",
                        "paymentId", "$.cS.paymentId",
                        "status", "paid",
                        "totalAmount", "$.cS.totalAmount"
                ))
                .build();

        // Step 3: Ship order - reads orderId and payment status from chain store
        Mock shipOrder = MockBuilder.http("/api/orders/:id/ship", "POST")
                .id("chain-order-ship")
                .chainId(chainId)
                .extract(new Extract()
                        .cStore(Map.of(
                                "status", "shipped",
                                "trackingNumber", "$.fake.UUID",
                                "shippedAt", "$.fake.DateISO"
                        ))
                )
                .respond(200, Map.of(
                        "orderId", "$.cS.orderId",
                        "status", "shipped",
                        "trackingNumber", "$.cS.trackingNumber",
                        "paymentId", "$.cS.paymentId",
                        "estimatedDelivery", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(createOrder);
        client.mocks().create(processPayment);
        client.mocks().create(shipOrder);
        System.out.println("Created order workflow mocks with Chain Store");
    }

    /**
     * Mock that uses Chain Store to link related requests.
     * The chain store persists across mock calls with the same chainId.
     */
    static void mockWithChainStore(MockartyClient client) {
        String chainId = "auth-session";

        // Login mock - stores session data
        Mock login = MockBuilder.http("/api/auth/login", "POST")
                .id("chain-auth-login")
                .chainId(chainId)
                .condition("username", AssertAction.NOT_EMPTY, null)
                .extract(new Extract()
                        .cStore(Map.of(
                                "sessionId", "$.fake.UUID",
                                "username", "$.req.username",
                                "loginAt", "$.fake.DateISO"
                        ))
                )
                .respond(200, Map.of(
                        "sessionId", "$.cS.sessionId",
                        "token", "$.fake.UUID",
                        "expiresIn", 3600
                ))
                .build();

        // Profile mock - reads from chain store
        Mock profile = MockBuilder.http("/api/auth/profile", "GET")
                .id("chain-auth-profile")
                .chainId(chainId)
                .respond(200, Map.of(
                        "sessionId", "$.cS.sessionId",
                        "username", "$.cS.username",
                        "loginAt", "$.cS.loginAt",
                        "email", "$.fake.Email"
                ))
                .build();

        client.mocks().create(login);
        client.mocks().create(profile);
        System.out.println("Created auth chain store mocks");
    }

    /**
     * Mock Store: ephemeral per-request state.
     * Useful for computing intermediate values within a single mock evaluation.
     */
    static void mockWithMockStore(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/compute/price", "POST")
                .id("store-mock-compute")
                .mockStore(Map.of(
                        "taxRate", 0.21,
                        "discountPercent", 10,
                        "currency", "EUR"
                ))
                .respond(200, Map.of(
                        "basePrice", "$.req.price",
                        "taxRate", "$.mS.taxRate",
                        "discount", "$.mS.discountPercent",
                        "currency", "$.mS.currency",
                        "message", "Price computed with mock store values"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with Mock Store");
    }
}
