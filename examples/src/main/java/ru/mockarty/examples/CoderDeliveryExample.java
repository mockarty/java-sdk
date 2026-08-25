package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

import java.util.Map;

public final class CoderDeliveryExample {
    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create()) {
            Map<String, Object> mission = client.coderDelivery().startMission(Map.of(
                    "goal", "Deploy the accepted commit",
                    "repoUrl", System.getenv("CODER_REPO_URL"),
                    "deployTarget", "staging"));
            System.out.println(mission.get("id") + " " + mission.get("status"));
        }
    }

    private CoderDeliveryExample() {}
}
