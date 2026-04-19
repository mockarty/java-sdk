// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Thrown by {@code TestPlanApi.testWebhook} when the server reports a
 * failed ping.
 */
public class WebhookDeliveryException extends MockartyException {

    public WebhookDeliveryException(String message) {
        super(message);
    }
}
