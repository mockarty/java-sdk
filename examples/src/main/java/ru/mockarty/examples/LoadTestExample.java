// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.builder.LoadTestBuilder;

import java.util.Map;

/**
 * Example: describe a load test with the LoadTestBuilder DSL.
 *
 * <p>The builder emits either a k6-compatible script or a perf-config JSON
 * carrying the full load profile (stages/thresholds/env). It does not run
 * anything itself — a thin wrapper around the existing perf engine.</p>
 *
 * <p>Run the generated config locally with the CLI:</p>
 * <pre>{@code mockarty-cli perf run --from-config checkout.json}</pre>
 */
public final class LoadTestExample {

    private LoadTestExample() {
    }

    public static void main(String[] args) throws Exception {
        LoadTestBuilder profile = LoadTestBuilder.named("checkout-load")
                .target("http://127.0.0.1:8080")
                .get("/health")
                .post("/cart", Map.of("sku", "abc", "qty", 2))
                .stage("30s", 50)   // ramp to 50 VUs
                .stage("1m", 50)    // hold 50 VUs
                .stage("10s", 0)    // ramp down
                .threshold("http_req_duration", "p(95)<800")
                .threshold("http_req_failed", "rate<0.01")
                .thinkTime(0.5);

        // 1) Inspect the generated k6 script.
        System.out.println("--- k6 script ---");
        System.out.println(profile.toK6Script());

        // 2) Save a perf-config and run it locally with the CLI:
        //      mockarty-cli perf run --from-config checkout.json
        profile.save("checkout.json");
        System.out.println("wrote checkout.json — run it with:");
        System.out.println("  mockarty-cli perf run --from-config checkout.json");

        // 3) (Optional) submit the same config to a Mockarty server via the SDK.
    }
}
