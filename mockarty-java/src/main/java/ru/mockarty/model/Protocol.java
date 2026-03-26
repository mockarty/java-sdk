// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Supported mock protocols in Mockarty.
 */
public enum Protocol {

    @JsonProperty("http")
    HTTP,

    @JsonProperty("grpc")
    GRPC,

    @JsonProperty("mcp")
    MCP,

    @JsonProperty("socket")
    SOCKET,

    @JsonProperty("soap")
    SOAP,

    @JsonProperty("graphql")
    GRAPHQL,

    @JsonProperty("sse")
    SSE,

    @JsonProperty("kafka")
    KAFKA,

    @JsonProperty("rabbitmq")
    RABBITMQ,

    @JsonProperty("smtp")
    SMTP
}
