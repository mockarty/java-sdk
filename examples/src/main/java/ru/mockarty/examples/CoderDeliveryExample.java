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
            System.out.println("independent AQC " + mission.get("aqcEvidence")
                    + " merge status " + mission.get("mrMergeStatus")
                    + " repair attempts " + mission.get("deployRepairAttempts"));
            if ("1".equals(System.getenv("CODER_ADD_GO_CHECK"))) {
                Map<String, Object> requiredCheck = Map.of(
                        "name", "Go unit tests",
                        "args", java.util.List.of("go", "test", "./..."));
                Map<String, Object> task = Map.of(
                        "prompt", "Run and fix the Go unit suite",
                        "requiredChecks", java.util.List.of(requiredCheck));
                mission = client.coderDelivery().addToMission(mission.get("id").toString(), Map.of(
                        "tasks", java.util.List.of(task)));
            }
            String outcome = System.getenv("CODER_DEPLOY_RECONCILIATION");
            if (outcome != null && !outcome.isBlank()) {
                mission = client.coderDelivery().reconcileDeploy(mission.get("id").toString(), outcome);
                System.out.println("reconciled " + mission.get("deployStopState"));
            }
            if ("1".equals(System.getenv("CODER_OBSERVE"))) {
                Map<String, Object> sources = client.coderDelivery().observabilitySources();
                System.out.println("observability " + sources.get("sources"));
            }
        }
    }

    private CoderDeliveryExample() {}
}
