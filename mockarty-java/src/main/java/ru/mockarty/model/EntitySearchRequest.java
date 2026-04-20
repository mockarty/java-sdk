// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

/**
 * Fluent builder for narrowing a unified entity-search call.
 *
 * <p>Only {@link #type(String)} is required. Tenant-scoped tokens cannot
 * search a different namespace — the server silently ignores the override
 * and uses the token's bound namespace. Global admins may pass any
 * namespace (or leave it null for cross-namespace search).</p>
 *
 * <p>Use the {@code TYPE_*} constants to avoid typos at the call site.</p>
 */
public class EntitySearchRequest {

    // ── Entity type constants — kept in sync with the server-side handler.
    public static final String TYPE_MOCK = "mock";
    public static final String TYPE_TEST_PLAN = "test_plan";
    public static final String TYPE_PERF_CONFIG = "perf_config";
    public static final String TYPE_FUZZ_CONFIG = "fuzz_config";
    public static final String TYPE_CHAOS_EXPERIMENT = "chaos_experiment";
    public static final String TYPE_CONTRACT_PACT = "contract_pact";

    /** Server default — applied when {@link #limit} is not set. */
    public static final int DEFAULT_LIMIT = 50;
    /** Server-side hard cap — larger values are silently clamped. */
    public static final int MAX_LIMIT = 200;

    private String type;
    private String namespace;
    private String query;
    private int limit;
    private int offset;

    public EntitySearchRequest type(String type) { this.type = type; return this; }
    public EntitySearchRequest namespace(String namespace) { this.namespace = namespace; return this; }
    public EntitySearchRequest query(String query) { this.query = query; return this; }
    public EntitySearchRequest limit(int limit) { this.limit = limit; return this; }
    public EntitySearchRequest offset(int offset) { this.offset = offset; return this; }

    public String getType() { return type; }
    public String getNamespace() { return namespace; }
    public String getQuery() { return query; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
