// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageAnalyzerApiTest {
    @Test
    void pathsCarryConfiguredNamespace() throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").apiKey("mk_test").namespace("team a").build()) {
            PageAnalyzerApi api = client.pageAnalyzer();
            var method = PageAnalyzerApi.class.getDeclaredMethod("path", String.class);
            method.setAccessible(true);
            assertEquals("/api/v1/page-analyzer/results/res-1?namespace=team%20a",
                    method.invoke(api, "results/res-1"));
        }
    }
}
