package ru.mockarty.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mockarty.MockartyClient;

public final class ConnectionAuthorityExample {
    private ConnectionAuthorityExample() {}

    public static void main(String[] args) throws Exception {
        String namespace = System.getenv("MOCKARTY_NAMESPACE");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode descriptor = mapper.createObjectNode();
        descriptor.put("namespace", namespace);
        descriptor.put("contractVersion", "mockarty.connection/v1");
        descriptor.put("id", "gitlab-prod");
        descriptor.put("kind", "gitlab");
        descriptor.put("endpoint", "https://gitlab.example.com/api/v4");
        ObjectNode target = descriptor.putObject("targetPolicy");
        target.putArray("schemes").add("https");
        target.putArray("hosts").add("gitlab.example.com");
        target.putArray("ports").add(443);
        target.putArray("pathPrefixes").add("/api/v4");
        ObjectNode secretRef = descriptor.putArray("secretRefs").addObject();
        secretRef.put("storeId", "team-vault");
        secretRef.put("key", "gitlab-token");
        secretRef.put("version", 3);
        descriptor.putArray("allowedOperationIds").add("gitlab.pipeline.observe");

        try (MockartyClient client = MockartyClient.create(
                System.getenv("MOCKARTY_BASE_URL"), System.getenv("MOCKARTY_API_KEY"))) {
            JsonNode snapshot = client.connections().create(descriptor);
            System.out.printf("connection revision %d: %s%n",
                    snapshot.path("descriptor").path("revision").asLong(), snapshot.path("digest").asText());
        }
    }
}
