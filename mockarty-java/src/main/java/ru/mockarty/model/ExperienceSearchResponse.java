// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class ExperienceSearchResponse {
    private List<ExperienceItem> results;
    private String engine;
    private int total;
    private boolean available;

    public List<ExperienceItem> getResults() { return results == null ? Collections.emptyList() : results; }
    public String getEngine() { return engine; }
    public int getTotal() { return total; }
    public boolean isAvailable() { return available; }
}
