// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.
package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

/** Echo the original question identity; never fetch a newer round on retry. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionAnswerRequest {
    private String answer;
    private String idempotencyKey;
    private String questionId;
    private String text;
    private Long revision;
    private Long generation;
    private List<String> selected;
    private Entity entity;

    public MissionAnswerRequest answer(String value) { answer = value == null ? null : value.trim(); return this; }
    public MissionAnswerRequest idempotencyKey(String value) { idempotencyKey = value == null ? null : value.trim(); return this; }
    public MissionAnswerRequest questionId(String value) { questionId = value; return this; }
    public MissionAnswerRequest revision(long value) { revision = value; return this; }
    public MissionAnswerRequest generation(long value) { generation = value; return this; }
    public MissionAnswerRequest text(String value) { text = value; return this; }
    public MissionAnswerRequest selected(List<String> value) { selected = value == null ? null : new java.util.ArrayList<>(value); return this; }
    public MissionAnswerRequest entity(Entity value) { entity = value; return this; }
    public String getAnswer() { return answer; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getQuestionId() { return questionId; }
    public String getText() { return text; }
    public Long getRevision() { return revision; }
    public Long getGeneration() { return generation; }
    public List<String> getSelected() { return selected; }
    public Entity getEntity() { return entity; }

    public void validate() {
        boolean typed = questionId != null || revision != null || generation != null || text != null || selected != null || entity != null;
        if (!typed) {
            if (answer == null || answer.isBlank()) throw new IllegalArgumentException("answer or captured typed question answer is required");
            return;
        }
        if (answer != null && !answer.isBlank()) throw new IllegalArgumentException("do not mix legacy answer with typed fields");
        if (!bounded(questionId, 128, true) || revision == null || revision <= 0 || (generation != null && generation < 0))
            throw new IllegalArgumentException("captured questionId, positive revision and non-negative generation are required");
        if ((text == null || text.isBlank()) && (selected == null || selected.isEmpty()) && entity == null)
            throw new IllegalArgumentException("typed answer is empty");
        if (text != null && (!validUnicode(text) || text.codePointCount(0, text.length()) > 4000 ||
                text.codePoints().anyMatch(c -> Character.isISOControl(c) && c != 10 && c != 13 && c != 9)))
            throw new IllegalArgumentException("text exceeds 4000 characters or contains control characters");
        if (selected != null) {
            if (selected.size() > 32) throw new IllegalArgumentException("at most 32 selections are supported");
            var seen = new HashSet<String>();
            for (String value : selected) {
                if (!bounded(value, 128, true) || !seen.add(value.strip()))
                    throw new IllegalArgumentException("selections must be unique, nonempty and bounded");
            }
        }
        if (entity != null && (!bounded(entity.kind, 64, true) || !bounded(entity.id, 256, true) || !bounded(entity.namespace, 256, false)))
            throw new IllegalArgumentException("entity requires bounded kind/id and optional namespace");
    }
    private static boolean bounded(String value, int maxBytes, boolean required) {
        if (value == null) return !required;
        value = value.strip();
        return (!required || !value.isEmpty()) && validUnicode(value) && value.getBytes(StandardCharsets.UTF_8).length <= maxBytes;
    }
    private static boolean validUnicode(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) return false;
        }
        return true;
    }

    /** Scoped reference offered by the original question; conveys no permission. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Entity {
        private String kind;
        private String id;
        private String namespace;
        public Entity kind(String value) { kind = value; return this; }
        public Entity id(String value) { id = value; return this; }
        public Entity namespace(String value) { namespace = value; return this; }
        public String getKind() { return kind; }
        public String getId() { return id; }
        public String getNamespace() { return namespace; }
    }
}
