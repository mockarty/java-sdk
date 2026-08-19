// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class LLMBudgetList {
    private List<LLMBudget> budgets;
    public List<LLMBudget> getBudgets() { return budgets == null ? Collections.emptyList() : budgets; }
}
