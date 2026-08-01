// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.Tag;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API for tag management operations.
 */
public class TagApi {

    private final MockartyClient client;

    public TagApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all tags.
     *
     * @return list of tags
     */
    public List<Tag> list() throws MockartyException {
        String namespace = client.getConfig().getNamespace();
        String path = "/api/v1/tags";
        if (namespace != null && !namespace.isEmpty()) {
            path += "?namespace=" + URLEncoder.encode(namespace, StandardCharsets.UTF_8);
        }
        // The server wraps the result as {"namespace":..,"tags":["a","b"]} where
        // each entry is a bare tag name (not a Tag object), so unwrap + map.
        JsonNode root = client.get(path, JsonNode.class);
        JsonNode arr = root != null && root.isObject() ? root.path("tags") : root;
        List<Tag> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                if (n.isTextual()) {
                    out.add(new Tag().name(n.asText()));
                } else if (n.isObject()) {
                    out.add(client.getObjectMapper().convertValue(n, Tag.class));
                }
            }
        }
        return out;
    }

    /**
     * Creates a new tag.
     *
     * @param name the tag name
     * @return the created tag
     */
    public Tag create(String name) throws MockartyException {
        return client.post("/api/v1/tags", Map.of("name", name), Tag.class);
    }
}
