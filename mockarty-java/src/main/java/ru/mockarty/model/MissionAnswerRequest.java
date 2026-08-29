// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

/** Durable human input accepted while a unified mission waits for an answer. */
public class MissionAnswerRequest {
    private String answer;
    private String idempotencyKey;

    public MissionAnswerRequest answer(String value) {
        this.answer = value == null ? null : value.trim();
        return this;
    }

    public MissionAnswerRequest idempotencyKey(String value) {
        this.idempotencyKey = value == null ? null : value.trim();
        return this;
    }

    public String getAnswer() { return answer; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
