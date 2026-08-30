package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

public final class CloudIdentityExample {
    private CloudIdentityExample() {}

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.create(System.getenv("MOCKARTY_BASE_URL"), System.getenv("MOCKARTY_API_KEY"))) {
            client.cloudIdentity().list().forEach(identity -> System.out.println(identity.getProvider()));
        }
    }
}
