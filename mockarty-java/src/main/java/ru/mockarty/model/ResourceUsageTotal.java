// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class ResourceUsageTotal {
    private long events;
    private long quantity;
    private String eventKind;
    private String unit;

    public long getEvents() { return events; }
    public long getQuantity() { return quantity; }
    public String getEventKind() { return eventKind; }
    public String getUnit() { return unit; }
}
