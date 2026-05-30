// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline tests for the LoadTestBuilder DSL → k6 script / perf-config. The
 * local-run round-trip (config → perf engine → requests &gt; 0) is proven on
 * the Go/CLI side in cmd/cli/cmd/perf_from_config_test.go; here we verify the
 * SDK emits a config of exactly that shape.
 */
class LoadTestBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void basicScriptHasOptionsAndRequest() {
        String script = LoadTestBuilder.named("smoke")
                .target("http://127.0.0.1:8080")
                .get("/health")
                .vus(5)
                .duration("30s")
                .toK6Script();

        assertTrue(script.contains("import http from 'k6/http'"));
        assertTrue(script.contains("export const options"));
        assertTrue(script.contains("export default function"));
        assertTrue(script.contains("http.get(`${__ENV.BASE_URL}/health`)"));
        assertTrue(script.contains("\"vus\":5"));
        assertTrue(script.contains("\"duration\":\"30s\""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void stagesWinOverConstantVus() {
        Map<String, Object> cfg = LoadTestBuilder.named("ramp")
                .target("http://x")
                .get("/")
                .vus(99)
                .stage("10s", 20)
                .stage("30s", 20)
                .stage("5s", 0)
                .toPerfConfig();

        List<Map<String, Object>> stages = (List<Map<String, Object>>) cfg.get("stages");
        assertEquals(3, stages.size());
        assertEquals("10s", stages.get(0).get("duration"));
        assertEquals(20, stages.get(0).get("target"));
        assertFalse(cfg.containsKey("vus"), "vus must not be emitted when stages present");
    }

    @Test
    @SuppressWarnings("unchecked")
    void thresholdsCollected() {
        Map<String, Object> cfg = LoadTestBuilder.named("t")
                .target("http://x")
                .threshold("http_req_duration", "p(95)<500")
                .threshold("http_req_duration", "p(99)<900")
                .threshold("http_req_failed", "rate<0.01")
                .toPerfConfig();

        Map<String, List<String>> th = (Map<String, List<String>>) cfg.get("thresholds");
        assertEquals(List.of("p(95)<500", "p(99)<900"), th.get("http_req_duration"));
        assertEquals(List.of("rate<0.01"), th.get("http_req_failed"));
    }

    @Test
    void postBodyJsonContentType() {
        String script = LoadTestBuilder.named("t")
                .target("http://api")
                .post("/cart", Map.of("sku", "abc"))
                .toK6Script();

        assertTrue(script.contains("http.post(`${__ENV.BASE_URL}/cart`"));
        assertTrue(script.contains("application/json"));
        assertTrue(script.contains("sku"));
        assertTrue(script.contains("abc"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void envAndBaseUrl() {
        Map<String, Object> cfg = LoadTestBuilder.named("t")
                .target("https://staging.example.com")
                .env("TOKEN", "secret")
                .get("/")
                .toPerfConfig();

        Map<String, String> env = (Map<String, String>) cfg.get("environment");
        assertEquals("https://staging.example.com", env.get("BASE_URL"));
        assertEquals("secret", env.get("TOKEN"));
    }

    @Test
    void defaultRequestIsGetRoot() {
        String script = LoadTestBuilder.named("t").target("http://x").toK6Script();
        assertTrue(script.contains("http.get(`${__ENV.BASE_URL}/`)"));
    }

    @Test
    void toJsonIsValidAndFullProfile() throws Exception {
        String raw = LoadTestBuilder.named("checkout")
                .target("http://127.0.0.1:8080")
                .get("/health")
                .post("/order", Map.of("item", 1))
                .stage("2s", 3)
                .threshold("http_req_failed", "rate<0.1")
                .thinkTime(0.5)
                .toJson();

        Map<String, Object> parsed = MAPPER.readValue(raw, Map.class);
        assertEquals("checkout", parsed.get("name"));
        String script = (String) parsed.get("script");
        assertTrue(script.startsWith("import http"));
        assertTrue(script.contains("sleep(0.5)"));
    }

    @Test
    void save(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Path p = dir.resolve("load.json");
        String out = LoadTestBuilder.named("x").target("http://x").get("/").save(p.toString());
        assertEquals(p.toString(), out);
        Map<String, Object> parsed = MAPPER.readValue(Files.readString(p), Map.class);
        assertEquals("x", parsed.get("name"));
        assertTrue(parsed.containsKey("script"));
    }
}
