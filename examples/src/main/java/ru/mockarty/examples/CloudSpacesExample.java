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
            String auditSpaceId = System.getenv("MOCKARTY_TEAM_AUDIT_SPACE_ID");
            if (auditSpaceId != null && !auditSpaceId.isBlank()) {
                Map<String, Object> audit = client.cloudSpaces().listTeamAudit(
                        auditSpaceId, System.getenv("MOCKARTY_TEAM_AUDIT_BEFORE_ID"), 50);
                List<Map<String, Object>> records = (List<Map<String, Object>>) audit.getOrDefault("records", List.of());
                System.out.printf("team=%s events=%d chain_ok=%s has_more=%s next_before_id=%s%n",
                        audit.get("team_id"), records.size(), audit.get("chain_ok"),
                        audit.get("has_more"), audit.get("next_before_id"));
            }
            if (!spaces.isEmpty()) {
                String spaceId = String.valueOf(spaces.get(0).get("id"));
                Map<String, Object> selected = (Map<String, Object>) client.cloudSpaces().get(spaceId).get("space");
                if (Boolean.TRUE.equals(selected.get("owned"))) {
                    Map<String, Object> organization = (Map<String, Object>) client.cloudSpaces()
                            .organization(spaceId).get("organization");
                    System.out.printf("account=%s occupied=%s/%s%n", organization.get("account_id"),
                            organization.get("seats_used"), organization.get("seats_total"));
                    if ("team".equals(organization.get("account_kind"))) {
                        String removeMemberId = System.getenv("MOCKARTY_TEAM_REMOVE_MEMBER_ID");
                        if (removeMemberId != null && !removeMemberId.isBlank()) {
                            client.cloudSpaces().removeTeamMember(spaceId, removeMemberId,
                                    System.getenv("MOCKARTY_TEAM_MEMBER_REMOVE_KEY"));
                            System.out.println("Team member removed");
                        }
                        String successorId = System.getenv("MOCKARTY_TEAM_SUCCESSOR_ID");
                        if (successorId != null && !successorId.isBlank()) {
                            client.cloudSpaces().transferTeamOwner(spaceId, successorId,
                                    System.getenv("MOCKARTY_TEAM_OWNER_TRANSFER_KEY"));
                            System.out.println("Team ownership transferred");
                        }
                        String teamSpaceName = System.getenv("MOCKARTY_TEAM_SPACE_NAME");
                        if (teamSpaceName != null && !teamSpaceName.isBlank()) {
                            Map<String, Object> created = client.cloudSpaces().createTeamSpace(
                                    String.valueOf(organization.get("account_id")), teamSpaceName, "team", "",
                                    System.getenv("MOCKARTY_TEAM_SPACE_KEY"));
                            System.out.printf("created_team_space=%s%n", created.get("space_id"));
                        }
                        String teamEmail = System.getenv("MOCKARTY_TEAM_MEMBER_EMAIL");
                        if (teamEmail != null && !teamEmail.isBlank()) {
                            client.cloudSpaces().inviteTeamMember(
                                    spaceId, teamEmail, "member", System.getenv("MOCKARTY_TEAM_INVITE_KEY"));
                            System.out.println("Team invitation queued");
                        }
                    }
                    String teamInviteId = System.getenv("MOCKARTY_TEAM_INVITE_ID");
                    if (teamInviteId != null && !teamInviteId.isBlank()) {
                        String key = System.getenv("MOCKARTY_IDEMPOTENCY_KEY");
                        if (key == null || key.isBlank()) {
                            throw new IllegalArgumentException("set MOCKARTY_IDEMPOTENCY_KEY for an exact retry");
                        }
                        client.cloudSpaces().revokeTeamInvite(spaceId, teamInviteId, key);
                        System.out.println("Team invitation revoked");
                    }
                }
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
