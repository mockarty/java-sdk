package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JavaScript scripted response. When set on a {@link ContentResponse}, the code
 * runs every time the mock is hit: it receives {@code request} and fills
 * {@code response} (status/headers/body/delay). See the Scripted Responses guide.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseScript {

    @JsonProperty("code")
    private String code;

    @JsonProperty("language")
    private String language;

    @JsonProperty("timeoutMs")
    private Integer timeoutMs;

    @JsonProperty("allowNet")
    private Boolean allowNet;

    public ResponseScript() {
    }

    public ResponseScript code(String code) {
        this.code = code;
        return this;
    }

    public ResponseScript language(String language) {
        this.language = language;
        return this;
    }

    public ResponseScript timeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    /** Allow the script to make outbound calls (mk.http.send). Off by default. */
    public ResponseScript allowNet(boolean allowNet) {
        this.allowNet = allowNet;
        return this;
    }

    public String getCode() {
        return code;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public Boolean getAllowNet() {
        return allowNet;
    }
}
