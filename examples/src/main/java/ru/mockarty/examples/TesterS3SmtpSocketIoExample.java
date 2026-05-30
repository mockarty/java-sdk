// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.tester.S3HttpClient;
import ru.mockarty.tester.SMTPClient;
import ru.mockarty.tester.Tester;

import java.time.Duration;

/**
 * Author one autotest that exercises S3, SMTP and Socket.IO endpoints
 * through the Mockarty SDK Tester — the same way you'd test HTTP / gRPC
 * / Kafka.
 *
 * <p>Point it at a Mockarty testbackend (cmd/testbackend) or any
 * S3-compatible / SMTP / Socket.IO server via env vars:</p>
 *
 * <pre>
 *   # from the main repo:
 *   go run ./cmd/testbackend &amp;
 *   S3_ENDPOINT=http://localhost:18770/s3 \
 *   SMTP_HOST=localhost SMTP_PORT=18772 \
 *   SOCKETIO_URL=http://localhost:18770 \
 *     java ru.mockarty.examples.TesterS3SmtpSocketIoExample
 * </pre>
 */
public final class TesterS3SmtpSocketIoExample {

    public static void main(String[] args) {
        String s3Endpoint = envOr("S3_ENDPOINT", "http://localhost:18770/s3");
        String smtpHost = envOr("SMTP_HOST", "localhost");
        int smtpPort = Integer.parseInt(envOr("SMTP_PORT", "18772"));
        String socketUrl = envOr("SOCKETIO_URL", "http://localhost:18770");

        Tester t = new Tester.Builder().build();

        // ── S3: put -> get -> list -> delete ──────────────────────────
        S3HttpClient s3 = S3HttpClient.create(s3Endpoint);
        String key = "report-" + System.currentTimeMillis() + ".csv";
        t.s3(s3).put("mockarty-test", key)
                .body("region,sales\neu,42\n").contentType("text/csv").meta("owner", "finance")
                .expectOK().expectStatus(200).extractETag("etag");
        t.s3(s3).get("mockarty-test", key)
                .expectOK().expectContentType("text/csv")
                .expectBodyContains("eu,42").expectMeta("owner", "finance");
        t.s3(s3).list("mockarty-test").expectKey(key);
        t.s3(s3).delete("mockarty-test", key).expectOK().expectStatus(204);

        // ── SMTP: send an authenticated mail ──────────────────────────
        SMTPClient mail = SMTPClient.builder(smtpHost, smtpPort).auth("user", "pass").build();
        t.smtp(mail).send("billing@corp", "customer@corp")
                .subject("Your invoice").body("Please find your invoice attached.")
                .expectAccepted();

        // ── Socket.IO: connect -> emit -> assert echoed events ────────
        t.socketio(socketUrl).connect()
                .emit("greet", "World").collect(Duration.ofSeconds(2))
                .expectConnected()
                .expectEvent("greeting")
                .expectEventJsonPath("greeting", "$.msg", "hello World");

        t.finish();

        if (t.ok()) {
            System.out.println("PASS — " + t.report().size() + " steps");
        } else {
            System.out.println("FAIL:");
            t.errors().forEach(e -> System.out.println("  - " + e));
            System.exit(1);
        }
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }

    private TesterS3SmtpSocketIoExample() {}
}
