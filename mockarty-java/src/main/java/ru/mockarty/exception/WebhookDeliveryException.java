// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

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
