// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.util.Map;

/**
 * API for proxying requests through Mockarty.
 */
public class ProxyApi {

    private final MockartyClient client;

    public ProxyApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Proxies an HTTP request.
     *
     * @param request the proxy request parameters
     * @return the proxy response
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> http(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/proxy/http", request, Map.class);
    }

    /**
     * Proxies a SOAP request.
     *
     * @param request the proxy request parameters
     * @return the proxy response
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> soap(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/proxy/soap", request, Map.class);
    }

    /**
     * Proxies a gRPC request.
     *
     * @param request the proxy request parameters
     * @return the proxy response
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> grpc(Map<String, Object> request) throws MockartyException {
        return client.post("/api/v1/proxy/grpc", request, Map.class);
    }
}
