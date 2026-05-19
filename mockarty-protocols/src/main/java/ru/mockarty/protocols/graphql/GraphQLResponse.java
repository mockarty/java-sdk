package ru.mockarty.protocols.graphql;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** Parsed GraphQL response. Mirrors the Python SDK's GraphQLResponse shape. */
public final class GraphQLResponse {

    private final int statusCode;
    private final JsonNode data;
    private final List<Map<String, Object>> errors;
    private final Map<String, Object> extensions;

    GraphQLResponse(int statusCode, JsonNode data, List<Map<String, Object>> errors, Map<String, Object> extensions) {
        this.statusCode = statusCode;
        this.data = data;
        this.errors = List.copyOf(errors);
        this.extensions = Map.copyOf(extensions);
    }

    public int getStatusCode() { return statusCode; }
    public JsonNode getData() { return data; }
    public List<Map<String, Object>> getErrors() { return errors; }
    public Map<String, Object> getExtensions() { return extensions; }

    /** @return true when HTTP status is &lt; 400 AND no GraphQL errors[]. */
    public boolean isOk() { return statusCode < 400 && errors.isEmpty(); }
}
