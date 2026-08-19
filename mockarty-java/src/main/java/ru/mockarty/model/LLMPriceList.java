// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class LLMPriceList {
    private List<LLMPrice> prices;
    public List<LLMPrice> getPrices() { return prices == null ? Collections.emptyList() : prices; }
}
