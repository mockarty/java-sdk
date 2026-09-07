// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.Collections;
import java.util.List;

/** Namespace-scoped catalogue of active plugin-supplied protocols. */
public class PluginProtocolCatalogue {
    private List<PluginProtocol> protocols;
    private String listener;
    private String usage;
    private int count;

    public List<PluginProtocol> getProtocols() {
        return protocols == null ? Collections.emptyList() : protocols;
    }
    public String getListener() { return listener; }
    public String getUsage() { return usage; }
    public int getCount() { return count; }
}
