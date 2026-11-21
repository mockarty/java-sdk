// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class LLMPriceList {
    private List<LLMPrice> prices;
    public List<LLMPrice> getPrices() { return prices == null ? Collections.emptyList() : prices; }
}
