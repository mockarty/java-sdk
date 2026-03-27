// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Defines expected baseline conditions and the checks used to verify them.
 *
 * <p>Maps to the server-side SteadyState struct in internal/chaos/models.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SteadyState {

    @JsonProperty("checks")
    private List<SteadyStateCheck> checks;

    public SteadyState() {
    }

    public SteadyState checks(List<SteadyStateCheck> checks) {
        this.checks = checks;
        return this;
    }

    public List<SteadyStateCheck> getChecks() {
        return checks;
    }

    @Override
    public String toString() {
        return "SteadyState{" +
                "checks=" + (checks != null ? checks.size() : 0) +
                '}';
    }

    /**
     * A single verification of a steady-state hypothesis.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SteadyStateCheck {

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;

        @JsonProperty("endpoint")
        private String endpoint;

        @JsonProperty("method")
        private String method;

        @JsonProperty("expected")
        private Object expected;

        @JsonProperty("tolerance")
        private Double tolerance;

        @JsonProperty("timeoutSec")
        private Integer timeoutSec;

        @JsonProperty("intervalSec")
        private Integer intervalSec;

        @JsonProperty("query")
        private String query;

        @JsonProperty("headers")
        private Map<String, String> headers;

        public SteadyStateCheck() {
        }

        public SteadyStateCheck name(String name) {
            this.name = name;
            return this;
        }

        public SteadyStateCheck type(String type) {
            this.type = type;
            return this;
        }

        public SteadyStateCheck endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public SteadyStateCheck method(String method) {
            this.method = method;
            return this;
        }

        public SteadyStateCheck expected(Object expected) {
            this.expected = expected;
            return this;
        }

        public SteadyStateCheck tolerance(Double tolerance) {
            this.tolerance = tolerance;
            return this;
        }

        public SteadyStateCheck timeoutSec(Integer timeoutSec) {
            this.timeoutSec = timeoutSec;
            return this;
        }

        public SteadyStateCheck intervalSec(Integer intervalSec) {
            this.intervalSec = intervalSec;
            return this;
        }

        public SteadyStateCheck query(String query) {
            this.query = query;
            return this;
        }

        public SteadyStateCheck headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getEndpoint() { return endpoint; }
        public String getMethod() { return method; }
        public Object getExpected() { return expected; }
        public Double getTolerance() { return tolerance; }
        public Integer getTimeoutSec() { return timeoutSec; }
        public Integer getIntervalSec() { return intervalSec; }
        public String getQuery() { return query; }
        public Map<String, String> getHeaders() { return headers; }

        @Override
        public String toString() {
            return "SteadyStateCheck{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
