// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentsTest {

    @AfterEach
    void cleanup() {
        MockartyContext.resetForTest();
    }

    @Test
    void attach_recordsOnActiveCaseFrame() {
        MockartyContext.CaseFrame frame = new MockartyContext.CaseFrame();
        MockartyContext.pushCase(frame);
        try {
            Attachments.attach("hello", "world");
            assertEquals(1, frame.attachments.size());
            Map<String, Object> entry = frame.attachments.get(0);
            assertEquals("hello", entry.get("name"));
            assertArrayEquals("world".getBytes(), (byte[]) entry.get("body"));
            assertEquals(Attachments.TEXT_CONTENT_TYPE, entry.get("contentType"));
        } finally {
            MockartyContext.popCase();
        }
    }

    @Test
    void attachJson_setsApplicationJsonContentType() {
        MockartyContext.CaseFrame frame = new MockartyContext.CaseFrame();
        MockartyContext.pushCase(frame);
        try {
            Attachments.attachJson("payload", "{\"a\":1}");
            assertEquals(Attachments.JSON_CONTENT_TYPE,
                    frame.attachments.get(0).get("contentType"));
        } finally {
            MockartyContext.popCase();
        }
    }

    @Test
    void attach_outsideFrame_isFailSoft() {
        // No active case — must not throw, and obviously can't record anything.
        assertNull(MockartyContext.currentCase());
        Attachments.attach("orphan", "x");
    }

    @Test
    void attach_rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> Attachments.attach("", new byte[]{1, 2}, "application/octet-stream"));
    }
}
