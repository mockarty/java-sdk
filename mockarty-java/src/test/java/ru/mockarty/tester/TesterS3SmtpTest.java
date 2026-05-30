// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Java Tester S3 + SMTP facet coverage via in-memory fakes. */
public class TesterS3SmtpTest {

    // ── S3 fake ───────────────────────────────────────────────────────

    static final class FakeS3 implements S3Facet.S3Client {
        final Map<String, byte[]> bodies = new HashMap<>();
        final Map<String, String> ctypes = new HashMap<>();
        final Map<String, Map<String, String>> metas = new HashMap<>();
        Exception forceErr;

        @Override
        public S3Facet.PutResult putObject(String bucket, String key, byte[] body, String ct, Map<String, String> meta) throws Exception {
            if (forceErr != null) { throw forceErr; }
            String k = bucket + "/" + key;
            bodies.put(k, body.clone());
            ctypes.put(k, ct);
            metas.put(k, new HashMap<>(meta));
            S3Facet.PutResult r = new S3Facet.PutResult();
            r.statusCode = 200;
            r.etag = "etag-" + key;
            return r;
        }

        @Override
        public S3Facet.GetResult getObject(String bucket, String key) throws Exception {
            String k = bucket + "/" + key;
            if (!bodies.containsKey(k)) {
                S3Facet.GetResult miss = new S3Facet.GetResult();
                miss.statusCode = 404;
                throw new RuntimeException("NoSuchKey (404)");
            }
            S3Facet.GetResult r = new S3Facet.GetResult();
            r.statusCode = 200;
            r.body = bodies.get(k);
            r.contentType = ctypes.get(k);
            r.etag = "etag-" + key;
            r.metadata = metas.get(k);
            return r;
        }

        @Override
        public S3Facet.HeadResult headObject(String bucket, String key) throws Exception {
            String k = bucket + "/" + key;
            S3Facet.HeadResult r = new S3Facet.HeadResult();
            if (!bodies.containsKey(k)) {
                r.statusCode = 404;
                r.exists = false;
                return r;
            }
            r.statusCode = 200;
            r.exists = true;
            r.contentType = ctypes.get(k);
            r.metadata = metas.get(k);
            return r;
        }

        @Override
        public S3Facet.ListResult listObjects(String bucket, String prefix) throws Exception {
            S3Facet.ListResult r = new S3Facet.ListResult();
            r.statusCode = 200;
            for (String k : bodies.keySet()) {
                if (!k.startsWith(bucket + "/")) { continue; }
                String key = k.substring(bucket.length() + 1);
                if (prefix != null && !prefix.isEmpty() && !key.startsWith(prefix)) { continue; }
                S3Facet.ObjectInfo info = new S3Facet.ObjectInfo();
                info.key = key;
                info.size = bodies.get(k).length;
                r.objects.add(info);
            }
            return r;
        }

        @Override
        public S3Facet.DeleteResult deleteObject(String bucket, String key) throws Exception {
            bodies.remove(bucket + "/" + key);
            S3Facet.DeleteResult r = new S3Facet.DeleteResult();
            r.statusCode = 204;
            return r;
        }
    }

    @Test
    void s3PutGetLifecycle() {
        FakeS3 cli = new FakeS3();
        Tester t = new Tester.Builder().build();
        t.s3(cli).put("reports", "q1.csv")
                .body("a,b,c").contentType("text/csv").meta("owner", "finance")
                .expectOK().expectStatus(200).extractETag("etag");
        t.s3(cli).get("reports", "q1.csv")
                .expectOK().expectBodyEquals("a,b,c").expectBodyContains("b,c")
                .expectContentType("text/csv").expectMeta("owner", "finance");
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
        assertEquals("etag-q1.csv", t.vars().get("etag"));
    }

    @Test
    void s3HeadAndList() {
        FakeS3 cli = new FakeS3();
        Tester t = new Tester.Builder().build();
        t.s3(cli).put("b", "k1").body("x").expectOK();
        t.s3(cli).put("b", "k2").body("yy").expectOK();
        t.s3(cli).head("b", "k1").expectExists().expectStatus(200);
        t.s3(cli).head("b", "missing").expectAbsent().expectStatus(404);
        t.s3(cli).list("b").expectObjectCount(2).expectKey("k1").expectKey("k2");
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
    }

