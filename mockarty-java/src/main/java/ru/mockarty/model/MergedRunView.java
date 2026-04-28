// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Response shape for the merge create/get/list endpoints — the parent row
 * plus the current snapshot of every attached source run.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergedRunView {

    private MergedTestRun run;
    private List<MergedTestRun> sources = new ArrayList<>();

    public MergedTestRun getRun() {
        return run;
    }

    public void setRun(MergedTestRun run) {
        this.run = run;
    }

    public List<MergedTestRun> getSources() {
        return sources;
    }

    public void setSources(List<MergedTestRun> sources) {
        this.sources = sources == null ? new ArrayList<>() : sources;
    }
}
