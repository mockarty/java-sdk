package ru.mockarty.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.MockartyClient;

/** Creates one Shared SaaS project through the public Cloud proxy. */
public final class CloudSharedProjectsExample {
    private CloudSharedProjectsExample() { }
    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(System.getenv("MOCKARTY_BASE_URL"))
                .apiKey(System.getenv("MOCKARTY_API_KEY")).build()) {
            var body = new ObjectMapper().createObjectNode().put("version", 1);
            var project = client.cloudSharedProjects().create(System.getenv("MOCKARTY_SPACE_ID"), "SDK example", body);
            System.out.println(project.getId() + " revision " + project.getRevision());
        }
    }
}
