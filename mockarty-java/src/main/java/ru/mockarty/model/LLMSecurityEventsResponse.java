// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class LLMSecurityEventsResponse {
    private List<LLMSecurityEvent> events;

    public List<LLMSecurityEvent> getEvents() {
        return events == null ? Collections.emptyList() : events;
    }
}
