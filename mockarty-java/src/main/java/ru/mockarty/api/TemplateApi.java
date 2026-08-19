// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.TemplateFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API for response template file management.
 */
public class TemplateApi {

    private final MockartyClient client;

    public TemplateApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all template files.
     *
     * @return list of template files
     */
    public List<TemplateFile> list() throws MockartyException {
        String namespace = client.getConfig().getNamespace();
        // The server wraps the result as {"templates":["a.txt","b.txt"],...}
        // where each entry is a bare file name (not a TemplateFile object).
        JsonNode root = client.get("/api/v1/templates?namespace=" + encode(namespace), JsonNode.class);
        JsonNode arr = root != null && root.isObject() ? root.path("templates") : root;
        List<TemplateFile> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                if (n.isTextual()) {
                    out.add(new TemplateFile().name(n.asText()).namespace(namespace));
                } else if (n.isObject()) {
                    out.add(client.getObjectMapper().convertValue(n, TemplateFile.class));
                }
            }
        }
        return out;
    }

    /**
     * Gets a specific template file by name.
     *
     * @param name the template file name
     * @return the template file metadata
     */
    public TemplateFile get(String name) throws MockartyException {
        return client.get("/api/v1/templates/" + encode(name), TemplateFile.class);
    }

    /**
     * Uploads a new template file or updates an existing one.
     *
     * @param name    the template file name
     * @param content the file content
     * @return the uploaded template file metadata
     */
    public TemplateFile upload(String name, String content) throws MockartyException {
        Map<String, Object> body = Map.of(
                "name", name,
                "content", content,
                "namespace", client.getConfig().getNamespace()
        );
        return client.post("/api/v1/templates", body, TemplateFile.class);
    }

    /**
     * Deletes a template file.
     *
     * @param name the template file name
     */
    public void delete(String name) throws MockartyException {
        client.delete("/api/v1/templates/" + encode(name));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
