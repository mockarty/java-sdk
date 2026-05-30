// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage of the s3 / smtp / socket.io facets against a live
 * Mockarty testbackend (cmd/testbackend). Skipped unless the backend URL
 * is provided so the default build stays hermetic.
 *
 * <pre>
 *   # from the main repo:
 *   go run ./cmd/testbackend &amp;
 *   MOCKARTY_TESTBACKEND_URL=http://127.0.0.1:18770 \
 *   MOCKARTY_TESTBACKEND_SMTP_HOST=127.0.0.1 \
 *   MOCKARTY_TESTBACKEND_SMTP_PORT=18772 \
 *     ./gradlew :mockarty-java:test --tests '*ProtocolTargetsIntegration*'
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "MOCKARTY_TESTBACKEND_URL", matches = ".+")
public class TesterProtocolTargetsIntegrationTest {

    private static String base() {
        String u = System.getenv("MOCKARTY_TESTBACKEND_URL");
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    @Test
    void s3LifecycleAgainstTestbackend() {
        S3HttpClient cli = S3HttpClient.create(base() + "/s3");
        Tester t = new Tester.Builder().build();
        String key = "java-it-" + System.currentTimeMillis() + ".txt";
        t.s3(cli).put("mockarty-test", key)
                .body("integration-body").contentType("text/plain").meta("owner", "java")
                .expectOK().expectStatus(200);
        t.s3(cli).get("mockarty-test", key)
                .expectOK().expectBodyEquals("integration-body").expectContentType("text/plain");
        t.s3(cli).head("mockarty-test", key).expectExists();
        t.s3(cli).list("mockarty-test").expectKey(key);
        t.s3(cli).delete("mockarty-test", key).expectOK().expectStatus(204);
        t.s3(cli).get("mockarty-test", key).expectError();
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
    }

    @Test
    void smtpSendAgainstTestbackend() throws Exception {
        String smtpHost = envOr("MOCKARTY_TESTBACKEND_SMTP_HOST", "127.0.0.1");
        int smtpPort = Integer.parseInt(envOr("MOCKARTY_TESTBACKEND_SMTP_PORT", "18772"));
        SMTPClient cli = SMTPClient.builder(smtpHost, smtpPort).auth("alice", "secret").build();
        String rcpt = "javait-" + System.currentTimeMillis() + "@corp";
        String subj = "Java Integration " + rcpt;

        Tester t = new Tester.Builder().build();
        t.smtp(cli).send("sender@corp", rcpt).subject(subj).body("integration body").expectAccepted();
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());

        // Assert receipt via the testbackend inbox surface.
        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> resp = http.send(
                HttpRequest.newBuilder(URI.create(base() + "/smtp/inbox?to=" + rcpt))
                        .timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertTrue(resp.body().contains(subj),
                () -> "delivered message not found in inbox: " + resp.body());
        assertTrue(resp.body().contains("\"authUser\":\"alice\""),
                () -> "authUser not captured: " + resp.body());
    }

    @Test
    void socketIoAgainstTestbackend() {
        Tester t = new Tester.Builder().build();
        t.socketio(base()).connect()
                .emit("greet", "JavaWorld")
                .collect(Duration.ofSeconds(3))
                .expectConnected()
                .expectEvent("greeting")
                .expectEventJsonPath("greeting", "$.msg", "hello JavaWorld");
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }
}
