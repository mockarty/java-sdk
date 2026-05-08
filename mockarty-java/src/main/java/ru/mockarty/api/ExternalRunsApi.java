// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalRunResponse;

/**
 * Client for {@code POST /api/v1/namespaces/:namespace/tcm/external-runs}.
 *
 * <p>Used by the JUnit 5 framework adapter (and direct callers) to ship
 * a per-test outcome — status, steps, captured stdout/stderr, small
 * attachments — to TCM as a synthetic case run, without invoking the
 * orchestrator.</p>
 *
 * <p>Defaults are tuned for the 80% case: caller sets {@code status} and
 * one of {@code caseId} / {@code caseName} on the request and that's it.
 * Empty optional fields are dropped from the wire payload by the
 * {@code @JsonInclude(NON_DEFAULT)} on {@link ExternalRunRequest}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ExternalRunResponse resp = client.externalRuns().report("qa",
 *     new ExternalRunRequest()
 *         .status(ExternalRunRequest.STATUS_PASSED)
 *         .caseId("11111111-1111-1111-1111-111111111111")
 *         .framework("junit5")
 *         .externalId("ru.example.LoginTest#login"));
 * System.out.println("Run url: " + resp.getUrl());
 * }</pre>
 */
public class ExternalRunsApi {

    private final MockartyClient client;

    public ExternalRunsApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Persist a synthetic case run.
     *
     * @param namespace target namespace; required.
     * @param request   the run envelope; must carry {@code status} plus
     *                  one of {@code caseId} / {@code caseName}.
     * @return the server's response (run id, resolved case, deep-link URL).
     */
    public ExternalRunResponse report(String namespace, ExternalRunRequest request)
            throws MockartyException {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            throw new IllegalArgumentException("request.status is required");
        }
        if ((request.getCaseId() == null || request.getCaseId().isEmpty())
                && (request.getCaseName() == null || request.getCaseName().isEmpty())) {
            throw new IllegalArgumentException(
                    "one of request.caseId / request.caseName is required");
        }
        String path = "/api/v1/namespaces/" + namespace + "/tcm/external-runs";
        return client.post(path, request, ExternalRunResponse.class);
    }
}
