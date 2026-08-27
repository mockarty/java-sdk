// Copyright (c) 2026 Mockarty. All rights reserved.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

import java.util.Map;

public final class PageAnalyzerExample {
    private PageAnalyzerExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create()) {
            Map<String, Object> run = client.pageAnalyzer().run(Map.of(
                    "targetUrl", "https://example.com",
                    "options", Map.of("checkResources", true, "followRedirects", true)
            ));
            System.out.println(run);
        }
    }
}
