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
            String sourceId = System.getenv("MOCKARTY_SOURCE_CASE_ID");
            if (sourceId == null || sourceId.isEmpty()) sourceId = "sdk-demo:login-smoke";
            String caseName = System.getenv("MOCKARTY_CASE_NAME");
            if (caseName == null || caseName.isEmpty()) caseName = "Login smoke";
            JsonNode created = api.createCase(namespace, Map.of("name", caseName, "externalId", sourceId, "expectedResult", "Signed in", "customFields", List.of(Map.of("name", "Component", "value", "authentication")), "steps", List.of(Map.of("stepUid", "login", "name", "Sign in", "mode", "manual", "executorType", "manual", "action", "Enter valid credentials", "expectedResult", "Home page"))));
            String caseId = created.path("id").asText();
            if (!caseName.equals(api.getCase(namespace, caseId).path("name").asText())) throw new IllegalStateException("case lookup changed the name");
            api.listCases(namespace, Map.of());
            JsonNode version = api.getCaseVersion(namespace, caseId, created.path("currentVersionId").asText());
            List<Map<String,Object>> steps = new ObjectMapper().convertValue(version.path("steps"), new TypeReference<List<Map<String,Object>>>() {});
            steps.get(0).put("expectedResult", "Dashboard displays the signed-in user");
            JsonNode saved = api.saveCaseSteps(namespace, caseId, steps, "Clarify expected result");
            JsonNode repeated = api.saveCaseSteps(namespace, caseId, steps, "Clarify expected result");
            if (!saved.path("id").equals(repeated.path("id")) || !repeated.path("dedupSkipped").asBoolean()) throw new IllegalStateException("unchanged save did not return the same version");
            byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
            String filename = System.getenv("MOCKARTY_ATTACHMENT_NAME");
            if (filename == null || filename.isEmpty()) filename = "login.png";
            JsonNode attachment = api.uploadAttachment(namespace, "tcm_case", caseId, filename, png, "image/png");
            List<JsonNode> media = api.listAttachments(namespace, "tcm_case", caseId);
            String expectedFilename = filename;
            if (media.stream().noneMatch(item -> attachment.path("id").equals(item.path("id")) && expectedFilename.equals(item.path("originalName").asText()))) throw new IllegalStateException("attachment name changed");
            if (!Arrays.equals(api.downloadAttachment(namespace, attachment.path("id").asText()), png)) throw new IllegalStateException("attachment bytes changed");
            JsonNode run = api.runCase(namespace, caseId, Map.of("mode", "manual"));
            api.getCaseRun(namespace, run.path("id").asText());
            System.out.println("Case " + caseId + " version " + saved.path("version") + ", PNG " + attachment.path("id").asText() + " (" + png.length + " bytes), run " + run.path("id").asText() + "; unchanged save deduplicated");
        }
    }
}
