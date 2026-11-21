// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class LLMBudgetList {
    private List<LLMBudget> budgets;
    public List<LLMBudget> getBudgets() { return budgets == null ? Collections.emptyList() : budgets; }
}
