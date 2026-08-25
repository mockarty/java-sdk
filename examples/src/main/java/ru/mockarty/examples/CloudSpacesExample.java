// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

import java.util.List;
import java.util.Map;

/** Lists joined and owned Cloud Spaces without inferring a default tenant. */
public final class CloudSpacesExample {
    private CloudSpacesExample() {}

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(System.getenv("MOCKARTY_BASE_URL"))
                .apiKey(System.getenv("MOCKARTY_API_KEY"))
                .build()) {
            List<Map<String, Object>> spaces = (List<Map<String, Object>>) client.cloudSpaces()
                    .list("", 25).getOrDefault("items", List.of());
            spaces.forEach(space -> System.out.printf("%s %s role=%s revision=%s%n",
                    space.get("id"), space.get("name"), space.get("role"), space.get("revision")));
            if (!spaces.isEmpty()) {
                String spaceId = String.valueOf(spaces.get(0).get("id"));
                Map<String, Object> selected = (Map<String, Object>) client.cloudSpaces().get(spaceId).get("space");
                List<Map<String, Object>> members = (List<Map<String, Object>>) client.cloudSpaces()
                        .listMembers(spaceId, "", 25).getOrDefault("items", List.of());
                List<Map<String, Object>> invites = (List<Map<String, Object>>) client.cloudSpaces()
                        .listInvites(spaceId, "", 25).getOrDefault("items", List.of());
                System.out.printf("selected=%s members=%d invites=%d%n", spaceId, members.size(), invites.size());

                String inviteEmail = System.getenv("MOCKARTY_INVITE_EMAIL");
                if (inviteEmail != null && !inviteEmail.isBlank()) {
                    String key = System.getenv("MOCKARTY_IDEMPOTENCY_KEY");
                    if (key == null || key.isBlank()) {
                        throw new IllegalArgumentException(
                                "set MOCKARTY_IDEMPOTENCY_KEY and preserve it for an exact retry");
                    }
                    String etag = "\"space-" + spaceId + "-r" + selected.get("revision") + "\"";
                    Map<String, Object> created = client.cloudSpaces().createInvite(
                            spaceId, inviteEmail, "viewer", 0, etag, key);
                    Map<String, Object> invite = (Map<String, Object>) created.get("invite");
                    System.out.printf("invite=%s token=%s next_revision=%s%n",
                            invite.get("id"), invite.get("token"), created.get("revision"));
                }
            }
            String inviteToken = System.getenv("MOCKARTY_INVITE_TOKEN");
            if (inviteToken != null && !inviteToken.isBlank()) {
                Map<String, Object> preview = client.cloudSpaces().previewInvite(inviteToken);
                String key = System.getenv("MOCKARTY_IDEMPOTENCY_KEY");
                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException(
                            "set MOCKARTY_IDEMPOTENCY_KEY and preserve it for an exact retry");
                }
                Map<String, Object> accepted = client.cloudSpaces().acceptInvite(
                        inviteToken, String.valueOf(preview.get("etag")), key);
                System.out.printf("accepted_space=%s role=%s%n",
                        accepted.get("space_id"), accepted.get("role"));
            }
        }
    }
}
