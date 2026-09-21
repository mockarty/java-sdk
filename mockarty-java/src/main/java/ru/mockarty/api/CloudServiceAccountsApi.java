package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Human management API for customer-owned Cloud service accounts. */
public class CloudServiceAccountsApi {
    private final MockartyClient client;

    public CloudServiceAccountsApi(MockartyClient client) { this.client = client; }

    public Page list(String spaceId) throws MockartyException {
        Page page = client.get(collection(spaceId), Page.class);
        return page == null ? new Page() : page;
    }

    public ServiceAccount get(String spaceId, String accountId) throws MockartyException {
        return client.get(account(spaceId, accountId), ServiceAccount.class);
    }

    public CreateResult create(String spaceId, String name, List<String> capabilities,
                               boolean issueCredential, long credentialTtlSeconds,
                               String idempotencyKey) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("capabilities", capabilities);
        body.put("issue_credential", issueCredential);
        body.put("credential_ttl_seconds", credentialTtlSeconds);
        return client.postWithHeaders(collection(spaceId), body, CreateResult.class, headers(idempotencyKey));
    }

    public Binding bind(String spaceId, String accountId, String targetSpaceId,
                        List<String> capabilities, String idempotencyKey) throws MockartyException {
        return client.postWithHeaders(account(spaceId, accountId) + "/bindings",
                Map.of("space_id", targetSpaceId, "capabilities", capabilities), Binding.class,
                headers(idempotencyKey));
    }

    public CredentialIssued issueCredential(String spaceId, String accountId, long ttlSeconds,
                                             String idempotencyKey) throws MockartyException {
        return credential(spaceId, accountId, ttlSeconds, idempotencyKey, false);
    }

    public CredentialIssued rotateCredential(String spaceId, String accountId, long ttlSeconds,
                                              String idempotencyKey) throws MockartyException {
        return credential(spaceId, accountId, ttlSeconds, idempotencyKey, true);
    }

    private CredentialIssued credential(String spaceId, String accountId, long ttlSeconds,
                                        String idempotencyKey, boolean rotate) throws MockartyException {
        if (ttlSeconds < 0) throw new IllegalArgumentException("ttlSeconds must not be negative");
        String path = account(spaceId, accountId) + "/credentials" + (rotate ? "/rotate" : "");
        return client.postWithHeaders(path, Map.of("ttl_seconds", ttlSeconds), CredentialIssued.class,
                headers(idempotencyKey));
    }

    public ServiceAccount revoke(String spaceId, String accountId, String idempotencyKey) throws MockartyException {
        return client.deleteWithHeaders(account(spaceId, accountId), ServiceAccount.class, headers(idempotencyKey));
    }

    private static Map<String, String> headers(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        return Map.of("Idempotency-Key", idempotencyKey.trim());
    }

    private static String collection(String spaceId) {
        if (spaceId == null || spaceId.isBlank()) throw new IllegalArgumentException("space id is required");
        return "/api/v1/cloud/spaces/" + encode(spaceId) + "/service-accounts";
    }

    private static String account(String spaceId, String accountId) {
        if (accountId == null || accountId.isBlank()) throw new IllegalArgumentException("account id is required");
        return collection(spaceId) + "/" + encode(accountId);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Page {
        @JsonProperty("service_accounts") private List<ServiceAccount> serviceAccounts = Collections.emptyList();
        private Capacity capacity;
        @JsonProperty("delegatable_capabilities") private List<String> delegatableCapabilities = Collections.emptyList();
        @JsonProperty("authority_capabilities") private List<String> authorityCapabilities = Collections.emptyList();
        @JsonProperty("request_id") private String requestId;
        public Page() { }
        public List<ServiceAccount> getServiceAccounts() { return serviceAccounts == null ? Collections.emptyList() : serviceAccounts; }
        public Capacity getCapacity() { return capacity; }
        public List<String> getDelegatableCapabilities() { return delegatableCapabilities; }
        public List<String> getAuthorityCapabilities() { return authorityCapabilities; }
        public String getRequestId() { return requestId; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Capacity {
        private int used;
        private int limit;
        private int remaining;
        public Capacity() { }
        public int getUsed() { return used; }
        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceAccount {
        private String id;
        private String name;
        private String status;
        @JsonProperty("billing_account_id") private String billingAccountId;
        @JsonProperty("created_by_user_id") private String createdByUserId;
        @JsonProperty("created_at") private String createdAt;
        @JsonProperty("updated_at") private String updatedAt;
        @JsonProperty("revoked_at") private String revokedAt;
        private List<Binding> bindings = Collections.emptyList();
        private List<Credential> credentials = Collections.emptyList();
        public ServiceAccount() { }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getBillingAccountId() { return billingAccountId; }
        public String getCreatedByUserId() { return createdByUserId; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public String getRevokedAt() { return revokedAt; }
        public List<Binding> getBindings() { return bindings == null ? Collections.emptyList() : bindings; }
        public List<Credential> getCredentials() { return credentials == null ? Collections.emptyList() : credentials; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Binding {
        @JsonProperty("space_id") private String spaceId;
        @JsonProperty("space_name") private String spaceName;
        @JsonProperty("bound_at") private String boundAt;
        private List<String> capabilities = Collections.emptyList();
        public Binding() { }
        public String getSpaceId() { return spaceId; }
        public String getSpaceName() { return spaceName; }
        public String getBoundAt() { return boundAt; }
        public List<String> getCapabilities() { return capabilities == null ? Collections.emptyList() : capabilities; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Credential {
        private String id;
        private String status;
        @JsonProperty("token_prefix") private String tokenPrefix;
        @JsonProperty("created_at") private String createdAt;
        @JsonProperty("last_used_at") private String lastUsedAt;
        @JsonProperty("expires_at") private String expiresAt;
        @JsonProperty("revoked_at") private String revokedAt;
        @JsonProperty("rotated_from_id") private String rotatedFromId;
        @JsonProperty("never_used") private boolean neverUsed;
        public Credential() { }
        public String getId() { return id; }
        public String getStatus() { return status; }
        public String getTokenPrefix() { return tokenPrefix; }
        public String getCreatedAt() { return createdAt; }
        public String getLastUsedAt() { return lastUsedAt; }
        public String getExpiresAt() { return expiresAt; }
        public String getRevokedAt() { return revokedAt; }
        public String getRotatedFromId() { return rotatedFromId; }
        public boolean isNeverUsed() { return neverUsed; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CredentialIssued extends Credential {
        private String secret;
        public CredentialIssued() { }
        public String getSecret() { return secret; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateResult {
        @JsonProperty("service_account") private ServiceAccount serviceAccount;
        private CredentialIssued credential;
        public CreateResult() { }
        public ServiceAccount getServiceAccount() { return serviceAccount; }
        public CredentialIssued getCredential() { return credential; }
    }
}
