package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

public final class MediaDeliveryExample {
    private MediaDeliveryExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create()) {
            System.out.println(client.mediaDelivery().listFenced("transcribe").get("count"));
        }
    }
}