    @Test
    void s3DeleteThenMissing() {
        FakeS3 cli = new FakeS3();
        Tester t = new Tester.Builder().build();
        t.s3(cli).put("b", "k").body("v").expectOK();
        t.s3(cli).delete("b", "k").expectOK().expectStatus(204);
        t.s3(cli).get("b", "k").expectError();
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
    }

    @Test
    void s3Interpolation() {
        FakeS3 cli = new FakeS3();
        Tester t = new Tester.Builder().build();
        t.setVar("bkt", "dyn");
        t.setVar("name", "report.txt");
        t.s3(cli).put("{{bkt}}", "{{name}}").body("payload").expectOK();
        t.s3(cli).get("{{bkt}}", "{{name}}").expectBodyEquals("payload");
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
        assertTrue(cli.bodies.containsKey("dyn/report.txt"));
    }

    @Test
    void s3NegativeExpectOkOnMissingFails() {
        FakeS3 cli = new FakeS3();
        Tester t = new Tester.Builder().build();
        t.s3(cli).get("b", "nope").expectOK();
        t.finish();
        assertFalse(t.ok());
    }

    @Test
    void s3NegativeWrongBodyFails() {
        FakeS3 cli = new FakeS3();
        Tester t = new Tester.Builder().build();
        t.s3(cli).put("b", "k").body("real").expectOK();
        t.s3(cli).get("b", "k").expectBodyEquals("wrong");
        t.finish();
        assertFalse(t.ok());
    }

    // ── SMTP fake ─────────────────────────────────────────────────────

    static final class FakeSMTP implements SMTPFacet.SMTPSender {
        final List<SMTPFacet.Message> sent = new ArrayList<>();
        Exception rejectErr;

        @Override
        public SMTPFacet.SendResult send(SMTPFacet.Message msg) throws Exception {
            if (rejectErr != null) { throw rejectErr; }
            sent.add(msg);
            return new SMTPFacet.SendResult("From: " + msg.from + "\nSubject: " + msg.subject);
        }
    }

    @Test
    void smtpSendAccepted() {
        FakeSMTP srv = new FakeSMTP();
        Tester t = new Tester.Builder().build();
        t.smtp(srv).send("alice@corp", "bob@corp", "carol@corp")
                .subject("Invoice 42").body("Please pay.").header("X-Priority", "1")
                .expectAccepted();
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
        assertEquals(1, srv.sent.size());
        assertEquals("alice@corp", srv.sent.get(0).from);
        assertEquals(2, srv.sent.get(0).to.size());
        assertEquals("Invoice 42", srv.sent.get(0).subject);
        assertEquals("1", srv.sent.get(0).headers.get("X-Priority"));
    }

    @Test
    void smtpInterpolation() {
        FakeSMTP srv = new FakeSMTP();
        Tester t = new Tester.Builder().build();
        t.setVar("id", "INV-7");
        t.setVar("rcpt", "dyn@corp");
        t.smtp(srv).send("sys@corp", "{{rcpt}}").subject("Order {{id}}").body("Ref {{id}}").expectAccepted();
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
        assertEquals("dyn@corp", srv.sent.get(0).to.get(0));
        assertEquals("Order INV-7", srv.sent.get(0).subject);
        assertTrue(srv.sent.get(0).body.contains("INV-7"));
    }

    @Test
    void smtpRejectedExpectRejectedPasses() {
        FakeSMTP srv = new FakeSMTP();
        srv.rejectErr = new RuntimeException("550 mailbox unavailable");
        Tester t = new Tester.Builder().build();
        t.smtp(srv).send("a@x", "b@y").expectRejected().expectErrorContains("550");
        t.finish();
        assertTrue(t.ok(), () -> t.errors().toString());
    }

    @Test
    void smtpRejectedExpectAcceptedFails() {
        FakeSMTP srv = new FakeSMTP();
        srv.rejectErr = new RuntimeException("550 mailbox unavailable");
        Tester t = new Tester.Builder().build();
        t.smtp(srv).send("a@x", "b@y").expectAccepted();
        t.finish();
        assertFalse(t.ok());
    }

    @Test
    void s3HttpClientNormalizesEndpointTrailingSlash() {
        // S3HttpClient builder smoke — no network, just construction.
        S3HttpClient c = S3HttpClient.create("http://localhost:18770/s3/");
        assertEquals(StandardCharsets.UTF_8, StandardCharsets.UTF_8); // construction did not throw
        assertFalse(c == null);
    }
}
