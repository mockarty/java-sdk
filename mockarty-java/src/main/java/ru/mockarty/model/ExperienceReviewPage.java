// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class ExperienceReviewPage {
    private List<ExperienceReviewItem> items;
    private String nextCursor;

    public List<ExperienceReviewItem> getItems() { return items == null ? Collections.emptyList() : items; }
    public String getNextCursor() { return nextCursor == null ? "" : nextCursor; }
}
