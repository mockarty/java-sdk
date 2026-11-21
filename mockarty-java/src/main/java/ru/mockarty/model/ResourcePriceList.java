// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class ResourcePriceList {
    private List<ResourcePrice> resourcePrices;
    public List<ResourcePrice> getResourcePrices() {
        return resourcePrices == null ? Collections.emptyList() : resourcePrices;
    }
}
