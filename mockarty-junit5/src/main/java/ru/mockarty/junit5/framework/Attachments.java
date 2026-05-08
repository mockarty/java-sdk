// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Static helpers for registering small artifacts on the active case
 * frame. The bytes are held in memory until {@link AttachReport} fires
 * the upload after the test.
 *
 * <p>Heavy artifacts (multi-MB) should go through the SDK directly via
 * the TCM API — {@code Attachments.attach} is for the inline-report
 * artefacts (logs, response snippets, screenshots).</p>
 */
public final class Attachments {

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    public static final String TEXT_CONTENT_TYPE = "text/plain; charset=utf-8";
    public static final String JSON_CONTENT_TYPE = "application/json";

    private Attachments() {}

    /** Attach a byte payload with explicit content type. */
    public static void attach(String name, byte[] body, String contentType) {
        validate(name);
        if (body == null) {
            body = new byte[0];
        }
        if (contentType == null || contentType.isEmpty()) {
            contentType = DEFAULT_CONTENT_TYPE;
        }
        MockartyContext.CaseFrame frame = MockartyContext.currentCase();
        if (frame == null) {
            // Fail-soft outside a case binding.
            return;
        }
        Map<String, Object> entry = new HashMap<>();
        entry.put("name", name);
        entry.put("body", body);
        entry.put("contentType", contentType);
        frame.attachments.add(entry);
    }

    /** Attach a UTF-8 string body, auto-tagged as text/plain when not specified. */
    public static void attach(String name, String body) {
        attach(name, body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8), TEXT_CONTENT_TYPE);
    }

    /** Attach a JSON payload (string), tagged as application/json. */
    public static void attachJson(String name, String json) {
        attach(name, json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8), JSON_CONTENT_TYPE);
    }

    private static void validate(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachments.attach requires a non-empty name");
        }
    }
}
