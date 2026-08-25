// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.ExperienceRecordRequest;
import ru.mockarty.model.ExperienceRecordResponse;
import ru.mockarty.model.ExperienceSearchResponse;
import ru.mockarty.model.ExperienceReviewDetail;
import ru.mockarty.model.ExperienceReviewPage;
import ru.mockarty.model.ExperienceReviewRequest;
import ru.mockarty.model.ExperienceReviewResponse;

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

    public ExperienceReviewPage listReview(int limit, String cursor) throws MockartyException {
		return listReview("candidate", limit, cursor);
	}

	public ExperienceReviewPage listReview(String state, int limit, String cursor) throws MockartyException {
		String effectiveState = state == null || state.isBlank() ? "candidate" : state.trim();
		StringBuilder path = new StringBuilder("/api/v1/autotester/context/knowledge/review?state=")
				.append(enc(effectiveState));
        if (limit > 0) path.append("&limit=").append(limit);
        if (cursor != null && !cursor.isBlank()) path.append("&cursor=").append(enc(cursor));
        return client.get(path.toString(), ExperienceReviewPage.class);
    }

    public ExperienceReviewDetail getReview(String id) throws MockartyException {
        return client.get(reviewPath(id), ExperienceReviewDetail.class);
    }

    public ExperienceReviewResponse review(String id, ExperienceReviewRequest request) throws MockartyException {
        if (request == null) throw new IllegalArgumentException("request is required");
        String decision = request.getDecision() == null ? "" : request.getDecision().trim();
        if (!decision.equals("publish") && !decision.equals("reject")) {
            throw new IllegalArgumentException("decision must be publish or reject");
        }
        if (request.getExpectedVersion() <= 0 || request.getReason() == null || request.getReason().isBlank()
                || request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("expected version, reason, and idempotency key are required");
        }
		if (decision.equals("reject") && (request.getExpiresAt() != null
				|| (request.getSupersedesId() != null && !request.getSupersedesId().isBlank())
                || (request.getContradictsIds() != null && !request.getContradictsIds().isEmpty()))) {
            throw new IllegalArgumentException("publish relations and expiry are not valid for reject");
        }
        return client.post(reviewPath(id), request, ExperienceReviewResponse.class);
    }

    private static String reviewPath(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        return "/api/v1/autotester/context/knowledge/review/" + enc(id.trim()).replace("+", "%20");
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
