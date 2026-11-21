// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.pact.broker;

/** Thrown by {@link BrokerClient#fetch} on 404. */
public final class PactNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PactNotFoundException(String resource) {
        super("pact not found in broker: " + resource);
    }
}
