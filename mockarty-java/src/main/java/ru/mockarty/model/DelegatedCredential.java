// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One issued delegated credential.
 *
 * <p>A delegated credential is the primitive for shared work: the issuer freezes
 * the namespace set, the action set and the expiry, and the holder cannot widen
 * any of the three. That is what separates it from an API key, whose namespace
 * is a default its holder can change and which carries no expiry of its own.
 *
 * <p>The stored bcrypt hash and sha256 index are never part of this payload, so
 * there is nothing here a caller must remember to strip.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegatedCredential {
    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("namespaces") private List<String> namespaces;
    @JsonProperty("allowed_actions") private List<String> allowedActions;
    @JsonProperty("expires_at") private Instant expiresAt;
    @JsonProperty("created_at") private Instant createdAt;
    @JsonProperty("updated_at") private Instant updatedAt;
    @JsonProperty("revoked_at") private Instant revokedAt;
    @JsonProperty("last_used_at") private Instant lastUsedAt;
    @JsonProperty("issuer_user_id") private String issuerUserId;
    @JsonProperty("issuer_credential_id") private String issuerCredentialId;
    @JsonProperty("issuer_user_role") private String issuerUserRole;
    @JsonProperty("issuer_user_email") private String issuerUserEmail;
    @JsonProperty("issuer_user_login") private String issuerUserLogin;
    /** Display label only; useless for authentication. */
    @JsonProperty("token_prefix") private String tokenPrefix;
    @JsonProperty("metadata") private Map<String, Object> metadata;

    public DelegatedCredential() { }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getNamespaces() { return namespaces == null ? Collections.emptyList() : namespaces; }
    public List<String> getAllowedActions() { return allowedActions == null ? Collections.emptyList() : allowedActions; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public String getIssuerUserId() { return issuerUserId; }
    public String getIssuerCredentialId() { return issuerCredentialId; }
    public String getIssuerUserRole() { return issuerUserRole; }
    public String getIssuerUserEmail() { return issuerUserEmail; }
    public String getIssuerUserLogin() { return issuerUserLogin; }
    public String getTokenPrefix() { return tokenPrefix; }
    public Map<String, Object> getMetadata() { return metadata; }

    /** Revocation is terminal — there is no un-revoke. */
    public boolean isRevoked() { return revokedAt != null; }

    /**
     * Whether the credential has lapsed at {@code now}.
     *
     * <p>A credential whose expiry did not decode answers {@code true}: treating a
     * missing field as "unbounded" is the one reading that keeps a shared
     * credential working past the instant the issuer chose.
     */
    public boolean isExpired(Instant now) {
        if (expiresAt == null) return true;
        Instant moment = now == null ? Instant.now() : now;
        return !moment.isBefore(expiresAt);
    }

    /** Whether {@code namespace} is inside the frozen namespace set. */
    public boolean hasNamespace(String namespace) {
        return namespace != null && !namespace.isBlank() && getNamespaces().contains(namespace);
    }

    /** Whether {@code action} is inside the frozen action set. */
    public boolean allows(String action) {
        return action != null && getAllowedActions().contains(action);
    }
}
