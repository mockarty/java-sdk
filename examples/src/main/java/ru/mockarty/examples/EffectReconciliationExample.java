package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

public final class EffectReconciliationExample {
    private EffectReconciliationExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create()) {
            System.out.println(client.effectReconciliation().listQueue(null, null, null, 0, 20, null).get("items"));
        }
    }
}
