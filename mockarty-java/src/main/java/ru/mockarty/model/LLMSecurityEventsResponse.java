// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class LLMSecurityEventsResponse {
    private List<LLMSecurityEvent> events;

    public List<LLMSecurityEvent> getEvents() {
        return events == null ? Collections.emptyList() : events;
    }
}
