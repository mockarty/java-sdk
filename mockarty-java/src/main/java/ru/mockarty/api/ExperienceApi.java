// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.ExperienceRecordRequest;
import ru.mockarty.model.ExperienceRecordResponse;
import ru.mockarty.model.ExperienceSearchResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Search and record reusable AutoTester run experience. */
public class ExperienceApi {
    public static final String KIND_MISSION_LESSON = "mission_lesson";
    public static final String KIND_PITFALL = "pitfall";
    public static final String KIND_PRODUCT_FACT = "product_fact";
    public static final String KIND_DEFECT_ROOT_CAUSE = "defect_root_cause";

    private final MockartyClient client;

    public ExperienceApi(MockartyClient client) { this.client = client; }

    public ExperienceSearchResponse search(String query) throws MockartyException {
        return search(query, List.of(), null, 0);
    }

    public ExperienceSearchResponse search(String query, List<String> kinds, String minTrust, int limit) throws MockartyException {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("query is required");
        }
        StringBuilder path = new StringBuilder("/api/v1/autotester/context/knowledge/search?query=")
                .append(enc(query.trim()));
        if (kinds != null && !kinds.isEmpty()) path.append("&kinds=").append(enc(String.join(",", kinds)));
        if (minTrust != null && !minTrust.isBlank()) path.append("&minTrust=").append(enc(minTrust));
        if (limit > 0) path.append("&k=").append(limit);
        return client.get(path.toString(), ExperienceSearchResponse.class);
    }

    public ExperienceRecordResponse record(ExperienceRecordRequest request) throws MockartyException {
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("text is required");
        }
        if (request.getSource() == null || request.getSource().trim().isEmpty()) {
            throw new IllegalArgumentException("source is required");
        }
        return client.post("/api/v1/autotester/context/knowledge", request, ExperienceRecordResponse.class);
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
