// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One consumer-provider HTTP interaction.
 *
 * <p>In Pact V3 each interaction has a single {@code providerState} string
 * field; in V4 it carries an array of {@link ProviderState} entries (each
 * with an optional {@code params} map). The DSL always lets callers add
 * multiple states — {@link PactWriter} flattens them down to a single
 * string under V3 (concatenating with {@code " AND "} per Pact V3 idiom,
 * the same behaviour pact-jvm exhibits for backward-compat).</p>
 */
public final class Interaction {

    private final String description;
    private final List<ProviderState> providerStates;
    private final PactRequest request;
    private final PactResponse response;

    Interaction(
            String description,
            List<ProviderState> providerStates,
            PactRequest request,
            PactResponse response) {
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.providerStates = Collections.unmodifiableList(providerStates);
        this.request = Objects.requireNonNull(request, "request must not be null");
        this.response = Objects.requireNonNull(response, "response must not be null");
    }

    public String description() { return description; }
    public List<ProviderState> providerStates() { return providerStates; }
    public PactRequest request() { return request; }
    public PactResponse response() { return response; }
}
