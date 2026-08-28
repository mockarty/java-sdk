package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CloudSharedProject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public Cloud proxy for Shared SaaS project CRUD. */
public class CloudSharedProjectsApi {
    private final MockartyClient client;

    public CloudSharedProjectsApi(MockartyClient client) { this.client = client; }

    public Page list(String spaceId, String cursor, int limit) throws MockartyException {
        int bounded = limit < 1 || limit > 100 ? 50 : limit;
        String path = collection(spaceId) + "?limit=" + bounded;
        if (cursor != null && !cursor.isBlank()) path += "&cursor=" + encode(cursor);
        Page page = client.get(path, Page.class);
        return page == null ? new Page() : page;
    }

    public CloudSharedProject get(String spaceId, String projectId) throws MockartyException {
        return client.get(project(spaceId, projectId), CloudSharedProject.class);
    }

    public CloudSharedProject create(String spaceId, String name, JsonNode body) throws MockartyException {
        return client.post(collection(spaceId), mutation(name, body, 0), CloudSharedProject.class);
    }

    public CloudSharedProject update(String spaceId, String projectId, String name, JsonNode body, long revision) throws MockartyException {
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
        return client.put(project(spaceId, projectId), mutation(name, body, revision), CloudSharedProject.class);
    }

    public void delete(String spaceId, String projectId, long revision) throws MockartyException {
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
        client.delete(project(spaceId, projectId) + "?revision=" + revision);
    }

    private static Map<String, Object> mutation(String name, JsonNode body, long revision) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", name);
        value.put("body", body);
        if (revision > 0) value.put("revision", revision);
        return value;
    }

    private static String collection(String spaceId) {
        if (spaceId == null || spaceId.isBlank()) throw new IllegalArgumentException("space id is required");
        return "/api/v1/cloud/spaces/" + encode(spaceId) + "/shared/projects";
    }

    private static String project(String spaceId, String projectId) {
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("project id is required");
        return collection(spaceId) + "/" + encode(projectId);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Page {
        private List<CloudSharedProject> projects = Collections.emptyList();
        @com.fasterxml.jackson.annotation.JsonProperty("next_cursor") private String nextCursor;
        @com.fasterxml.jackson.annotation.JsonProperty("has_more") private boolean hasMore;
        public Page() { }
        public List<CloudSharedProject> getProjects() { return projects == null ? Collections.emptyList() : projects; }
        public String getNextCursor() { return nextCursor; }
        public boolean isHasMore() { return hasMore; }
    }
}
