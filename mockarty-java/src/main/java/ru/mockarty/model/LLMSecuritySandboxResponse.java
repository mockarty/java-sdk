// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class LLMSecuritySandboxResponse {
    private List<LLMSecurityFinding> findings;
    private String decision;
    private String mode;
    private int score;
    private boolean truncated;

    public List<LLMSecurityFinding> getFindings() { return findings == null ? Collections.emptyList() : findings; }
    public String getDecision() { return decision; }
    public String getMode() { return mode; }
    public int getScore() { return score; }
    public boolean isTruncated() { return truncated; }
}
