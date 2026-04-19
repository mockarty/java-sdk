// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

/**
 * Extra knobs for {@code TestPlanApi.patch(...)}.
 *
 * <p>{@code ifMatch} is the RFC 7232 strong-validator string the server
 * compares against the plan's current etag. When {@code null} or empty the
 * SDK does a pre-fetch to obtain a fresh value — safe for one-shot CLI
 * tooling but vulnerable to lost updates in concurrent scenarios. Always
 * pass an explicit etag captured from a prior {@code create} / {@code get}
 * / {@code patch} response when correctness matters.</p>
 *
 * <p>{@code namespace} overrides the client-default namespace for this
 * call only — useful for admin tooling that reaches into multiple tenants
 * from one client.</p>
 */
public class PatchOptions {

    private String ifMatch;
    private String namespace;

    public PatchOptions() {
    }

    public static PatchOptions of(String ifMatch) {
        return new PatchOptions().ifMatch(ifMatch);
    }

    public PatchOptions ifMatch(String ifMatch) {
        this.ifMatch = ifMatch;
        return this;
    }

    public PatchOptions namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getIfMatch() {
        return ifMatch;
    }

    public String getNamespace() {
        return namespace;
    }
}
