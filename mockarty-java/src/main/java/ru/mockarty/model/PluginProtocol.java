// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One active plugin-supplied codec served by the unified listener. */
public class PluginProtocol {
    private String key;
    private String name;
    private String description;
    private String transport;
    private String magic;
    @JsonProperty("pluginId")
    private String pluginId;
    @JsonProperty("mockProtocol")
    private String mockProtocol;
    @JsonProperty("serverName")
    private String serverName;

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTransport() { return transport; }
    public String getMagic() { return magic; }
    public String getPluginId() { return pluginId; }
    public String getMockProtocol() { return mockProtocol; }
    public String getServerName() { return serverName; }
}
