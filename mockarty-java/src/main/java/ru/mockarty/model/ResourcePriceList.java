// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

public class ResourcePriceList {
    private List<ResourcePrice> resourcePrices;
    public List<ResourcePrice> getResourcePrices() {
        return resourcePrices == null ? Collections.emptyList() : resourcePrices;
    }
}
