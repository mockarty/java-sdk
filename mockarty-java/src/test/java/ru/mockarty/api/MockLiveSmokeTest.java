package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Page;
import ru.mockarty.model.SaveMockResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live wire-parity smoke test for {@link MockApi} against a running admin.
 *
 * <p>The hand-written mock tests verify the SDK's own decoding, not that it
 * matches the real admin's wire shape. This drives the full lifecycle —
 * create -> get -> resolve through the stub engine -> list -> delete -> gone —
 * entirely through the SDK (plus a bare HTTP GET to the stub-serving path),
 * so a drift between the Java SDK and the admin's actual JSON fails here.
 *
 * <p>Gated by {@code TOK} (same convention as {@link FlowRunsLiveSmokeTest});
 * skipped otherwise.
 */
@EnabledIfEnvironmentVariable(named = "TOK", matches = ".+")
class MockLiveSmokeTest {

    private static final String BASE = "http://127.0.0.1:5770";

    @Test
    void liveMockCrudAndResolveRoundTrip() throws Exception {
        String token = System.getenv("TOK");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(BASE)
                .apiKey(token)
                .namespace("sandbox")
                .build()) {

            String mockId = "java-sdk-live-" + System.currentTimeMillis();
            String route = "/jsdk/live/" + mockId;
            Map<String, Object> payload = Map.of("ok", true, "id", mockId);

            Mock mock = MockBuilder.http(route, "GET")
                    .id(mockId)
                    .namespace("sandbox")
                    .respond(200, payload)
                    .build();

            // create -> SaveMockResponse{id, mock, isNew}; the server echoes
            // the caller-provided id back verbatim.
            SaveMockResponse created = client.mocks().create(mock);
            assertEquals(mockId, created.getId(),
                    "create returned id=" + created.getId() + ", want " + mockId);

            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            try {
                // get -> Mock with the same id + route (wire shape decodes cleanly)
                Mock got = client.mocks().get(mockId);
                assertEquals(mockId, got.getId(), "get id mismatch");
                assertNotNull(got.getHttp(), "get returned no http context");
                assertEquals(route, got.getHttp().getRoute(), "get route mismatch");

                // resolve through the actual stub engine — the mock really serves
                HttpResponse<String> resolved = http.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(BASE + "/stubs/sandbox" + route))
                                .timeout(Duration.ofSeconds(10))
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, resolved.statusCode(),
                        "stub resolve: " + resolved.statusCode() + " " + resolved.body());
                assertTrue(resolved.body().contains("\"id\":\"" + mockId + "\""),
                        "stub body did not contain the payload id: " + resolved.body());

                // list (active default) includes the new mock
                Page<Mock> page = client.mocks().list("sandbox", null, mockId, 0, 50);
                assertTrue(page.getItems().stream().anyMatch(m -> mockId.equals(m.getId())),
                        "list(search=" + mockId + ") did not include the mock");
            } finally {
                // delete always runs so a failed assertion doesn't leak the mock
                client.mocks().delete(mockId);
            }

            // after delete, the stub no longer resolves
            HttpResponse<String> gone = http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(BASE + "/stubs/sandbox" + route))
                            .timeout(Duration.ofSeconds(10))
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(404, gone.statusCode(),
                    "after delete, stub should 404, got " + gone.statusCode());
        }
    }
}
