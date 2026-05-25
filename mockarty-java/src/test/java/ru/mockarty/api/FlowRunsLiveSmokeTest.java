package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.FlowRunResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class FlowRunsLiveSmokeTest {
    @Test
    void liveHTTPStepReturnsPassed() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.create("http://127.0.0.1:5770", token)) {
            Map<String, Object> flow = Map.of(
                "ir_version", 1,
                "name", "java-sdk-live",
                "steps", List.of(Map.of(
                    "kind", "http",
                    "name", "probe",
                    "http", Map.of(
                        "method", "GET",
                        "path", "http://127.0.0.1:5770/health",
                        "expects", List.of(Map.of("kind","status","args", List.of(200)))
                    )
                ))
            );
            FlowRunResponse resp = client.flowRuns().execute(flow);
            assertNotNull(resp);
            assertEquals("passed", resp.getStatus(), "errors=" + resp.getErrors());
        }
    }
}
