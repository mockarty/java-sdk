// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SMTP facet. Mirrors {@code sdk/go-sdk/tester/smtp.go} and the Python
 * port. Sends a mail and asserts the server accepted (or rejected) it.
 *
 * <p>{@link SMTPSender} is the contract; {@link SMTPClient} is a stdlib
 * implementation. Tests can plug an in-memory fake.</p>
 */
public final class SMTPFacet {

    private final Tester t;
    private final SMTPSender sender;

    SMTPFacet(Tester t, SMTPSender sender) {
        this.t = t;
        this.sender = sender;
    }

    public SMTPStep send(String from, String... to) {
        t.flushPending();
        Map<String, String> v = t.snapshotVars();
        Message msg = new Message();
        msg.from = Interpolate.apply(from, v);
        for (String r : to) {
            msg.to.add(Interpolate.apply(r, v));
        }
        SMTPStep s = new SMTPStep(t, sender, msg);
        t.setPending(s);
        return s;
    }

    /** Minimal contract the SMTP facet needs. */
    public interface SMTPSender {
        SendResult send(Message msg) throws Exception;
    }

    public static final class Message {
        public String from = "";
        public final List<String> to = new ArrayList<>();
        public String subject = "";
        public String body = "";
        public final Map<String, String> headers = new HashMap<>();
    }

    public static final class SendResult {
        public String raw = "";
        public SendResult() {}
        public SendResult(String raw) { this.raw = raw; }
    }

    public static final class SMTPStep implements Committable {
        private final Tester t;
        private final SMTPSender sender;
        private final Message msg;
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private Throwable err;
        private SendResult result = new SendResult();
        private final List<String> failures = new ArrayList<>();

        SMTPStep(Tester t, SMTPSender sender, Message msg) {
            this.t = t; this.sender = sender; this.msg = msg;
        }

        public SMTPStep subject(String s) {
            if (guard("subject")) { return this; }
            msg.subject = Interpolate.apply(s, t.snapshotVars());
            return this;
        }
        public SMTPStep body(String b) {
            if (guard("body")) { return this; }
            msg.body = Interpolate.apply(b, t.snapshotVars());
            return this;
        }
        public SMTPStep header(String k, String v) {
            if (guard("header")) { return this; }
            msg.headers.put(k, Interpolate.apply(v, t.snapshotVars()));
            return this;
        }

        public SMTPStep expectAccepted() {
            if (!ensureSent()) { return this; }
            if (err != null) { fail("expectAccepted: " + err.getMessage()); }
            return this;
        }
        public SMTPStep expectRejected() {
            ensureSent();
            if (err == null) { fail("expectRejected: message was accepted"); }
            return this;
        }
        public SMTPStep expectErrorContains(String sub) {
            ensureSent();
            if (err == null) { return fail("expectErrorContains: no error"); }
            if (err.getMessage() == null || !err.getMessage().contains(sub)) {
                fail("expectErrorContains: " + sub + " not found in: " + err.getMessage());
            }
            return this;
        }

        public String raw() {
            ensureSent();
            return result == null ? "" : result.raw;
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        private SMTPStep fail(String msg) { failures.add(msg); return this; }

        private boolean guard(String method) {
            if (sent) { fail(method + "() called after send"); return true; }
            return false;
        }

        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            startedAt = Instant.now();
            try {
                result = sender.send(msg);
                if (result == null) { result = new SendResult(); }
            } catch (Exception e) {
                err = e;
            }
            endedAt = Instant.now();
            return true;
        }

        @Override
        public void commit() {
            if (committed) { return; }
            committed = true;
            if (!sent) { ensureSent(); }
            StepRecord rec = new StepRecord();
            rec.protocol = "smtp";
            rec.method = "send";
            rec.name = "smtp send " + msg.from + " -> " + String.join(",", msg.to);
            rec.url = String.join(",", msg.to);
            rec.statusOrCode = err != null ? 550 : 250;
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }
}
