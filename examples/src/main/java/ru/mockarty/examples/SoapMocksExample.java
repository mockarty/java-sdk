// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.ContentResponse;
import ru.mockarty.model.Mock;

import java.util.List;
import java.util.Map;

/**
 * SOAP mock examples covering service/method matching,
 * XML body conditions, SOAP faults, and header-based routing.
 */
public class SoapMocksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createSimpleSoapMock(client);
            createSoapWithConditions(client);
            createSoapFaultMock(client);
            createSoapWithHeaderConditions(client);
            createSoapOneOfMock(client);
        }
    }

    /**
     * Simple SOAP mock for a weather service.
     */
    static void createSimpleSoapMock(MockartyClient client) {
        Mock mock = MockBuilder.soap("WeatherService", "GetWeather")
                .id("soap-get-weather")
                .respond(200, Map.of(
                        "GetWeatherResponse", Map.of(
                                "Temperature", "$.fake.IntRange(0,40)",
                                "Humidity", "$.fake.IntRange(20,90)",
                                "Condition", "Sunny",
                                "City", "$.fake.City",
                                "LastUpdated", "$.fake.DateISO"
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created simple SOAP mock");
    }

    /**
     * SOAP mock with XML body field conditions.
     * Matches when the SOAP request body contains specific values.
     */
    static void createSoapWithConditions(MockartyClient client) {
        Mock mock = MockBuilder.soap("AccountService", "GetAccountBalance")
                .id("soap-account-balance")
                .condition("AccountNumber", AssertAction.MATCHES, "^[A-Z]{2}\\d{10}$")
                .condition("Currency", AssertAction.EQUALS, "EUR")
                .respond(200, Map.of(
                        "GetAccountBalanceResponse", Map.of(
                                "AccountNumber", "$.req.AccountNumber",
                                "Balance", "$.fake.FloatRange(100.00,50000.00)",
                                "Currency", "EUR",
                                "AvailableBalance", "$.fake.FloatRange(50.00,45000.00)",
                                "LastTransaction", "$.fake.DateISO",
                                "Status", "ACTIVE"
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created SOAP mock with conditions");
    }

    /**
     * SOAP fault response for error simulation.
     */
    static void createSoapFaultMock(MockartyClient client) {
        // Invalid account number returns a SOAP fault
        Mock mock = MockBuilder.soap("AccountService", "GetAccountBalance")
                .id("soap-account-fault")
                .condition("AccountNumber", AssertAction.EQUALS, "INVALID")
                .priority(100)
                .respond(new ContentResponse()
                        .statusCode(500)
                        .soapFault(Map.of(
                                "faultCode", "soap:Client",
                                "faultString", "Invalid account number",
                                "detail", Map.of(
                                        "errorCode", "ACC-001",
                                        "message", "The provided account number is not valid",
                                        "suggestion", "Account numbers must match format: XX0000000000"
                                )
                        ))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created SOAP fault mock");
    }

    /**
     * SOAP mock with header-based routing.
     * Different responses based on SOAPAction or custom headers.
     */
    static void createSoapWithHeaderConditions(MockartyClient client) {
        Mock mock = MockBuilder.soap("NotificationService", "SendEmail")
                .id("soap-send-email")
                .headerCondition("SOAPAction", AssertAction.CONTAINS, "SendEmail")
                .headerCondition("X-Client-ID", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "SendEmailResponse", Map.of(
                                "MessageId", "$.fake.UUID",
                                "Status", "QUEUED",
                                "EstimatedDelivery", "$.fake.DateISO",
                                "RecipientCount", 1
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created SOAP mock with header conditions");
    }

    /**
     * SOAP OneOf mock for simulating intermittent service behavior.
     */
    static void createSoapOneOfMock(MockartyClient client) {
        Mock mock = MockBuilder.soap("PaymentService", "ProcessPayment")
                .id("soap-payment-oneof")
                .oneOfOrdered(
                        // First call: success
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of(
                                        "ProcessPaymentResponse", Map.of(
                                                "TransactionId", "$.fake.UUID",
                                                "Status", "APPROVED",
                                                "AuthCode", "$.fake.IntRange(100000,999999)"
                                        )
                                )),
                        // Second call: pending
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of(
                                        "ProcessPaymentResponse", Map.of(
                                                "TransactionId", "$.fake.UUID",
                                                "Status", "PENDING",
                                                "Message", "Transaction requires manual review"
                                        )
                                )),
                        // Third call: declined
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of(
                                        "ProcessPaymentResponse", Map.of(
                                                "TransactionId", "$.fake.UUID",
                                                "Status", "DECLINED",
                                                "Reason", "Insufficient funds"
                                        )
                                ))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created SOAP OneOf mock");
    }
}
