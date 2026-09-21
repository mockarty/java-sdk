package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

/** Inspect service-account capacity or the identity behind an msa_ credential. */
public final class CloudServiceAccountsExample {
    private CloudServiceAccountsExample() { }

    public static void main(String[] args) throws Exception {
        String credential = System.getenv("MOCKARTY_SERVICE_ACCOUNT_CREDENTIAL");
        String token = credential == null || credential.isBlank() ? System.getenv("MOCKARTY_API_KEY") : credential;
        try (var client = MockartyClient.builder().baseUrl(System.getenv("MOCKARTY_BASE_URL")).apiKey(token).build()) {
            if (credential != null && !credential.isBlank()) {
                var identity = client.cloudServiceAccount().self();
                System.out.println(identity.getServiceAccount().path("name").asText());
            } else {
                var page = client.cloudServiceAccounts().list(System.getenv("MOCKARTY_SPACE_ID"));
                System.out.println(page.getCapacity().getRemaining());
            }
        }
    }
}
