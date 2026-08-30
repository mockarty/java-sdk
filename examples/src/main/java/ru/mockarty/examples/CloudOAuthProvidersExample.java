package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.CloudOAuthProvider;

public final class CloudOAuthProvidersExample {
    private CloudOAuthProvidersExample() {}

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder().build()) {
            CloudOAuthProvider provider = client.cloudOAuthProviders().update(
                    "github", "your-github-client-id",
                    "env://CLOUD_API_PROVIDER_SECRET_OAUTH_GITHUB",
                    0, true, "configure-github-1");
            System.out.printf("%s revision=%d secretConfigured=%s%n", provider.getProvider(),
                    provider.getConfigRevision(), provider.isSecretConfigured());
        }
    }
}
