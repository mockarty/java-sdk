// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMSecurityFinding {
    private String ruleId;
    private String category;
    private String path;
    private String fingerprint;
    private int score;
    private int start;
    private int end;
    private int decodedDepth;
    private boolean normalized;

    public String getRuleId() { return ruleId; }
    public String getCategory() { return category; }
    public String getPath() { return path; }
    public String getFingerprint() { return fingerprint; }
    public int getScore() { return score; }
    public int getStart() { return start; }
    public int getEnd() { return end; }
    public int getDecodedDepth() { return decodedDepth; }
    public boolean isNormalized() { return normalized; }
}
