// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.MissionEffectiveSettingsOptions;
import ru.mockarty.model.MissionStartRequest;
import ru.mockarty.model.MissionCancelRequest;
import ru.mockarty.model.MissionAnswerRequest;
import ru.mockarty.model.MissionRevisionReference;

import java.util.List;
import java.util.Map;

public final class AutonomousMissionsExample {
    private AutonomousMissionsExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(System.getenv("MOCKARTY_BASE_URL"))
                .apiKey(System.getenv("MOCKARTY_API_KEY"))
                .namespace(System.getenv().getOrDefault("MOCKARTY_NAMESPACE", "sandbox"))
                .build()) {
            String exampleAnswer = System.getenv("MOCKARTY_EXAMPLE_ANSWER");
            if (exampleAnswer != null && !exampleAnswer.isBlank()) {
                String missionId = System.getenv("MOCKARTY_ANSWER_MISSION_ID");
                if (missionId == null || missionId.isBlank()) throw new IllegalArgumentException("MOCKARTY_ANSWER_MISSION_ID must identify the mission that asked the captured question");
                String retryKey = System.getenv("MOCKARTY_ANSWER_IDEMPOTENCY_KEY");
                if (retryKey == null || retryKey.isBlank()) throw new IllegalArgumentException("Use a nonempty retry key unique per answered question; reuse it on retry");
                var answerRequest = new MissionAnswerRequest().answer(exampleAnswer)
                        .idempotencyKey(retryKey);
                // Echo data.question from the question the user answered; retain
                // the same identity and retry key after a lost response.
                String questionId = System.getenv("MOCKARTY_QUESTION_ID");
                if (questionId != null && !questionId.isBlank()) {
                    answerRequest.answer(null).questionId(questionId)
                            .revision(Long.parseLong(System.getenv("MOCKARTY_QUESTION_REVISION")))
                            .text(exampleAnswer);
                    String generation = System.getenv("MOCKARTY_QUESTION_GENERATION");
                    if (generation != null && !generation.isBlank()) answerRequest.generation(Long.parseLong(generation));
                    // Choice/entity schemas use selected(...) and entity(...) instead.
                }
                var answered = client.autonomousMissions().answer(missionId, answerRequest);
                System.out.printf("answer receipt=%s outcome=%s%n",
                        answered.getControl().getId(), answered.getControl().getOutcome());
                return;
            }
            String archiveMissionId = System.getenv("MOCKARTY_ARCHIVE_MISSION_ID");
            if (archiveMissionId != null && !archiveMissionId.isBlank()) {
                var archive = client.autonomousMissions().exportArchive(archiveMissionId);
                var restored = client.autonomousMissions().restoreArchive(archive);
                System.out.printf("archive=%s mission=%s created=%s%n",
                        archive.getDigest(), restored.getId(), restored.isCreated());
            }
            String productId = System.getenv().getOrDefault("MOCKARTY_PRODUCT_ID", "");
            var settings = client.autonomousMissions().getEffectiveSettings(
                    new MissionEffectiveSettingsOptions().productId(productId));
            var request = new MissionStartRequest()
                    .goal("Take the checkout release to production quality and provide evidence")
                    .productId(productId)
                    .autonomy(System.getenv().getOrDefault("MOCKARTY_EXAMPLE_AUTONOMY", "auto"))
                    .budget(100000, 0, 0)
                    .expectedSettingsDigest(settings.getSettingsDigest());
            // full preapproves testing of this declared product; chaos and paid
            // effects still need approval. Opt in through the environment above.
            String productUrl = System.getenv("MOCKARTY_PRODUCT_URL");
            if (productUrl != null && !productUrl.isBlank()) {
                request.data(Map.of("contextRefs", List.of(Map.of("kind", "product_url", "value", productUrl))));
            }
            String targetDigest = System.getenv("MOCKARTY_TARGET_DIGEST");
            if (targetDigest != null && !targetDigest.isBlank()) {
                request.targets(List.of(new MissionRevisionReference()
                        .kind("repo")
                        .id(System.getenv("MOCKARTY_TARGET_ID"))
                        .revision(Long.parseLong(System.getenv("MOCKARTY_TARGET_REVISION")))
                        .digest(targetDigest)));
            }
            var started = client.autonomousMissions().start(request);
            System.out.printf("mission=%s status=%s created=%s%n",
                    started.getMission().getId(), started.getMission().getStatus(), started.isCreated());
            if ("1".equals(System.getenv("MOCKARTY_EXAMPLE_CANCEL"))) {
                var cancelled = client.autonomousMissions().cancel(started.getMission().getId(),
                        new MissionCancelRequest().reason("example run no longer needed")
                                .idempotencyKey("autonomous-missions-example-cancel"));
                System.out.printf("cancel receipt=%s outcome=%s reason=%s%n",
                        cancelled.getControl().getId(), cancelled.getControl().getOutcome(),
                        cancelled.getControl().getReason());
                cancelled.getExecutionBindings().forEach(binding ->
                        System.out.printf("child=%s kind=%s state=%s%n",
                                binding.getExternalId(), binding.getKind(), binding.getState()));
            }
        }
    }
}
