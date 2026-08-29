package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoderDeliveryApiTest {
    @Test
    void startRequiresGoalAndRepository() {
        try (MockartyClient client = MockartyClient.create("http://127.0.0.1:1", "mk_test")) {
            CoderDeliveryApi api = client.coderDelivery();
            assertThrows(IllegalArgumentException.class, () -> api.startMission(Map.of("goal", "ship")));
            assertThrows(IllegalArgumentException.class, () -> api.getMission(""));
            assertThrows(IllegalArgumentException.class, () -> api.reconcileDeploy("m1", ""));
        }
    }

    @Test
    void approvalSuffixPrecedesNamespaceQuery() throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").apiKey("mk_test").namespace("team a").build()) {
            CoderDeliveryApi api = client.coderDelivery();
            var method = CoderDeliveryApi.class.getDeclaredMethod("missionPath", String.class, String.class);
            method.setAccessible(true);
            assertEquals("/api/v1/coder/missions/m%2F1/approve?namespace=team%20a", method.invoke(api, "m/1", "/approve"));
            assertEquals("/api/v1/coder/missions/m%2F1/deploy-outcome?namespace=team%20a", method.invoke(api, "m/1", "/deploy-outcome"));
        }
    }
}
