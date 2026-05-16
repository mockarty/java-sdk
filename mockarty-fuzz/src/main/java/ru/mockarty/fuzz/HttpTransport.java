// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.io.IOException;
import java.util.Map;

/**
 * Tiny HTTP seam so {@link Runner} can be unit-tested with an in-process
 * stub instead of a live socket.
 *
 * <p>Production code uses {@link JdkHttpTransport} (built on
 * {@code java.net.http.HttpClient} from the JDK — no external dep).
 * Tests inject a fake that records requests and returns canned JSON.</p>
 *
 * <p>The seam covers only what {@link Runner} actually needs: a synchronous
 * request/response pair plus a streaming SSE-style server-events
 * subscription. The richer mockarty-java client surface stays untouched —
 * we're deliberately not reusing it here so a consumer can ship the
 * fuzz module without dragging in the full Mockarty admin API client.</p>
 */
public interface HttpTransport {

    /**
     * Performs a single HTTP request and returns the response body as a
     * UTF-8 string. {@code method} is uppercase ("GET"/"POST"/"DELETE");
     * {@code body} is null for GET/DELETE.
     */
    HttpResponse request(String method,
                          String url,
                          Map<String, String> headers,
                          String body) throws IOException, InterruptedException;

    /**
     * Subscribes to a server-sent-events stream. The callback is invoked
     * once per {@code event:}/{@code data:} record received. Returns when
     * the server closes the stream or {@code closer} signals cancellation.
     */
    void streamEvents(String url,
                       Map<String, String> headers,
                       java.util.function.Consumer<String> onLine,
                       java.util.function.BooleanSupplier shouldStop)
            throws IOException, InterruptedException;

    /**
     * Response container — keeps the seam dependency-free (no Apache
     * HttpResponse, no OkHttp Response). Just status + body.
     */
    record HttpResponse(int status, String body) {}
}
