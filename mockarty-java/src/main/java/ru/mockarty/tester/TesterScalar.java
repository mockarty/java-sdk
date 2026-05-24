// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared scalar-stringify helper used by every facet's Extract path —
 * unifies number / bool / null / fallback shapes so the var-store value
 * format is consistent across HTTP / Kafka / RabbitMQ / SOAP / DB.
 */
final class TesterScalar {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TesterScalar() {}

    static String stringify(Object v) {
        if (v == null) { return ""; }
        if (v instanceof Boolean) { return ((Boolean) v) ? "true" : "false"; }
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        }
        if (v instanceof String) { return (String) v; }
        if (v instanceof byte[]) { return new String((byte[]) v); }
        try {
            return MAPPER.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            return v.toString();
        }
    }
}
