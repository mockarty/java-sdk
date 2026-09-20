package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

public final class CloudIdentityExample {
    private CloudIdentityExample() {}

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.create(System.getenv("MOCKARTY_BASE_URL"), System.getenv("MOCKARTY_API_KEY"))) {
            client.cloudIdentity().list().forEach(identity -> System.out.println(identity.getProvider()));
            // An email change requires a Cloud cabinet session and confirmation
            // from both the current and new mailboxes (or enrolled 2FA first).
            String newEmail = System.getenv("MOCKARTY_NEW_EMAIL");
            if (newEmail != null && !newEmail.isBlank()) {
                System.out.println(client.cloudIdentity().requestEmailChange(newEmail, "", "").getStage());
            }
        }
    }
}
