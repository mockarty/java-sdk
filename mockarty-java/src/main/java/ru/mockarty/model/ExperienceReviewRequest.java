// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.ArrayList;
import java.util.List;

public class ExperienceReviewRequest {
    private String decision;
    private String reason;
    private String idempotencyKey;
    private String expiresAt;
    private String supersedesId;
    private List<String> contradictsIds = new ArrayList<>();
    private long expectedVersion;

    public ExperienceReviewRequest decision(String value) { this.decision = value; return this; }
    public ExperienceReviewRequest reason(String value) { this.reason = value; return this; }
    public ExperienceReviewRequest idempotencyKey(String value) { this.idempotencyKey = value; return this; }
    public ExperienceReviewRequest expiresAt(String value) { this.expiresAt = value; return this; }
    public ExperienceReviewRequest supersedesId(String value) { this.supersedesId = value; return this; }
    public ExperienceReviewRequest contradictsIds(List<String> value) { this.contradictsIds = value; return this; }
    public ExperienceReviewRequest expectedVersion(long value) { this.expectedVersion = value; return this; }

    public String getDecision() { return decision; }
    public String getReason() { return reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getExpiresAt() { return expiresAt; }
    public String getSupersedesId() { return supersedesId; }
    public List<String> getContradictsIds() { return contradictsIds; }
    public long getExpectedVersion() { return expectedVersion; }
}
