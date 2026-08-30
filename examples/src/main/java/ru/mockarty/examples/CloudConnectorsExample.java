package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

/** Lists safe connector metadata; secret values are never returned. */
public final class CloudConnectorsExample {
    private CloudConnectorsExample() {}

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder().build()) {
            client.cloudConnectors().list().forEach(connector ->
                    System.out.printf("%s revision=%d secret-configured=%s%n",
                            connector.getKey(), connector.getRevision(), connector.isSecretConfigured()));
        }
    }
}
