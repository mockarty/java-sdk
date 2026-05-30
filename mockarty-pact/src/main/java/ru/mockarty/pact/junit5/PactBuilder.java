// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.junit5;

import ru.mockarty.pact.Consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code static} method that returns a {@link Consumer} (or
 * {@link ru.mockarty.pact.Pact}) used as the contract for a test class.
 *
 * <p>The method must be on the test class itself and take no arguments.
 * It is invoked once per test method to obtain a fresh contract — the
 * {@link MockartyPactExtension} then starts a {@link ru.mockarty.pact.MockServer}
 * around it.</p>
 *
 * <p>Example:</p>
 * <pre>
 * &#64;PactConsumer(name = "OrderService", provider = "PaymentService")
 * class ExampleTest {
 *
 *     &#64;PactBuilder
 *     static Consumer pact() {
 *         return Consumer.named("OrderService")
 *             .withProvider("PaymentService")
 *             .addInteraction(it -&gt; it.uponReceiving("…")
 *                 .withRequest("GET", "/ping")
 *                 .willRespondWith(200));
 *     }
 *
 *     &#64;Test
 *     void smoke(MockServer server) { … }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PactBuilder {
}
