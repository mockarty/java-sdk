// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.MissionEffectiveSettingsOptions;
import ru.mockarty.model.MissionStartRequest;
import ru.mockarty.model.MissionCancelRequest;

public final class AutonomousMissionsExample {
    private AutonomousMissionsExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(System.getenv("MOCKARTY_BASE_URL"))
                .apiKey(System.getenv("MOCKARTY_API_KEY"))
                .namespace(System.getenv().getOrDefault("MOCKARTY_NAMESPACE", "sandbox"))
                .build()) {
            String productId = System.getenv().getOrDefault("MOCKARTY_PRODUCT_ID", "");
            var settings = client.autonomousMissions().getEffectiveSettings(
                    new MissionEffectiveSettingsOptions().productId(productId));
            var started = client.autonomousMissions().start(new MissionStartRequest()
                    .goal("Take the checkout release to production quality and provide evidence")
                    .productId(productId)
                    .autonomy("auto")
                    .budget(100000, 0, 0)
                    .expectedSettingsDigest(settings.getSettingsDigest()));
            System.out.printf("mission=%s status=%s created=%s%n",
                    started.getMission().getId(), started.getMission().getStatus(), started.isCreated());
            if ("1".equals(System.getenv("MOCKARTY_EXAMPLE_CANCEL"))) {
                var cancelled = client.autonomousMissions().cancel(started.getMission().getId(),
                        new MissionCancelRequest().reason("example run no longer needed")
                                .idempotencyKey("autonomous-missions-example-cancel"));
                System.out.printf("cancel receipt=%s outcome=%s reason=%s%n",
                        cancelled.getControl().getId(), cancelled.getControl().getOutcome(),
                        cancelled.getControl().getReason());
            }
        }
    }
}
