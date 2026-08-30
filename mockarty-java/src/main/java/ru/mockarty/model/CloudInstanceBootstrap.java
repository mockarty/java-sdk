package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudInstanceBootstrap {
    private String username;
    private String password;
    private String reason;
    private boolean available;
    private boolean oneTime;

    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean value) { available = value; }
    public boolean isOneTime() { return oneTime; }
    @JsonProperty("one_time") public void setOneTime(boolean value) { oneTime = value; }
}
