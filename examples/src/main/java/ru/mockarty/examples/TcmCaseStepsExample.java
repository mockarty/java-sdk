// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import ru.mockarty.MockartyClient;
import ru.mockarty.api.TcmApi;

/** Create a manual case, edit its steps and save an idempotent version. */
public final class TcmCaseStepsExample {
    public static void main(String[] args) throws Exception {
        String namespace = System.getenv("MOCKARTY_NAMESPACE");
        try (MockartyClient client = MockartyClient.create(System.getenv("MOCKARTY_SERVER"), System.getenv("MOCKARTY_API_KEY"))) {
            TcmApi api = client.tcm();
            JsonNode created = api.createCase(namespace, Map.of("name", "Login smoke", "expectedResult", "Signed in", "steps", List.of(Map.of("stepUid", "login", "name", "Sign in", "mode", "manual", "executorType", "manual", "action", "Enter valid credentials", "expectedResult", "Home page"))));
            String caseId = created.path("id").asText();
            JsonNode version = api.getCaseVersion(namespace, caseId, created.path("currentVersionId").asText());
            List<Map<String,Object>> steps = new ObjectMapper().convertValue(version.path("steps"), new TypeReference<List<Map<String,Object>>>() {});
            steps.get(0).put("expectedResult", "Dashboard displays the signed-in user");
            JsonNode saved = api.saveCaseSteps(namespace, caseId, steps, "Clarify expected result");
            JsonNode repeated = api.saveCaseSteps(namespace, caseId, steps, "Clarify expected result");
            if (!saved.path("id").equals(repeated.path("id")) || !repeated.path("dedupSkipped").asBoolean()) throw new IllegalStateException("unchanged save did not return the same version");
            System.out.println("Case " + caseId + " version " + saved.path("version") + "; unchanged save deduplicated");
        }
    }
}
