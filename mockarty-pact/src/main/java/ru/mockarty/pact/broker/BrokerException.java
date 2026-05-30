// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.broker;

/**
 * Surfaces a 4xx/5xx response from the Pact Broker. {@code status} +
 * {@code body} let CI scripts log + branch on classes of failure
 * (auth, validation, server-side).
 */
public final class BrokerException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int status;
    private final String body;

    public BrokerException(int status, String body, String message) {
        super(message != null && !message.isBlank()
            ? message
            : "pact broker HTTP " + status + ": " + truncate(body));
        this.status = status;
        this.body = body == null ? "" : body;
    }

    public int status() { return status; }
    public String body() { return body; }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
