package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

public final class CloudCustomerOperationsExample {
    private CloudCustomerOperationsExample() {}

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(System.getenv("MOCKARTY_BASE_URL"))
                .apiKey(System.getenv("MOCKARTY_API_KEY"))
                .build()) {
            String spaceId = System.getenv("MOCKARTY_CLOUD_SPACE_ID");
            System.out.println(client.cloudCustomer().listLoyaltyRedemptions(spaceId, "", 25));
            System.out.println(client.cloudOperations().listSupportCases("open", "", 50));
        }
    }
}
