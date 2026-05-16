// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.Map;
import java.util.Objects;

/**
 * Wrapper around a request/response body in the Pact DSL.
 *
 * <p>A body has a content kind (JSON / raw text / binary) and may carry
 * matchers attached to JSONPath-style locations. We never store the
 * "rendered" body — {@link PactWriter} walks the structure at serialisation
 * time, replacing {@link Matcher} instances with their {@code example()}
 * values for the body, and emitting parallel {@code matchingRules} entries
 * for the rule tree.</p>
 */
public sealed interface PactBody permits PactBody.Json, PactBody.Text, PactBody.Binary, PactBody.Empty {

    /** No body. */
    record Empty() implements PactBody {}

    /** JSON body: nested map/list tree. Matchers can appear at any leaf. */
    record Json(Object root) implements PactBody {
        public Json {
            Objects.requireNonNull(root, "Json body root must not be null — use PactBody.empty() instead");
        }
    }

    /** Raw text body. */
    record Text(String body, String contentType) implements PactBody {
        public Text {
            Objects.requireNonNull(body, "Text body must not be null");
            Objects.requireNonNull(contentType, "Text body contentType must not be null");
        }
    }

    /** Binary body. Pact V4 only — stored as base64 in the JSON. */
    record Binary(byte[] body, String contentType) implements PactBody {
        public Binary {
            Objects.requireNonNull(body, "Binary body must not be null");
            Objects.requireNonNull(contentType, "Binary body contentType must not be null");
        }
    }

    /** Convenience: an empty body. */
    static PactBody empty() {
        return new Empty();
    }

    /** Convenience: a JSON body from a Map. */
    static PactBody json(Map<String, Object> root) {
        return new Json(root);
    }
}
