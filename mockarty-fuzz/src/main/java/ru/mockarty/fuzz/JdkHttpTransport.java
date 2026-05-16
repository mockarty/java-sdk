// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Default {@link HttpTransport} backed by the JDK's
 * {@code java.net.http.HttpClient} — no external dependency. The same
 * pattern the mockarty-pact module uses for its own HTTP calls.
 *
 * <p>SSE handling is intentionally minimal: we open the stream with the
 * appropriate {@code Accept} header and surface each non-blank line to
 * the caller as-is. Parsing the {@code event:}/{@code data:} prefixes
 * lives in {@link Runner} so the transport stays generic.</p>
 */
public final class JdkHttpTransport implements HttpTransport {

    private final HttpClient client;
    private final Duration timeout;

    public JdkHttpTransport() {
        this(Duration.ofSeconds(30));
    }

    public JdkHttpTransport(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public HttpResponse request(String method,
                                 String url,
                                 Map<String, String> headers,
                                 String body) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(timeout);
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                b.header(h.getKey(), h.getValue());
            }
        }
        HttpRequest.BodyPublisher pub = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        // We use {@code method(...)} (rather than the GET/POST helpers) so
        // we can also handle DELETE / PUT without a switch.
        b.method(method, pub);
        java.net.http.HttpResponse<String> resp = client.send(
                b.build(), java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new HttpResponse(resp.statusCode(), resp.body());
    }

    @Override
    public void streamEvents(String url,
                              Map<String, String> headers,
                              Consumer<String> onLine,
                              BooleanSupplier shouldStop) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ZERO.equals(timeout) ? Duration.ofMinutes(60) : timeout)
                .GET();
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                if ("Accept".equalsIgnoreCase(h.getKey())) continue;
                b.header(h.getKey(), h.getValue());
            }
        }
        java.net.http.HttpResponse<java.io.InputStream> resp = client.send(
                b.build(), java.net.http.HttpResponse.BodyHandlers.ofInputStream());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (shouldStop != null && shouldStop.getAsBoolean()) break;
                onLine.accept(line);
            }
        }
    }
}
