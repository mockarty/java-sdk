// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class ExperienceReviewDetail {
    private ExperienceReviewItem item;
    private List<ExperienceReviewRelation> relations;
    private List<ExperienceReviewMutation> history;

    public ExperienceReviewItem getItem() { return item; }
    public List<ExperienceReviewRelation> getRelations() { return relations == null ? Collections.emptyList() : relations; }
    public List<ExperienceReviewMutation> getHistory() { return history == null ? Collections.emptyList() : history; }
}
