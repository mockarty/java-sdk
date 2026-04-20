// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.EntitySearchRequest;
import ru.mockarty.model.EntitySearchResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * API for the unified entity-picker endpoint
 * ({@code GET /api/v1/entity-search}) defined in
 * {@code internal/webui/entity_search_handlers.go}.
 *
 * <p>Used by UI pickers and CI/CD automation that needs to resolve a
 * human-readable name into the canonical UUID before issuing further API
 * calls. Server-side matching is case-insensitive substring on
 * {@code name}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * EntitySearchResponse plans = client.entitySearch().search(
 *     new EntitySearchRequest()
 *         .type(EntitySearchRequest.TYPE_TEST_PLAN)
 *         .query("smoke")
 *         .limit(25));
 * for (EntitySearchResult r : plans.getItems()) {
 *     System.out.println(r.getId() + " " + r.getName());
 * }
 * }</pre>
 */
public class EntitySearchApi {

    private static final String PATH = "/api/v1/entity-search";

    private final MockartyClient client;

    public EntitySearchApi(MockartyClient client) {
        this.client = client;
    }

    /** GET /api/v1/entity-search. */
    public EntitySearchResponse search(EntitySearchRequest req) throws MockartyException {
        if (req == null) {
            throw new IllegalArgumentException("request is required");
        }
        String type = req.getType() == null ? "" : req.getType().trim();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("type is required");
        }
        StringBuilder sb = new StringBuilder();
        append(sb, "type", type);
        if (req.getNamespace() != null) {
            String ns = req.getNamespace().trim();
            if (!ns.isEmpty()) {
                append(sb, "namespace", ns);
            }
        }
        if (req.getQuery() != null) {
            String q = req.getQuery().trim();
            if (!q.isEmpty()) {
                append(sb, "q", q);
            }
        }
        if (req.getLimit() > 0) {
            append(sb, "limit", Integer.toString(req.getLimit()));
        }
        if (req.getOffset() > 0) {
            append(sb, "offset", Integer.toString(req.getOffset()));
        }
        String url = PATH + "?" + sb;
        return client.get(url, EntitySearchResponse.class);
    }

    private static void append(StringBuilder sb, String key, String value) {
        if (sb.length() > 0) {
            sb.append('&');
        }
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
          .append('=')
          .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
