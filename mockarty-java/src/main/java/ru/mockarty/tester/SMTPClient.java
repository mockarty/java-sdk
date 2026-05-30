// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Minimal SMTP client over a raw socket — mirrors
 * {@code sdk/go-sdk/protocols/smtp} (which uses Go's net/smtp). Speaks
 * EHLO, optional AUTH PLAIN, MAIL FROM / RCPT TO / DATA / QUIT. No
 * extra dependency (no JavaMail). Plain transport only — for CI test
 * targets and Mockarty SMTP mocks.
 *
 * <p>Out of scope: STARTTLS, DKIM, bounce parsing.</p>
 */
public final class SMTPClient implements SMTPFacet.SMTPSender {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final int timeoutMillis;

    private SMTPClient(String host, int port, String username, String password, int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.timeoutMillis = timeoutMillis;
    }

    public static Builder builder(String host, int port) {
        return new Builder(host, port);
    }

    public static SMTPClient create(String host, int port) {
        return new Builder(host, port).build();
    }

    @Override
    public SMTPFacet.SendResult send(SMTPFacet.Message msg) throws Exception {
        if (msg.from == null || msg.from.isEmpty()) {
            throw new IOException("mockarty smtp: empty from address");
        }
        if (msg.to.isEmpty()) {
            throw new IOException("mockarty smtp: no recipients");
        }
        String raw = buildMessage(msg);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);

            expect(in, "220");
            sendLine(out, "EHLO mockarty-java");
            readMultiline(in, "250");

            if (username != null && !username.isEmpty()) {
                String creds = "\0" + username + "\0" + (password == null ? "" : password);
                String b64 = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
                sendLine(out, "AUTH PLAIN " + b64);
                expect(in, "235");
            }

            sendLine(out, "MAIL FROM:<" + msg.from + ">");
            expect(in, "250");
            for (String rcpt : msg.to) {
                sendLine(out, "RCPT TO:<" + rcpt + ">");
                expect(in, "250");
            }
            sendLine(out, "DATA");
            expect(in, "354");
            // Dot-stuff the payload and terminate with a lone ".".
            out.write(dotStuff(raw));
            out.write("\r\n.\r\n");
            out.flush();
            expect(in, "250");
            sendLine(out, "QUIT");
        }
        return new SMTPFacet.SendResult(raw);
    }

    // ── internals ─────────────────────────────────────────────────────

    private static String buildMessage(SMTPFacet.Message msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(msg.from).append("\r\n");
        if (!msg.to.isEmpty()) {
            sb.append("To: ").append(String.join(", ", msg.to)).append("\r\n");
        }
        if (msg.subject != null && !msg.subject.isEmpty()) {
            sb.append("Subject: ").append(msg.subject).append("\r\n");
        }
        msg.headers.forEach((k, v) -> {
            String lk = k.toLowerCase();
            if (!lk.equals("from") && !lk.equals("to") && !lk.equals("subject")) {
                sb.append(k).append(": ").append(v).append("\r\n");
            }
        });
        sb.append("\r\n");
        sb.append(msg.body == null ? "" : msg.body);
        return sb.toString();
    }

    private static String dotStuff(String body) {
        // RFC 5321: a line beginning with "." must be escaped to "..".
        StringBuilder sb = new StringBuilder(body.length());
        for (String line : body.split("\n", -1)) {
            String l = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
            if (l.startsWith(".")) { sb.append('.'); }
            sb.append(l).append("\r\n");
        }
        // Trim the trailing CRLF we just added so the caller's terminator
        // sequence stays correct.
        if (sb.length() >= 2) { sb.setLength(sb.length() - 2); }
        return sb.toString();
    }

    private static void sendLine(Writer out, String line) throws IOException {
        out.write(line);
        out.write("\r\n");
        out.flush();
    }

    private static void expect(BufferedReader in, String code) throws IOException {
        String line = readMultiline(in, code);
        if (line == null || !line.startsWith(code)) {
            throw new IOException("mockarty smtp: expected " + code + ", got: " + line);
        }
    }

    /**
     * Reads one (possibly multi-line) SMTP reply and returns the last
     * line. Multi-line replies use "{code}-..." for continuation and
     * "{code} ..." for the final line.
     */
    private static String readMultiline(BufferedReader in, String code) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() >= 4 && line.charAt(3) == '-') {
                continue; // continuation
            }
            return line;
        }
        throw new IOException("mockarty smtp: connection closed awaiting " + code);
    }

    public static final class Builder {
        private final String host;
        private final int port;
        private String username = "";
        private String password = "";
        private int timeoutMillis = 30_000;

        Builder(String host, int port) { this.host = host; this.port = port; }

        public Builder auth(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public Builder timeoutMillis(int ms) {
            if (ms > 0) { this.timeoutMillis = ms; }
            return this;
        }

        public SMTPClient build() {
            return new SMTPClient(host, port, username, password, timeoutMillis);
        }
    }
}
