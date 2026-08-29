package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.CloudInstanceCreateResult;

public final class CloudInstancesExample {
    private CloudInstancesExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(System.getenv("MOCKARTY_CLOUD_URL"))
                .apiKey(System.getenv("MOCKARTY_CLOUD_TOKEN")).build()) {
            CloudInstanceCreateResult result = client.cloudInstances().create(
                    System.getenv("MOCKARTY_CLOUD_SPACE_ID"), "Managed beta", "example-create-1");
            // Persist result.getBootstrap().getPassword() in a secret manager; never log it.
            System.out.println(result.getInstance().getId() + " bootstrap available="
                    + (result.getBootstrap() != null && result.getBootstrap().isAvailable()));
        }
    }
}
